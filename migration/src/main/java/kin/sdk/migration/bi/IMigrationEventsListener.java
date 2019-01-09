package kin.sdk.migration.bi;

import java.math.BigDecimal;

import kin.sdk.migration.interfaces.IKinVersionProvider;

public interface IMigrationEventsListener {

    void onVersionCheckStart();
    void onVersionReceived(IKinVersionProvider.SdkVersion sdkVersion);
    void onVersionCheckFailed(Exception exception);
    void onSDKSelected(boolean isNewSDK, String source);

    void onAccountBurnStart();
    void onAccountBurnBalanceReceived(BigDecimal balance);
    void onAccountBurnFailed(Exception exception, BigDecimal balance);
    void onAccountBurnSuccess();

    void onMigrationStart();
    void onMigrationFailed(Exception exception);
    void onMigrationSuccess(BigDecimal balance);

}
