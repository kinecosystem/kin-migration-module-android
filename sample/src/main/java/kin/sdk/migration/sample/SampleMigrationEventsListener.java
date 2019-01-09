package kin.sdk.migration.sample;

import java.math.BigDecimal;

import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class SampleMigrationEventsListener implements IMigrationEventsListener {
    @Override
    public void onVersionCheckStart() {

    }

    @Override
    public void onVersionReceived(IKinVersionProvider.SdkVersion sdkVersion) {

    }

    @Override
    public void onVersionCheckFailed(Exception exception) {

    }

    @Override
    public void onSDKSelected(boolean isNewSDK, String source) {

    }

    @Override
    public void onAccountBurnStart() {

    }

    @Override
    public void onAccountBurnFailed(Exception exception, BigDecimal balance) {

    }

    @Override
    public void onAccountBurnSuccess() {

    }

    @Override
    public void onMigrationStart() {

    }

    @Override
    public void onMigrationFailed(Exception exception) {

    }

    @Override
    public void onMigrationSuccess(BigDecimal balance) {

    }
}
