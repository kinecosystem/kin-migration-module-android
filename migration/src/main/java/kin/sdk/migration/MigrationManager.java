package kin.sdk.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.concurrent.atomic.AtomicBoolean;
import org.stellar.sdk.responses.HttpResponseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import kin.core.ServiceProvider;
import kin.sdk.Environment;
import kin.sdk.migration.core_related.KinAccountCoreImpl;
import kin.sdk.migration.core_related.KinClientCoreImpl;
import kin.sdk.migration.exception.AccountNotActivatedException;
import kin.sdk.migration.exception.AccountNotFoundException;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.exception.MigrationFailedException;
import kin.sdk.migration.exception.MigrationInProcessException;
import kin.sdk.migration.exception.OperationFailedException;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IKinVersionProvider;
import kin.sdk.migration.interfaces.ITransactionId;
import kin.sdk.migration.interfaces.MigrationManagerListener;
import kin.sdk.migration.sdk_related.KinClientSdkImpl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MigrationManager {

    private static final String TAG = MigrationManager.class.getSimpleName();
    private static final String KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY = "kinMigrationModulePreferenceFileKey";
    private static final String KIN_MIGRATION_COMPLETED_KEY = "kinMigrationCompletedKey";
    private static final int TIMEOUT = 30;
    private static final int MAX_RETRIES = 3;
    private static final String URL_MIGRATE_ACCOUNT_SERVICE = "https://migration-devplatform-playground.developers.kinecosystem.com/migrate?address=";

    private final Context context;
    private final String appId;
    private final MigrationNetworkInfo migrationNetworkInfo;
    private final IKinVersionProvider kinVersionProvider;
    private final String storeKey;
    private Handler handler;
    private OkHttpClient okHttpClient;
    private final AtomicBoolean isMigrationInProcess; // defence against multiple calls

    public MigrationManager(@NonNull Context applicationContext, @NonNull String appId,
                            @NonNull MigrationNetworkInfo migrationNetworkInfo,
                            @NonNull IKinVersionProvider kinVersionProvider) {
        this(applicationContext, appId, migrationNetworkInfo, kinVersionProvider, "");
    }

    public MigrationManager(@NonNull Context applicationContext, @NonNull String appId,
                            @NonNull MigrationNetworkInfo migrationNetworkInfo,
                            @NonNull IKinVersionProvider kinVersionProvider, @NonNull String storeKey) {
        this.context = applicationContext;
        this.appId = appId;
        this.migrationNetworkInfo = migrationNetworkInfo;
        this.kinVersionProvider = kinVersionProvider;
        this.storeKey = storeKey;
        isMigrationInProcess = new AtomicBoolean();
    }

    /**
     * @return the current kin client.
     */
    public IKinClient getCurrentKinClient() {
        Log.d(TAG, "getLastKinClient: ");
        final IKinClient kinClient;
        if (isMigrationAlreadyCompleted()) {
            kinClient = initNewKin();
        } else {
            kinClient = initKinCore();
        }
        return kinClient;
    }

    /**
     * Starting the migration process from Kin2(Core library) to the new Kin(Sdk library - One Blockchain).
     * <p><b>Note:</b> This method internally uses a background thread and it is also access the network.</p>
     * <p><b>Note:</b> If all the migration process will be completed then this method minimum time is 6 seconds.</p>
     * <p><b>Note:</b> This method should be called only once, if required more then create another instance of this
     * class.</p>
     *
     * @param migrationManagerListener is a listener so the caller can get a callback for completion or error(on the UI
     * thread).
     * @throws MigrationInProcessException is thrown in case this method is called while it is not finished.
     */
    public void start(final MigrationManagerListener migrationManagerListener) throws MigrationInProcessException {
        if (isMigrationInProcess.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "starting the migration process in a background thread");
                    createHttpClient();
                    startMigrationProcess(migrationManagerListener);
                }
            }).start();
        } else {
            throw new MigrationInProcessException("You can't start migration while migration is still in process");
        }
    }

    private void createHttpClient() {
        RetryInterceptor retryInterceptor = new RetryInterceptor();
        handler = new Handler(Looper.getMainLooper());
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(retryInterceptor)
                .build();
    }

    private void startMigrationProcess(final MigrationManagerListener migrationManagerListener) {
        if (isMigrationAlreadyCompleted()) {
            Log.d(TAG, "startMigrationProcess: migration is already completed in the past");
            fireOnReady(migrationManagerListener, initNewKin(), false);
        } else {
            try {
                KinSdkVersion kinSdkVersion = kinVersionProvider.getKinSdkVersion(appId);
                if (kinSdkVersion == null) {
                    fireOnError(migrationManagerListener, new FailedToResolveSdkVersionException());
                } else {
                    if (kinSdkVersion == KinSdkVersion.NEW_KIN_SDK) {
                        Log.d(TAG, "startMigrationProcess: new sdk.");
                        burnAndMigrateAccount(migrationManagerListener);
                    } else {
                        fireOnReady(migrationManagerListener, initKinCore(), false);
                    }
                }
            } catch (FailedToResolveSdkVersionException e) {
                fireOnError(migrationManagerListener, e);
            } catch (MigrationFailedException e) {
                fireOnError(migrationManagerListener, e);
            } catch (OperationFailedException e) {
                fireOnError(migrationManagerListener, new MigrationFailedException(e.getMessage(), e));
            }
        }
    }

    private void burnAndMigrateAccount(MigrationManagerListener migrationManagerListener)
            throws OperationFailedException {
        KinClientCoreImpl kinClientCore = initKinCore();
        if (kinClientCore.hasAccount()) {
            postMigrationStart(migrationManagerListener);
            KinAccountCoreImpl account = (KinAccountCoreImpl) kinClientCore
                    .getAccount(kinClientCore.getAccountCount() - 1);
            String publicAddress = account.getPublicAddress();
            Log.d(TAG, "startMigrationProcess: retrieve this account: " + publicAddress);
            try {
                startBurnAccountProcess(publicAddress, account);
                migrateToNewKin(publicAddress, migrationManagerListener);
            } catch (AccountNotFoundException | AccountNotActivatedException e) {
                // If no account has been found or it not activated then we just return a new kin client that wil run on the new blockchain.
                fireOnReady(migrationManagerListener, initNewKin(), true);
            }
        } else {
            fireOnReady(migrationManagerListener, initNewKin(), true);
        }
    }

    private void postMigrationStart(final MigrationManagerListener migrationManagerListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                migrationManagerListener.onMigrationStart();
            }
        });
    }

    /**
     * Start the burn account process which checks if the account was already burned and if not then burned it.
     * @param account is the kin account.
     * @throws MigrationFailedException a general Migration Exception with a concrete message about the exception.
     */
    private void startBurnAccountProcess(final String publicAddress, final KinAccountCoreImpl account)
            throws OperationFailedException {
        if (publicAddress != null) {
            if (isAccountBurned(account)) {
                Log.d(TAG, "startBurnAccountProcess: account is already burned");
            } else {
                burnAccount(publicAddress, account);
            }
        } else {
            throw new MigrationFailedException("account not valid - public address is null");
        }
    }

    private boolean isAccountBurned(final KinAccountCoreImpl kinAccountCore) throws OperationFailedException {
        int retryCounter = 0;
        while (true) {
            try {
                Log.d(TAG, "isAccountBurned: ");
                return kinAccountCore.isAccountBurned();
            } catch (OperationFailedException e) {
                if (shouldRetry(retryCounter, e)) {
                    Log.d(TAG, "isAccountBurned: retry number " + retryCounter);
                    retryCounter++;
                    continue;
                }
                throw new MigrationFailedException("Checking if the old account is burned has failed", e);
            }
        }
    }

    private void burnAccount(String publicAddress, KinAccountCoreImpl account) throws MigrationFailedException {
        int retryCounter = 0;
        while (true) {
            try {
                Log.d(TAG, "burnAccount: ");
                ITransactionId transactionId = account.sendBurnTransactionSync(publicAddress);
                if (transactionId.id() == null || transactionId.id().isEmpty()) {
                    throw new OperationFailedException("Burning the account could not succeed due to an unexpected error");
                } else {
                    break;
                }
            } catch (OperationFailedException e) {
                if (shouldRetry(retryCounter, e)) {
                    Log.d(TAG, "burnAccount: retry number " + retryCounter);
                    retryCounter++;  // TODO: 30/12/2018 maybe need to catch more exception for the retry? like java.net.ConnectException?
                    continue;
                }
                throw new MigrationFailedException("Burning the old account failed", e);
            }
        }
    }

    private boolean shouldRetry(int retryCounter, Throwable e) {
        boolean shouldRetry = false;
        Throwable cause = e.getCause();
        if (cause instanceof HttpResponseException) {
            HttpResponseException httpException = (HttpResponseException) cause;
            if (httpException.getStatusCode() >= 500 && retryCounter < MAX_RETRIES) {
                shouldRetry = true;
            }
        }
        return shouldRetry;
    }

    private void migrateToNewKin(final String publicAddress, final MigrationManagerListener migrationManagerListener) {
        Log.d(TAG, "migrateToNewKin: sending the request to migrate");
        try {
            Response response = sendRequest(URL_MIGRATE_ACCOUNT_SERVICE + publicAddress);
            if (response.isSuccessful()) {
                fireOnReady(migrationManagerListener, initNewKin(), true);
            } else {
                handleMigrationException(response, migrationManagerListener);
            }
        } catch (IOException e) {
            fireOnError(migrationManagerListener, e);
        }
    }

    // Handle the migration exceptions, if an exception that is not solvable then throw it, else just handle it.
    private void handleMigrationException(Response response, MigrationManagerListener migrationManagerListener) {
        // check if account has been migrated successfully and if yes then complete the process and update the persistent state.
        Log.d(TAG, "generateMigrationException: ");
        boolean handled = false;
        MigrationFailedException exception = new MigrationFailedException("Migration not completed due to an unexpected exception");
        ResponseBody body = response.body();
        if (body != null) {
            try {
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> error = new Gson().fromJson(body.string(), type);
                final String code = error.get("code");
                String message = error.get("message");
                if (code != null) {
                    switch (code) {
                        case "4001":  // account not burned
                            exception = new MigrationFailedException(message + ", code = " + code);
                            break;
                        case "4002":  // account was already migrated
                            fireOnReady(migrationManagerListener, initNewKin(), true);
                            handled = true;
                            break;
                        case "4003":  // public address is not valid, meaning the format of it is not valid.
                            exception = new MigrationFailedException(message + ", code = " + code);
                            break;
                        case "4041":  // account was not found
                            fireOnReady(migrationManagerListener, initNewKin(), true);
                            handled = true;
                            break;
                        default:
                            exception = new MigrationFailedException("Got an unexpected migration exception with message: " + message + ", and code: " + code);
                            break;
                    }
                }
            } catch (IOException e) {
                exception = new MigrationFailedException("Json parsing failed", e);
            }
        } else {
            exception = new MigrationFailedException("Body is null, response code is = " + response.code());
        }
        if (!handled) {
            fireOnError(migrationManagerListener, exception);
        }
    }

    @NonNull
    private KinClientCoreImpl initKinCore() {
        ServiceProvider environment = new ServiceProvider(migrationNetworkInfo.getCoreNetworkUrl(),
                migrationNetworkInfo.getCoreNetworkId()) {
            @Override
            protected String getIssuerAccountId() {
                return migrationNetworkInfo.getIssuer();
            }
        };
        return new KinClientCoreImpl(context, environment, appId, storeKey);
    }

    @NonNull
    private IKinClient initNewKin() {
        IKinClient kinClient;
        Environment environment = new Environment(migrationNetworkInfo.getSdkNetworkUrl(),
                migrationNetworkInfo.getSdkNetworkId());
        kinClient = new KinClientSdkImpl(context, environment, appId, storeKey);
        return kinClient;
    }

    private void saveMigrationCompleted() {
        // save migration completion status from the persistent state.
        SharedPreferences sharedPreferences = getSharedPreferences();
        sharedPreferences.edit().putBoolean(KIN_MIGRATION_COMPLETED_KEY, true).apply();
    }

    private boolean isMigrationAlreadyCompleted() {
        // get migration completion status from the persistent state.
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean(KIN_MIGRATION_COMPLETED_KEY, false);
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }

    private Response sendRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, ""))
                .build();
        return okHttpClient.newCall(request).execute();
    }

    private void fireOnError(final MigrationManagerListener migrationManagerListener, final Exception e) {
        Log.d(TAG, "fireOnError: ");
        handler.post(new Runnable() {
            @Override
            public void run() {
                isMigrationInProcess.set(false);
                if (migrationManagerListener != null) {
                    migrationManagerListener.onError(e);
                }
            }
        });
    }

    private void fireOnReady(final MigrationManagerListener migrationManagerListener, final IKinClient kinClient,
                             final boolean needToSave) {
        Log.d(TAG, "fireOnReady: ");
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (needToSave) {
                    saveMigrationCompleted();
                }
                isMigrationInProcess.set(false);
                if (migrationManagerListener != null) {
                    migrationManagerListener.onReady(kinClient);
                }
            }
        });
    }

    private static class RetryInterceptor implements Interceptor {

        private int retryCounter = 1;

        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Log.d(TAG, "intercept: ");
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (response.code() >= 500) {
                response = retryRequest(request, chain);
            }
            retryCounter = 1;
            Log.d(TAG, "INTERCEPTED: " + response.toString());
            return response;
        }

        private Response retryRequest(Request req, Chain chain) throws IOException {
            Log.d(TAG, "Retrying new request");
            Request newRequest = req.newBuilder().build();
            Response another = chain.proceed(newRequest);
            while (retryCounter < MAX_RETRIES && another.code() >= 500) {
                retryCounter++;
                retryRequest(newRequest, chain); // recursive call
            }
            return another;
        }
    }

}