package kin.sdk.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import kin.core.ServiceProvider;
import kin.sdk.Environment;
import kin.sdk.Logger;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.bi.IMigrationEventsListener.BurnReason;
import kin.sdk.migration.bi.IMigrationEventsListener.SelectedSdkReason;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.AccountNotFoundInListOfAccounts;
import kin.sdk.migration.common.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.common.exception.MigrationFailedException;
import kin.sdk.migration.common.exception.MigrationInProcessException;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IKinClient;
import kin.sdk.migration.common.interfaces.IKinVersionProvider;
import kin.sdk.migration.common.interfaces.IMigrationManagerCallbacks;
import kin.sdk.migration.internal.core_related.KinAccountCoreImpl;
import kin.sdk.migration.internal.core_related.KinClientCoreImpl;
import kin.sdk.migration.internal.sdk_related.KinClientSdkImpl;

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
	 * @param sdkVersion is the sdk version on which the KinClient should run. The sdk version should be the same as in
	 * your servers.
	 * @return the kin client.
	 */
	public IKinClient getKinClient(KinSdkVersion sdkVersion) {
		Logger.d("getCurrentKinClient - sdkVersion = " + sdkVersion.getVersion());
		return sdkVersion == KinSdkVersion.NEW_KIN_SDK ? initNewKin() : initKinCore();
	}

	/**
	 * Check locally if the account is already migrated. if the account is null, empty or not found then this method
	 * will return false.
	 *
	 * @param publicAddress is the address of the account to check.
	 * @return true if account is already migrated(checking this locally), false otherwise.
	 */
	public boolean accountAlreadyMigrated(String publicAddress) {
		return isMigrationAlreadyCompleted(publicAddress);
	}

	/**
	 * Starting the migration process from Kin2(Core library) to the new Kin(Sdk library - One Blockchain).
	 * <p><b>Note:</b> This method internally uses a background thread and it is also access the network.</p>
	 * <p><b>Note:</b> If all the migration process will be completed then this method minimum time is 6 seconds.</p>
	 * <p><b>Note:</b> This method should be called only once, if required more then create another instance of this
	 * class.</p>
	 *
	 * @param migrationManagerCallbacks is a listener so the caller can get a callback for completion or error(on the UI
	 * 0thread).
	 * @throws MigrationInProcessException is thrown in case this method is called while it is not finished.
	 */
	public void start(final IMigrationManagerCallbacks migrationManagerCallbacks)
		throws MigrationInProcessException {
		start(null, migrationManagerCallbacks);
	}

	/**
	 * Starting the migration process from Kin2(Core library) to the new Kin(Sdk library - One Blockchain).
	 * <p><b>Note:</b> This method internally uses a background thread and it is also access the network.</p>
	 * <p><b>Note:</b> If all the migration process will be completed then this method minimum time is 6 seconds.</p>
	 * <p><b>Note:</b> This method should be called only once, if required more then create another instance of this
	 * class.</p>
	 *
	 * @param publicAddress the address of the account to migrate.
	 * @param migrationManagerCallbacks is a listener so the caller can get a callback for completion or error(on the UI
	 * thread).
	 * @throws MigrationInProcessException is thrown in case this method is called while it is not finished.
	 */
	public void start(final String publicAddress, final IMigrationManagerCallbacks migrationManagerCallbacks)
		throws MigrationInProcessException {
		eventsNotifier.onMethodStarted();
		if (isMigrationInProcess.compareAndSet(false, true)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Logger.d("starting the migration process in a background thread");
					startMigrationProcess(migrationManagerCallbacks, publicAddress);
				}
			}).start();
		} else {
			Logger.d("Migration is in process, throwing MigrationInProcessException");
			throw new MigrationInProcessException("You can't start migration while migration is still in process");
		}
	}

	private void startMigrationProcess(final IMigrationManagerCallbacks migrationManagerCallbacks,
		final String publicAddress) {
		final IKinClient newKinClient = initNewKin();
		if (isMigrationAlreadyCompleted(publicAddress)) {
			eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.ALREADY_MIGRATED);
			fireOnReady(migrationManagerCallbacks, newKinClient, publicAddress, false);
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
						burnAndMigrateAccount(newKinClient, publicAddress, migrationManagerCallbacks);
					} else {
						eventsNotifier.onVersionCheckSucceeded(KinSdkVersion.OLD_KIN_SDK);
						eventsNotifier.onCallbackReady(KinSdkVersion.OLD_KIN_SDK, SelectedSdkReason.API_CHECK);
						fireOnReady(migrationManagerCallbacks, initKinCore(), publicAddress, false);
					}
				}
			} catch (FailedToResolveSdkVersionException e) {
				eventsNotifier.onVersionCheckFailed(e);
				fireOnError(migrationManagerCallbacks, e);
			}
		}
	}

	private void burnAndMigrateAccount(final IKinClient newKinClient, String publicAddress,
		final IMigrationManagerCallbacks migrationManagerCallbacks) {
		KinClientCoreImpl kinClientCore = initKinCore();
		if (kinClientCore.hasAccount() && publicAddress != null && !publicAddress.isEmpty()) {
			postMigrationStart(migrationManagerCallbacks);
			KinAccountCoreImpl account = getKinAccountCore(kinClientCore, publicAddress);
			try {
				AccountBurner accountBurner = new AccountBurner(eventsNotifier);
				BurnReason burnSuccessReason = accountBurner.start(account);
				switch (burnSuccessReason) {
					case BURNED:
					case ALREADY_BURNED:
						AccountMigrator accountMigrator = new AccountMigrator(eventsNotifier, migrationNetworkInfo);
						try {
							accountMigrator.migrateToNewKin(publicAddress);
							fireOnReady(migrationManagerCallbacks, newKinClient, publicAddress, true);
						} catch (Exception e) {
							fireOnError(migrationManagerCallbacks, e);
						}
						break;
					case NO_ACCOUNT:
					case NO_TRUSTLINE:
						eventsNotifier
							.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
						fireOnReady(migrationManagerCallbacks, newKinClient, publicAddress, true);
						break;
				}
			} catch (MigrationFailedException e) {
				fireOnError(migrationManagerCallbacks, e);
			}
		} else {
			eventsNotifier.onCallbackReady(KinSdkVersion.NEW_KIN_SDK, SelectedSdkReason.NO_ACCOUNT_TO_MIGRATE);
			fireOnReady(migrationManagerCallbacks, newKinClient, publicAddress, true);
		}
	}

	private KinAccountCoreImpl getKinAccountCore(KinClientCoreImpl kinClientCore, String publicAddress) {
		IKinAccount kinAccount = null;
		if (kinClientCore != null && !TextUtils.isEmpty(publicAddress)) {
			int numOfAccounts = kinClientCore.getAccountCount();
			for (int i = 0; i < numOfAccounts; i++) {
				IKinAccount account = kinClientCore.getAccount(i);
				if (account != null && account.getPublicAddress().equals(publicAddress)) {
					kinAccount = account;
					break;
				}
			}
		}
		return (KinAccountCoreImpl) kinAccount;
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

	private boolean isMigrationAlreadyCompleted(String publicAddress) {
		// get migration completion status from the persistent state.
		boolean isMigrationAlreadyCompleted = false;
		if (!TextUtils.isEmpty(publicAddress)) {
			SharedPreferences sharedPreferences = getSharedPreferences();
			isMigrationAlreadyCompleted = sharedPreferences
				.getBoolean(KIN_MIGRATION_COMPLETED_KEY + publicAddress, false);
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
		final String publicAddress, final boolean needToSave) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				isMigrationInProcess.set(false);
				boolean publicAddressIsEmpty = TextUtils.isEmpty(publicAddress);
				// If no accounts and no public address then that mean we can supply a kinClient object to begin with.
				// Or if the account has been found in the list of account then we can supply that kinClient which includes this account.
				if (!kinClient.hasAccount() && publicAddressIsEmpty
					|| isAccountFoundInListOfAccounts(kinClient, publicAddress)) {
					if (needToSave && !publicAddressIsEmpty) {
						saveMigrationCompleted(publicAddress);
					}
					if (migrationManagerCallbacks != null) {
						migrationManagerCallbacks.onReady(kinClient);
					}
					// If the account wasn't found in the list of account then we throw the next exception
				} else {
					fireOnError(migrationManagerCallbacks, new AccountNotFoundInListOfAccounts());
				}
			}
		});
	}

	private boolean isAccountFoundInListOfAccounts(IKinClient kinClient, String publicAddress) {
		boolean isAccountFoundInListOfAccounts = false;
		// If we have at least one account and they didn't supply public address or if we have at least one account
		// and they supply a public address but it wasn't found in the list of accounts twe will return false.
		if (kinClient.hasAccount() && !TextUtils.isEmpty(publicAddress)) {
			if (!TextUtils.isEmpty(publicAddress)) {
				int numOfAccounts = kinClient.getAccountCount();
				for (int i = 0; i < numOfAccounts; i++) {
					IKinAccount account = kinClient.getAccount(i);
					if (account != null && account.getPublicAddress().equals(publicAddress)) {
						isAccountFoundInListOfAccounts = true;
						break;
					}
				}
			}
		}
		return isAccountFoundInListOfAccounts;
	}

}