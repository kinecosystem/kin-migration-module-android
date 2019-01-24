package kin.sdk.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import kin.core.ServiceProvider;
import kin.sdk.Environment;
import kin.sdk.Logger;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.bi.IMigrationEventsListener.BurnReason;
import kin.sdk.migration.bi.IMigrationEventsListener.CheckBurnReason;
import kin.sdk.migration.bi.IMigrationEventsListener.RequestAccountMigrationSuccessReason;
import kin.sdk.migration.bi.IMigrationEventsListener.SelectedSdkReason;
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
import kin.sdk.migration.interfaces.IMigrationManagerCallbacks;
import kin.sdk.migration.interfaces.ITransactionId;
import kin.sdk.migration.sdk_related.KinClientSdkImpl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.stellar.sdk.responses.HttpResponseException;

public class MigrationManager {

	private static final String TAG = MigrationManager.class.getSimpleName();
	static final String KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY = "KinMigrationModule";
	static final String KIN_MIGRATION_COMPLETED_KEY = "migration_completed_key";
	private static final int TIMEOUT = 30;
	private static final int MAX_RETRIES = 3;

	private final Context context;
	private final String appId;
	private final MigrationNetworkInfo migrationNetworkInfo;
	private final IKinVersionProvider kinVersionProvider;
	private final String storeKey;
	private final MigrationEventsNotifier eventsNotifier;
	private Handler handler;
	private OkHttpClient okHttpClient;
	private final AtomicBoolean isMigrationInProcess; // defence against multiple calls

	public MigrationManager(@NonNull Context applicationContext, @NonNull String appId,
		@NonNull MigrationNetworkInfo migrationNetworkInfo,
		@NonNull IKinVersionProvider kinVersionProvider,
		@NonNull IMigrationEventsListener eventsListener) {
		this(applicationContext, appId, migrationNetworkInfo, kinVersionProvider, eventsListener, "");
	}

	public MigrationManager(@NonNull Context applicationContext, @NonNull String appId,
		@NonNull MigrationNetworkInfo migrationNetworkInfo,
		@NonNull IKinVersionProvider kinVersionProvider, @NonNull IMigrationEventsListener eventsListener,
		@NonNull String storeKey) {
		this.context = applicationContext.getApplicationContext();
		this.appId = appId;
		this.migrationNetworkInfo = migrationNetworkInfo;
		this.kinVersionProvider = kinVersionProvider;
		this.eventsNotifier = new MigrationEventsNotifier(eventsListener);
		this.storeKey = storeKey;
		isMigrationInProcess = new AtomicBoolean();
	}

	public void enableLogs(boolean enable) {
		Logger.enable(enable);
	}

	/**
	 * @return the current kin client.
	 */
	public IKinClient getCurrentKinClient() {
		final IKinClient kinClient;
		if (isMigrationAlreadyCompleted()) {
			kinClient = initNewKin();
		} else {
			kinClient = initKinCore();
		}
		Logger.d("getLastKinClient sdkVersion = " +
			(kinClient instanceof KinClientCoreImpl ? KinSdkVersion.OLD_KIN_SDK.getVersion()
				: KinSdkVersion.NEW_KIN_SDK.getVersion()));
		return kinClient;
	}

	/**
	 * Starting the migration process from Kin2(Core library) to the new Kin(Sdk library - One Blockchain).
	 * <p><b>Note:</b> This method internally uses a background thread and it is also access the network.</p>
	 * <p><b>Note:</b> If all the migration process will be completed then this method minimum time is 6 seconds.</p>
	 * <p><b>Note:</b> This method should be called only once, if required more then create another instance of this
	 * class.</p>
	 *
	 * @param migrationManagerCallbacks is a listener so the caller can get a callback for completion or error(on the UI
	 * thread).
	 * @throws MigrationInProcessException is thrown in case this method is called while it is not finished.
	 */
	public void start(final IMigrationManagerCallbacks migrationManagerCallbacks) throws MigrationInProcessException {
		eventsNotifier.onMethodStarted();
		if (isMigrationInProcess.compareAndSet(false, true)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Logger.d("starting the migration process in a background thread");
					createHttpClient();
					startMigrationProcess(migrationManagerCallbacks);
				}
			}).start();
		} else {
			Logger.d("Migration is in process, throwing MigrationInProcessException");
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

	private void startMigrationProcess(final IMigrationManagerCallbacks migrationManagerCallbacks) {
		if (isMigrationAlreadyCompleted()) {
			eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.ALREADY_MIGRATED);
			fireOnReady(migrationManagerCallbacks, initNewKin(), false);
		} else {
			try {
				eventsNotifier.onVersionCheckStarted();
				KinSdkVersion kinSdkVersion = kinVersionProvider.getKinSdkVersion();
				if (kinSdkVersion == null) {
					Exception failure = new FailedToResolveSdkVersionException();
					eventsNotifier.onVersionCheckFailed(failure);
					fireOnError(migrationManagerCallbacks, failure);
				} else {
					if (kinSdkVersion == KinSdkVersion.NEW_KIN_SDK) {
						eventsNotifier.onVersionCheckSucceeded(KinSdkVersion.NEW_KIN_SDK);
						burnAndMigrateAccount(migrationManagerCallbacks);
					} else {
						eventsNotifier.onVersionCheckSucceeded(KinSdkVersion.OLD_KIN_SDK);
						eventsNotifier.onCallbackReady(KinSdkVersion.OLD_KIN_SDK, SelectedSdkReason.API_CHECK);
						fireOnReady(
							migrationManagerCallbacks, initKinCore(), false);
					}
				}
			} catch (FailedToResolveSdkVersionException e) {
				eventsNotifier.onVersionCheckFailed(e);
				fireOnError(migrationManagerCallbacks, e);
			}
		}
	}

	private void burnAndMigrateAccount(IMigrationManagerCallbacks migrationManagerCallbacks) {
		KinClientCoreImpl kinClientCore = initKinCore();
		if (kinClientCore.hasAccount()) {
			postMigrationStart(migrationManagerCallbacks);
			KinAccountCoreImpl account = (KinAccountCoreImpl) kinClientCore
				.getAccount(kinClientCore.getAccountCount() - 1);
			String publicAddress = account.getPublicAddress();
			try {
				BurnReason burnSuccessReason = startBurnAccountProcess(publicAddress, account);
				switch (burnSuccessReason) {
					case BURNED:
					case ALREADY_BURNED:
						migrateToNewKin(publicAddress, migrationManagerCallbacks);
						break;
					case NO_ACCOUNT:
					case NO_TRUSTLINE:
						eventsNotifier
							.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
						fireOnReady(migrationManagerCallbacks, initNewKin(), true);
						break;
				}
			} catch (MigrationFailedException e) {
				fireOnError(migrationManagerCallbacks, e);
			}
		} else {
			eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
			fireOnReady(migrationManagerCallbacks, initNewKin(), true);
		}
	}

	private void postMigrationStart(final IMigrationManagerCallbacks migrationManagerCallbacks) {
		eventsNotifier.onCallbackStart();
		handler.post(new Runnable() {
			@Override
			public void run() {
				migrationManagerCallbacks.onMigrationStart();
			}
		});
	}

	/**
	 * Start the burn account process which checks if the account was already burned and if not then burned it.
	 *
	 * @param account is the kin account.
	 * @return the reason.
	 */
	@NonNull
	private BurnReason startBurnAccountProcess(final String publicAddress, final KinAccountCoreImpl account)
		throws MigrationFailedException {
		if (publicAddress != null) {
			CheckBurnReason state = checkAccountBurnedState(account);
			switch (state) {
				case NOT_BURNED:
					return burnAccount(publicAddress, account);
				case ALREADY_BURNED:
					return BurnReason.ALREADY_BURNED;
				case NO_ACCOUNT:
					return BurnReason.NO_ACCOUNT;
				case NO_TRUSTLINE:
					return BurnReason.NO_TRUSTLINE;
				default:
					throw new MigrationFailedException("checkAccountBurnedState returned unexpected result");
			}
		} else {
			throw new MigrationFailedException("account not valid - public address is null");
		}
	}

	@NonNull
	private CheckBurnReason checkAccountBurnedState(final KinAccountCoreImpl kinAccountCore)
		throws MigrationFailedException {
		String publicAddress = kinAccountCore.getPublicAddress();
		eventsNotifier.onCheckBurnStarted(publicAddress);
		int retryCounter = 0;
		while (true) {
			try {
				if (kinAccountCore.isAccountBurned()) {
					eventsNotifier.onCheckBurnSucceeded(publicAddress, CheckBurnReason.ALREADY_BURNED);
					return CheckBurnReason.ALREADY_BURNED;
				} else {
					eventsNotifier.onCheckBurnSucceeded(publicAddress, CheckBurnReason.NOT_BURNED);
					return CheckBurnReason.NOT_BURNED;
				}
			} catch (AccountNotFoundException e) {
				eventsNotifier.onCheckBurnSucceeded(publicAddress, CheckBurnReason.NO_ACCOUNT);
				return CheckBurnReason.NO_ACCOUNT;
			} catch (AccountNotActivatedException e) {
				eventsNotifier.onCheckBurnSucceeded(publicAddress, CheckBurnReason.NO_TRUSTLINE);
				return CheckBurnReason.NO_TRUSTLINE;
			} catch (OperationFailedException e) {
				if (shouldRetry(retryCounter, e)) {
					Logger.d("checkAccountBurnedState: retry number " + retryCounter);
					retryCounter++;
					continue;
				}
				eventsNotifier.onCheckBurnFailed(publicAddress, e);
				throw new MigrationFailedException("Checking if the old account is burned has failed", e);
			}
		}
	}

	private BurnReason burnAccount(String publicAddress, KinAccountCoreImpl account) throws MigrationFailedException {
		eventsNotifier.onBurnStarted(publicAddress);
		int retryCounter = 0;
		while (true) {
			try {
				ITransactionId transactionId = account.sendBurnTransactionSync(publicAddress);
				if (transactionId.id() == null || transactionId.id().isEmpty()) {
					MigrationFailedException exception =
						new MigrationFailedException(
							"Burning the account could not succeed due to an unexpected error, transaction id is empty");
					eventsNotifier.onBurnFailed(publicAddress, exception);
					throw exception;
				} else {
					eventsNotifier.onBurnSucceeded(publicAddress, BurnReason.BURNED);
					return BurnReason.BURNED;
				}
			} catch (AccountNotFoundException e) {
				eventsNotifier.onBurnSucceeded(publicAddress, BurnReason.NO_ACCOUNT);
				return BurnReason.NO_ACCOUNT;
			} catch (AccountNotActivatedException e) {
				eventsNotifier.onBurnSucceeded(publicAddress, BurnReason.NO_TRUSTLINE);
				return BurnReason.NO_TRUSTLINE;
			} catch (OperationFailedException e) {
				if (shouldRetry(retryCounter, e)) {
					Logger.d("burnAccount: retry number " + retryCounter);
					retryCounter++;
					continue;
				}
				eventsNotifier.onBurnFailed(publicAddress, e);
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

	private void migrateToNewKin(final String publicAddress,
		final IMigrationManagerCallbacks migrationManagerCallbacks) {
		eventsNotifier.onRequestAccountMigrationStarted(publicAddress);
		try {
			Response response = sendRequest(migrationNetworkInfo.getMigrationServiceUrl() + publicAddress);
			if (response.isSuccessful()) {
				eventsNotifier
					.onRequestAccountMigrationSucceeded(publicAddress, RequestAccountMigrationSuccessReason.MIGRATED);
				eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.MIGRATED);
				fireOnReady(migrationManagerCallbacks, initNewKin(), true);
			} else {
				handleMigrationException(response, migrationManagerCallbacks, publicAddress);
			}
		} catch (IOException e) {
			eventsNotifier.onRequestAccountMigrationFailed(publicAddress, e);
			fireOnError(migrationManagerCallbacks, e);
		}
	}

	// Handle the migration exceptions, if an exception that is not solvable then throw it, else just handle it.
	private void handleMigrationException(Response response, IMigrationManagerCallbacks migrationManagerCallbacks,
		String publicAddress) {
		// check if account has been migrated successfully and if yes then complete the process and update the persistent state.
		boolean handled = false;
		MigrationFailedException exception = new MigrationFailedException(
			"Migration not completed due to an unexpected exception");
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
							eventsNotifier.onRequestAccountMigrationSucceeded(publicAddress,
								RequestAccountMigrationSuccessReason.ALREADY_MIGRATED);
							eventsNotifier
								.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.ALREADY_MIGRATED);
							fireOnReady(migrationManagerCallbacks, initNewKin(), true);
							handled = true;
							break;
						case "4003":  // public address is not valid, meaning the format of it is not valid.
							exception = new MigrationFailedException(message + ", code = " + code);
							break;
						case "4041":  // account was not found
							eventsNotifier.onRequestAccountMigrationSucceeded(publicAddress,
								RequestAccountMigrationSuccessReason.ACCOUNT_NOT_FOUND);
							eventsNotifier
								.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
							fireOnReady(migrationManagerCallbacks, initNewKin(), true);
							handled = true;
							break;
						default:
							exception = new MigrationFailedException(
								"Got an unexpected migration exception with message: " + message + ", and code: "
									+ code);
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
			eventsNotifier.onRequestAccountMigrationFailed(publicAddress, exception);
			fireOnError(migrationManagerCallbacks, exception);
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

	private void fireOnError(final IMigrationManagerCallbacks migrationManagerCallbacks, final Exception e) {
		eventsNotifier.onCallbackFailed(e);
		handler.post(new Runnable() {
			@Override
			public void run() {
				isMigrationInProcess.set(false);
				if (migrationManagerCallbacks != null) {
					migrationManagerCallbacks.onError(e);
				}
			}
		});
	}

	private void fireOnReady(final IMigrationManagerCallbacks migrationManagerCallbacks, final IKinClient kinClient,
		final boolean needToSave) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (needToSave) {
					saveMigrationCompleted();
				}
				isMigrationInProcess.set(false);
				if (migrationManagerCallbacks != null) {
					migrationManagerCallbacks.onReady(kinClient);
				}
			}
		});
	}

	private static class RetryInterceptor implements Interceptor {

		private int retryCounter = 1;

		@Override
		public Response intercept(@NonNull Chain chain) throws IOException {
			Logger.d("RetryInterceptor, intercept: ");
			Request request = chain.request();
			Response response = chain.proceed(request);
			if (response.code() >= 500) {
				response = retryRequest(request, chain);
			}
			retryCounter = 1;
			Logger.d("RetryInterceptor, intercepted: " + response.toString());
			return response;
		}

		private Response retryRequest(Request req, Chain chain) throws IOException {
			Logger.d("RetryInterceptor, retrying new request");
			Request newRequest = req.newBuilder().build();
			Response another = chain.proceed(newRequest);
			while (retryCounter < MAX_RETRIES && another.code() >= 500) {
				retryCounter++;
				another = retryRequest(newRequest, chain); // recursive call
			}
			return another;
		}
	}

}