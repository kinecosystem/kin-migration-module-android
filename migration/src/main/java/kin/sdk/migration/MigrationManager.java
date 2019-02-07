package kin.sdk.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import java.util.concurrent.atomic.AtomicBoolean;
import kin.core.ServiceProvider;
import kin.sdk.Environment;
import kin.sdk.Logger;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.bi.IMigrationEventsListener.BurnReason;
import kin.sdk.migration.bi.IMigrationEventsListener.SelectedSdkReason;
import kin.sdk.migration.core_related.KinAccountCoreImpl;
import kin.sdk.migration.core_related.KinClientCoreImpl;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.exception.MigrationFailedException;
import kin.sdk.migration.exception.MigrationInProcessException;
import kin.sdk.migration.interfaces.IKinAccount;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IKinVersionProvider;
import kin.sdk.migration.interfaces.IMigrationManagerCallbacks;
import kin.sdk.migration.sdk_related.KinClientSdkImpl;

public class MigrationManager {

	private static final String TAG = MigrationManager.class.getSimpleName();
	static final String KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY = "KinMigrationModule";
	static final String KIN_MIGRATION_COMPLETED_KEY = "migration_completed_key_";

	private final Context context;
	private final String appId;
	private final MigrationNetworkInfo migrationNetworkInfo;
	private final IKinVersionProvider kinVersionProvider;
	private final String storeKey;
	private final MigrationEventsNotifier eventsNotifier;
	private final AtomicBoolean isMigrationInProcess; // defence against multiple calls
	private final Handler handler;

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
		handler = new Handler(Looper.getMainLooper());
	}

	public void enableLogs(boolean enable) {
		Logger.enable(enable);
	}

	/**
	 * @return the current kin client.
	 */
	public IKinClient getCurrentKinClient() {
		IKinClient kinClient = initNewKin();
		if (!isMigrationAlreadyCompleted(kinClient)) {
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
					startMigrationProcess(migrationManagerCallbacks);
				}
			}).start();
		} else {
			Logger.d("Migration is in process, throwing MigrationInProcessException");
			throw new MigrationInProcessException("You can't start migration while migration is still in process");
		}
	}

	private void startMigrationProcess(final IMigrationManagerCallbacks migrationManagerCallbacks) {
		final IKinClient newKinClient = initNewKin();
		if (isMigrationAlreadyCompleted(newKinClient)) {
			eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.ALREADY_MIGRATED);
			fireOnReady(migrationManagerCallbacks, newKinClient, false);
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
						burnAndMigrateAccount(newKinClient, migrationManagerCallbacks);
					} else {
						eventsNotifier.onVersionCheckSucceeded(KinSdkVersion.OLD_KIN_SDK);
						eventsNotifier.onCallbackReady(KinSdkVersion.OLD_KIN_SDK, SelectedSdkReason.API_CHECK);
						fireOnReady(migrationManagerCallbacks, initKinCore(), false);
					}
				}
			} catch (FailedToResolveSdkVersionException e) {
				eventsNotifier.onVersionCheckFailed(e);
				fireOnError(migrationManagerCallbacks, e);
			}
		}
	}

	private void burnAndMigrateAccount(final IKinClient newKinClient,
		final IMigrationManagerCallbacks migrationManagerCallbacks) {
		KinClientCoreImpl kinClientCore = initKinCore();
		if (kinClientCore.hasAccount()) {
			postMigrationStart(migrationManagerCallbacks);
			KinAccountCoreImpl account = (KinAccountCoreImpl) kinClientCore
				.getAccount(kinClientCore.getAccountCount() - 1);
			String publicAddress = account.getPublicAddress();
			try {
				AccountBurner accountBurner = new AccountBurner(eventsNotifier);
				BurnReason burnSuccessReason = accountBurner.startBurnAccountProcess(account);
				switch (burnSuccessReason) {
					case BURNED:
					case ALREADY_BURNED:
						AccountMigrator accountMigrator = new AccountMigrator(eventsNotifier, migrationNetworkInfo);
						try {
							accountMigrator.migrateToNewKin(publicAddress);
							fireOnReady(migrationManagerCallbacks, newKinClient, true);
						} catch (Exception e) {
							fireOnError(migrationManagerCallbacks, e);
						}
						break;
					case NO_ACCOUNT:
					case NO_TRUSTLINE:
						eventsNotifier
							.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
						fireOnReady(migrationManagerCallbacks, newKinClient, true);
						break;
				}
			} catch (MigrationFailedException e) {
				fireOnError(migrationManagerCallbacks, e);
			}
		} else {
			eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
			fireOnReady(migrationManagerCallbacks, newKinClient, true);
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

	private void saveMigrationCompleted(String publicAddress) {
		// save migration completion status from the persistent state.
		SharedPreferences sharedPreferences = getSharedPreferences();
		sharedPreferences.edit().putBoolean(KIN_MIGRATION_COMPLETED_KEY + publicAddress, true).apply();
	}

	private boolean isMigrationAlreadyCompleted(IKinClient newKinClient) {
		// get migration completion status from the persistent state.
		boolean isMigrationAlreadyCompleted = false;
		if (newKinClient.hasAccount()) {
			IKinAccount account = newKinClient.getAccount(newKinClient.getAccountCount() - 1);
			SharedPreferences sharedPreferences = getSharedPreferences();
			isMigrationAlreadyCompleted = sharedPreferences
				.getBoolean(KIN_MIGRATION_COMPLETED_KEY + account.getPublicAddress(), false);
		}
		return isMigrationAlreadyCompleted;
	}

	private SharedPreferences getSharedPreferences() {
		return context.getSharedPreferences(KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
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
				if (needToSave && kinClient.hasAccount()) {
					IKinAccount account = kinClient.getAccount(kinClient.getAccountCount() - 1);
					String publicAddress = account.getPublicAddress();
					saveMigrationCompleted(publicAddress);
				}
				isMigrationInProcess.set(false);
				if (migrationManagerCallbacks != null) {
					migrationManagerCallbacks.onReady(kinClient);
				}
			}
		});
	}

}