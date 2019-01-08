package kin.sdk.migration.bi;

import kin.sdk.migration.interfaces.IKinVersionProvider;

public interface IMigrationEventsListener {

    void onVersionCheckStart();
    void onVesrsionReceived(IKinVersionProvider.SdkVersion sdkVersion);
    void onVersionCheckFailed(Exception exception);
    void onSDKSelected(boolean isNewSDK, String source);

    void onAccountBurnStart();
    void onAccountBurnBalanceReceived(String balance);
    void onAccountBurnFailed(Exception exception, String balance);
    void onAccountBurnSuccess();

    void onMigrationStart();
    void onMigrationFailed(Exception exception);
    void onMigrationSuccess(String balance);

}
