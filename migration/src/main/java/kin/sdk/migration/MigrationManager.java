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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MigrationManager {

	private static final String TAG = MigrationManager.class.getSimpleName();
	private final static String MIGRATION_MODULE_PREFERENCE_FILE_KEY = "MIGRATION_MODULE_PREFERENCE_FILE_KEY";
	private final static String MIGRATION_COMPLETED_KEY = "MIGRATION_COMPLETED_KEY";
	private final static int TIMEOUT = 30;
	private final static int MAX_RETRIES = 3;
	private static final String URL_MIGRATE_ACCOUNT_SERVICE = "https://migration-devplatform-playground.developers.kinecosystem.com/migrate?address=";

	private Context context;
	private final String appId;
	private MigrationNetworkInfo migrationNetworkInfo;
	private IKinVersionProvider kinVersionProvider;
	private String storeKey;
	private Handler handler;
	private OkHttpClient okHttpClient;
	private volatile AtomicBoolean inMigrationProcess; // defence against multiple calls

	public MigrationManager(@NonNull Context context, @NonNull String appId,
		@NonNull MigrationNetworkInfo migrationNetworkInfo,
		@NonNull IKinVersionProvider kinVersionProvider) {
		this(context, appId, migrationNetworkInfo, kinVersionProvider, "");
	}

	public MigrationManager(@NonNull Context context, @NonNull String appId,
		@NonNull MigrationNetworkInfo migrationNetworkInfo,
		@NonNull IKinVersionProvider kinVersionProvider, @NonNull String storeKey) {
		this.context = context;
		this.appId = appId;
		this.migrationNetworkInfo = migrationNetworkInfo;
		this.kinVersionProvider = kinVersionProvider;
		this.storeKey = storeKey;
		inMigrationProcess = new AtomicBoolean();
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
		if (inMigrationProcess.compareAndSet(false, true)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "starting the migration process in a background thread");
					createHttpClient();
					startMigrationProcess(migrationManagerListener);
				}
			}).start();
		} else {
			inMigrationProcess.set(false);
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
				}
				if (kinSdkVersion == KinSdkVersion.NEW_KIN_SDK) {
					Log.d(TAG, "startMigrationProcess: new sdk.");
					burnAndMigrateAccount(migrationManagerListener);
				} else {
					fireOnReady(migrationManagerListener, initKinCore(), false);
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

	private void burnAndMigrateAccount(MigrationManagerListener migrationManagerListener)
		throws OperationFailedException {
		KinClientCoreImpl kinClientCore = initKinCore();
		if (kinClientCore.hasAccount()) {
			migrationManagerListener.onMigrationStart();
			KinAccountCoreImpl account = (KinAccountCoreImpl) kinClientCore
				.getAccount(kinClientCore.getAccountCount() - 1);
			String publicAddress = account.getPublicAddress();
			Log.d(TAG, "startMigrationProcess: retrieve this account: " + publicAddress);
			try {
				boolean burnProcessSucceeded = startBurnAccountProcess(publicAddress, account);
				if (burnProcessSucceeded) {
					migrateToNewKin(publicAddress, migrationManagerListener);
				} else {
					fireOnError(migrationManagerListener,
						new MigrationFailedException("Account could not be burn because of some unexpected exception"));
				}
			} catch (AccountNotFoundException | AccountNotActivatedException e) {
				// If no account has been found or it not activated then we just return a new kin client that wil run on the new blockchain.
				fireOnReady(migrationManagerListener, initNewKin(), true);
			}
		} else {
			fireOnReady(migrationManagerListener, initNewKin(), true);
		}
	}

	/**
	 * Start the burn account process which checks if the account was already burned and if not then burned it.
	 *
	 * @param account is the kin account.
	 * @return true if the process completed. Also note that it can complete even if the account was already burned.
	 * @throws MigrationFailedException a general Migration Exception with a concrete message about the exception.
	 */
	private boolean startBurnAccountProcess(final String publicAddress, final KinAccountCoreImpl account)
		throws OperationFailedException {
		if (publicAddress != null) {
			if (isAccountBurned(account)) {
				Log.d(TAG, "startBurnAccountProcess: account is already burned");
				return true;
			} else {
				return burnAccount(publicAddress, account);
			}
		} else {
			throw new MigrationFailedException("account not valid");
		}
	}

	private boolean isAccountBurned(final KinAccountCoreImpl kinAccountCore) throws OperationFailedException {
		int retryCounter = 0;
		while (true) {
			try {
				Log.d(TAG, "isAccountBurned: ");
				return kinAccountCore.isAccountBurned();
			} catch (OperationFailedException e) {
				Throwable cause = e.getCause();
				// Check if it is an http exception with 5xx code and if yes then retry until max tries reached.
				if (cause instanceof HttpResponseException) {
					HttpResponseException httpException = (HttpResponseException) cause;
					if (httpException.getStatusCode() >= 500 && retryCounter < MAX_RETRIES) {
						retryCounter++;
						Log.d(TAG, "isAccountBurned: retry number " + retryCounter);
						continue;
					}
				}
				throw e;
			}
		}
	}

	private boolean burnAccount(String publicAddress, KinAccountCoreImpl account) throws MigrationFailedException {
		int retryCounter = 0;
		while (true) {
			try {
				Log.d(TAG, "burnAccount: ");
				ITransactionId transactionId = account.sendBurnTransactionSync(publicAddress);
				return transactionId.id() != null && !transactionId.id().isEmpty();
			} catch (OperationFailedException e) {
				Throwable cause = e.getCause();
				// Check if it is an http exception with 5xx code and if yes then retry until max tries reached.
				if (cause instanceof HttpResponseException) { // TODO: 30/12/2018 maybe need to catch more exception for the retry? like java.net.ConnectException?
					HttpResponseException httpException = (HttpResponseException) cause;
					if (httpException.getStatusCode() >= 500 && retryCounter < MAX_RETRIES) {
						retryCounter++;
						Log.d(TAG, "burnAccount: retry number " + retryCounter);
						continue;
					}
				}
				throw new MigrationFailedException(e);
			}
		}
	}

	private void migrateToNewKin(final String publicAddress, final MigrationManagerListener migrationManagerListener) {
		Log.d(TAG, "migrateToNewKin: sending the request to migrate");
		sendRequest(URL_MIGRATE_ACCOUNT_SERVICE + publicAddress, new Callback() {
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e) {
				fireOnError(migrationManagerListener, e);
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) {
				if (response.isSuccessful()) {
					fireOnReady(migrationManagerListener, initNewKin(), true);
				} else {
					handleMigrationException(response.body(), migrationManagerListener);
				}
			}
		});
	}

	// Handle the migration exceptions, if an exception that is not solvable then throw it, else just handle it.
	private void handleMigrationException(ResponseBody body, MigrationManagerListener migrationManagerListener) {
		// check if account has been migrated successfully and if yes then complete the process and update the persistent state.
		Log.d(TAG, "generateMigrationException: ");
		boolean handled = false;
		MigrationFailedException exception = new MigrationFailedException(
			"Migration not completed due to an unexpected exception");
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
							exception = new MigrationFailedException(message);
							break;
						case "4002":  // account was already migrated
							fireOnReady(migrationManagerListener, initNewKin(), true);
							handled = true;
							break;
						case "4003":  // public address is not valid, meaning the format of it is not valid.
							exception = new MigrationFailedException(message);
							break;
						case "4041":  // account was not found
							fireOnReady(migrationManagerListener, initNewKin(), true);
							handled = true;
							break;
						default:
							exception = new MigrationFailedException();
							break;
					}
				}
			} catch (IOException e) {
				exception = new MigrationFailedException(e);
			}
		} else {
			exception = new MigrationFailedException();
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
		sharedPreferences.edit().putBoolean(MIGRATION_COMPLETED_KEY, true).apply();
	}

	private boolean isMigrationAlreadyCompleted() {
		// get migration completion status from the persistent state.
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getBoolean(MIGRATION_COMPLETED_KEY, false);
	}

	private SharedPreferences getSharedPreferences() {
		return context.getSharedPreferences(MIGRATION_MODULE_PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
	}

	private void sendRequest(String url, Callback callback) {
		Request request = new Request.Builder()
			.url(url)
			.post(RequestBody.create(null, ""))
			.build();
		okHttpClient.newCall(request).enqueue(callback);
	}

	private void fireOnError(final MigrationManagerListener migrationManagerListener, final Exception e) {
		Log.d(TAG, "fireOnError: ");
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (migrationManagerListener != null) {
					inMigrationProcess.set(false);
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
				if (migrationManagerListener != null) {
					if (needToSave) {
						saveMigrationCompleted();
					}
					inMigrationProcess.set(false);
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
				return retryRequest(request, chain);
			}
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
			retryCounter = 1;
			return another;
		}
	}

}
