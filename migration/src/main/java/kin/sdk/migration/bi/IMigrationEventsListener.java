package kin.sdk.migration.bi;

import kin.sdk.migration.KinSdkVersion;

public interface IMigrationEventsListener {

    enum CheckBurnSuccessReason {
        NOT_BURNED,
        ALREADY_BURNED,
        NO_ACCOUNT,
        NO_TRUSTLINE
    }

    enum BurnSuccessReason {
        BURNED,
        ALREADY_BURNED,
        NO_ACCOUNT,
        NO_TRUSTLINE
    }

    enum RequestAccountMigrationSuccessReason {
        MIGRATED,
        ALREADTY_MIGRATED,
        ACCOUNT_NOT_FOUND
    }

    enum SelectedSdkReason {
        MIGRATED,
        ALREADY_MIGREATED,
        NO_ACCOUNT_TO_MIGRATE,
        API_CHECK
    }

    void onMethodStarted();

    void onVersionCheckStarted();

    void onVersionCheckSucceeded(KinSdkVersion sdkVersion);

    void onVersionCheckFailed(Exception exception);

    void onCallbackStart();

    void onCheckBurnStarted(String publicAddress);

    void onCheckBurnSucceeded(String publicAddress, CheckBurnSuccessReason reason);

    void onCheckBurnFailed(String publicAddress, Exception exception);

    void onBurnStarted(String publicAddress);

    void onBurnSucceeded(String publicAddress, BurnSuccessReason reason);

    void onBurnFailed(String publicAddress, Exception exception);

    void onRequestAccountMigrationStarted(String publicAddress);

    void onRequestAccountMigrationSucceeded(String publicAddress, RequestAccountMigrationSuccessReason reason);

    void onRequestAccountMigrationFailed(String publicAddress, Exception exception);

    void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason);

    void onCallbackFailed(Exception exception);

}
