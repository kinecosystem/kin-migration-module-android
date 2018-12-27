package kin.sdk.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
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
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.interfaces.MigrationManagerListener;
import kin.sdk.migration.sdk_related.KinClientSdkImpl;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MigrationManager {

    private static final String TAG = MigrationManager.class.getSimpleName();
    private final static String MIGRATION_MODULE_PREFERENCE_FILE_KEY = "MIGRATION_MODULE_PREFERENCE_FILE_KEY";
    private final static String MIGRATION_COMPLETED_KEY = "MIGRATION_COMPLETED_KEY";
    private static final String URL_MIGRATE_ACCOUNT = "http";

    private final Context context;
    private final String appId;
    private final MigrationNetworkInfo migrationNetworkInfo;
    private final IKinVersionProvider kinVersionProvider;
    private final IWhitelistService whitelistService;
    private String storeKey;
    private Handler handler;
    private OkHttpClient okHttpClient;
    private boolean inMigrationProcess; // defence against multiple calls

    public MigrationManager(@NonNull Context context, @NonNull String appId, @NonNull MigrationNetworkInfo migrationNetworkInfo,
                            @NonNull IKinVersionProvider kinVersionProvider, @NonNull IWhitelistService whitelistService) { // TODO: 06/12/2018 maybe also add the eventLogger
        this(context, appId, migrationNetworkInfo, kinVersionProvider, whitelistService, "");
    }

    public MigrationManager(@NonNull Context context, @NonNull String appId, @NonNull MigrationNetworkInfo migrationNetworkInfo,
                            @NonNull IKinVersionProvider kinVersionProvider, @NonNull IWhitelistService whitelistService, @NonNull String storeKey) { // TODO: 06/12/2018 maybe also add the eventLogger
        this.context = context;
        this.appId = appId;
        this.migrationNetworkInfo = migrationNetworkInfo;
        this.kinVersionProvider = kinVersionProvider;
        this.whitelistService = whitelistService;
        this.storeKey = storeKey;
    }

    /**
     * Starting the migration process from Kin2(Core library) to the new Kin(Sdk library - One Blockchain).
     * <p><b>Note:</b> This method internally uses a background thread and it is also access the network.</p>
     * @param migrationManagerListener is a listener so the caller can get a callback for completion or error(on the UI thread).
     * @throws MigrationInProcessException is thrown in case this method is called while it is not finished.
     */
    public void startMigration(final MigrationManagerListener migrationManagerListener) throws MigrationInProcessException {
        // TODO: 12/12/2018 this is an api call so we need to make sure the one who implement it knows that
        // TODO: 12/12/2018 Maybe we should use actual boolean in the constructor or in the init method instead of using it as an interface?
        if (!inMigrationProcess) {
            new Thread(new Runnable() {
                @Override
                public void run() {
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
        handler = new Handler(Looper.getMainLooper()); // TODO: 23/12/2018 maybe put it in the constructor
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(retryInterceptor)
                .build();
    }

    private void startMigrationProcess(final MigrationManagerListener migrationManagerListener) {
        if (isMigrationAlreadyCompleted()) {
            // TODO: 24/12/2018 it really didn't complete because actually it was already completed in the past so need to pass this info to developer or change method name
            fireOnComplete(migrationManagerListener, initNewKin(), false);
        } else {
            try {
                if (kinVersionProvider.isKinSdkVersion()) {
                    KinClientCoreImpl kinClientCore = initKinCore();
                    if (kinClientCore.hasAccount()) {
                        if (startBurnAccountProcess(kinClientCore)) {
                            migrateToNewKin(migrationManagerListener);
                        } else {
                            fireOnError(migrationManagerListener, new MigrationFailedException("Account could not be burn because of some unexpected exception"));
                        }
                    } else {
                        // TODO: 24/12/2018 and maybe make sure that he has an account or create it (check it because we are creating all the account manually in server)
                        fireOnComplete(migrationManagerListener, initNewKin(), true);
                    }
                } else {
                    // TODO: 24/12/2018 it really didn't complete because actually there was no migration yet so need to pass this info to developer
                    fireOnComplete(migrationManagerListener, initKinCore(), false);
                }
            } catch (FailedToResolveSdkVersionException e) {
                fireOnError(migrationManagerListener, e);
            } catch (MigrationFailedException e) {
                fireOnError(migrationManagerListener, e);
            } catch (OperationFailedException e) {
                fireOnError(migrationManagerListener, new MigrationFailedException(e));
            }
        }
    }

    /**
     * Start the burn account process which checks if the account was already burned and if not then burned it.
     * @param kinClientCore is the kin client from the core library.
     * @return true if the process completed. Also note that it can complete even if the account was already burned.
     * @throws MigrationFailedException a general Migration Exception with a concrete message about the exception.
     */
    private boolean startBurnAccountProcess(KinClientCoreImpl kinClientCore) throws OperationFailedException {
        // TODO: 24/12/2018 check if account count -1 really works
        KinAccountCoreImpl account = (KinAccountCoreImpl) kinClientCore.getAccount(kinClientCore.getAccountCount() -1);
        String publicAddress = account.getPublicAddress();
        if (publicAddress != null) {
            if (isAccountBurned(publicAddress, account)) {
                return true;
            } else {
                return burnAccount(publicAddress, account);
            }
        } else {
            throw new MigrationFailedException("account not valid"); // TODO: 25/12/2018 maybe account not found or not exist?
        }
    }

    private boolean isAccountBurned(String publicAddress, KinAccountCoreImpl kinAccountCore) throws OperationFailedException {
        if (publicAddress != null) {
            try {
                return kinAccountCore.isAccountBurned(publicAddress);
            } catch (AccountNotFoundException e) {
                throw new MigrationFailedException("Account " + e.getAccountId() + " was not found");
            } catch (AccountNotActivatedException e) {
                throw new MigrationFailedException("Account " + e.getAccountId() + " is not activated");
            } catch (OperationFailedException e) {
                // TODO: 25/12/2018 add retry mechanism for specific errors
                throw new MigrationFailedException(e.getMessage(), e.getCause());
            }
        } else {
            throw new MigrationFailedException("account not valid");
        }
    }

    private boolean burnAccount(String publicAddress, KinAccountCoreImpl account) throws MigrationFailedException {
        try {
            ITransactionId transactionId = account.sendBurnTransactionSync(publicAddress);
            return transactionId.id() != null && !transactionId.id().isEmpty();
        } catch (AccountNotFoundException e) {
            throw new MigrationFailedException("Account " + e.getAccountId() + " was not found");
        } catch (AccountNotActivatedException e) {
            throw new MigrationFailedException("Account " + e.getAccountId() + " is not activated");
        } catch (OperationFailedException e) {
            // TODO: 25/12/2018 add retry mechanism for specific errors
            throw new MigrationFailedException(e.getMessage(), e.getCause());
        }
    }

    @NonNull
    private KinClientCoreImpl initKinCore() {
        ServiceProvider environment = new ServiceProvider(migrationNetworkInfo.getCoreNetworkUrl(), migrationNetworkInfo.getCoreNetworkId()) {
            @Override
            protected String getIssuerAccountId() {
                return migrationNetworkInfo.getIssuer();
            }
        };
        return new KinClientCoreImpl(context, environment, storeKey);
    }

    @NonNull
    private IKinClient initNewKin() {
        IKinClient kinClient;
        Environment environment = new Environment(migrationNetworkInfo.getSdkNetworkUrl(), migrationNetworkInfo.getSdkNetworkId());
        kinClient = new KinClientSdkImpl(context, environment, appId, whitelistService, storeKey);
        return kinClient;
    }

    private void migrateToNewKin(final MigrationManagerListener migrationManagerListener) {
//        sendRequest(URL_MIGRATE_SERVICE, new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                // TODO: 23/12/2018 implement retry mechanism only on errors that require a retry
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                // TODO: 23/12/2018 if this is running on the ui thread then do the work a background thread
//                // TODO: 23/12/2018 check if response is ok and that indeed the account has been burned
//                if (accountHasMigrated(response)) {
//                    migrateToNewKin(); // TODO or insert a retry in the interceptors
//                } else {
//                    // TODO: 23/12/2018  implement retry mechanism because somehow it didn't migrate
//                }
//            }
//        });
    }

    private boolean isMigrationAlreadyCompleted() {
        // get migration completion status from the persistent state.
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean(MIGRATION_COMPLETED_KEY, false);
    }

    private void saveMigrationCompleted() {
        // save migration completion status from the persistent state.
        SharedPreferences sharedPreferences = getSharedPreferences();
        sharedPreferences.edit().putBoolean(MIGRATION_COMPLETED_KEY, true).apply();
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(MIGRATION_MODULE_PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }

    private boolean accountHasMigrated(Response response) {
        // check if account has been migrated successfully and if yes then complete the process and update the persistent state
        return false;
    }

    private void sendRequest(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    private void fireOnError(final MigrationManagerListener migrationManagerListener, final Exception e) {
        // TODO: 25/12/2018 maybe clean some resources here?
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (migrationManagerListener != null) {
                    inMigrationProcess = false;
                    migrationManagerListener.onError(e);
                }
            }
        });
    }

    private void fireOnComplete(final MigrationManagerListener migrationManagerListener, final IKinClient kinClient, final boolean needToSave) {
        // TODO: 25/12/2018 maybe clean some resources here?
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (migrationManagerListener != null) {
                    if (needToSave) {
                        saveMigrationCompleted();
                    }
                    inMigrationProcess = false;
                    migrationManagerListener.onComplete(kinClient);
                }
            }
        });
    }

    private static class RetryInterceptor implements Interceptor {

        private final String TAG = getClass().getSimpleName();

        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (response.code() != 200) { // TODO: 23/12/2018 probably only 500 need a retry
                Response r = null;
                return retryRequest(request, chain);
            }
            Log.d(TAG, "INTERCEPTED: " + response.toString());
            return response;
        }

        private Response retryRequest(Request req, Chain chain) throws IOException {
            Log.d(TAG, "Retrying new request");
            // make a new request which is same as the original one.
            Request newRequest = req.newBuilder() // TODO: 23/12/2018 check if this request still has the same url as before
                    .build();
            Response another = chain.proceed(newRequest);
            while (another.code() != 200) { // TODO: 23/12/2018 use some retry const in order to retry only X times and do it only for specific errors
                retryRequest(newRequest, chain);
            }
            return another;
        }
    }

}
