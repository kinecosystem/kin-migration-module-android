package kin.sdk.migration;

import kin.sdk.Logger;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.common.KinSdkVersion;

class MigrationEventsNotifier implements IMigrationEventsListener {

    private final IMigrationEventsListener listener;

    MigrationEventsNotifier(IMigrationEventsListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMethodStarted() {
        Logger.d("onMethodStarted");
        listener.onMethodStarted();
    }

    @Override
    public void onVersionCheckStarted() {
        Logger.d("onVersionCheckStarted");
        listener.onVersionCheckStarted();
    }

    @Override
    public void onVersionCheckSucceeded(KinSdkVersion sdkVersion) {
        Logger.d("onVersionCheckSucceeded sdkVersion = " + sdkVersion.getVersion());
        listener.onVersionCheckSucceeded(sdkVersion);
    }

    @Override
    public void onVersionCheckFailed(Exception exception) {
        Logger.e("onVersionCheckFailed", exception);
        listener.onVersionCheckFailed(exception);
    }

    @Override
    public void onCallbackStart() {
        Logger.d("onCallbackStart");
        listener.onCallbackStart();
    }

    @Override
    public void onCheckBurnStarted(String publicAddress) {
        Logger.d("onCheckBurnStarted publicAddress = " + publicAddress);
        listener.onCheckBurnStarted(publicAddress);
    }

    @Override
    public void onCheckBurnSucceeded(String publicAddress, CheckBurnReason reason) {
        Logger.d("onCheckBurnSucceeded publicAddress = " + publicAddress + " reason = " + reason.value());
        listener.onCheckBurnSucceeded(publicAddress, reason);
    }

    @Override
    public void onCheckBurnFailed(String publicAddress, Exception exception) {
        Logger.e("onCheckBurnFailed publicAddress = " + publicAddress, exception);
        listener.onCheckBurnFailed(publicAddress, exception);
    }

    @Override
    public void onBurnStarted(String publicAddress) {
        Logger.d("onBurnStarted publicAddress = " + publicAddress);
        listener.onBurnStarted(publicAddress);
    }

    @Override
    public void onBurnSucceeded(String publicAddress, BurnReason reason) {
        Logger.d("onBurnSucceeded publicAddress = " + publicAddress + " reason = " + reason.value());
        listener.onBurnSucceeded(publicAddress, reason);
    }

    @Override
    public void onBurnFailed(String publicAddress, Exception exception) {
        Logger.e("onBurnFailed publicAddress = " + publicAddress, exception);
        listener.onBurnFailed(publicAddress, exception);
    }

    @Override
    public void onRequestAccountMigrationStarted(String publicAddress) {
        Logger.d("onRequestAccountMigrationStarted publicAddress = " + publicAddress);
        listener.onRequestAccountMigrationStarted(publicAddress);
    }

    @Override
    public void onRequestAccountMigrationSucceeded(String publicAddress, RequestAccountMigrationSuccessReason reason) {
        Logger.d("onRequestAccountMigrationSucceeded publicAddress = " + publicAddress + " reason = " + reason.value());
        listener.onRequestAccountMigrationSucceeded(publicAddress, reason);
    }

    @Override
    public void onRequestAccountMigrationFailed(String publicAddress, Exception exception) {
        Logger.e("onRequestAccountMigrationFailed publicAddress = " + publicAddress, exception);
        listener.onRequestAccountMigrationFailed(publicAddress, exception);
    }

    @Override
    public void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason) {
        Logger.d("onCallbackReady sdkVersion = " + sdkVersion.getVersion() + " reason = " + selectedSdkReason.value());
        listener.onCallbackReady(sdkVersion, selectedSdkReason);
    }

    @Override
    public void onCallbackFailed(Exception exception) {
        Logger.e("onCallbackFailed", exception);
        listener.onCallbackFailed(exception);
    }
}
