package kin.sdk.migration.sample;

import kin.sdk.migration.KinSdkVersion;
import kin.sdk.migration.bi.IMigrationEventsListener;

public class SampleMigrationEventsListener implements IMigrationEventsListener {

    @Override
    public void onMethodStarted() {
    }

    @Override
    public void onVersionCheckStarted() {
    }

    @Override
    public void onVersionCheckSucceeded(KinSdkVersion sdkVersion) {
    }

    @Override
    public void onVersionCheckFailed(Exception exception) {
    }

    @Override
    public void onCallbackStart() {
    }

    @Override
    public void onCheckBurnStarted(String publicAddress) {
    }

    @Override
    public void onCheckBurnSucceeded(String publicAddress, CheckBurnReason reason) {
    }

    @Override
    public void onCheckBurnFailed(String publicAddress, Exception exception) {
    }

    @Override
    public void onBurnStarted(String publicAddress) {
    }

    @Override
    public void onBurnSucceeded(String publicAddress, BurnReason reason) {
    }

    @Override
    public void onBurnFailed(String publicAddress, Exception exception) {
    }

    @Override
    public void onRequestAccountMigrationStarted(String publicAddress) {
    }

    @Override
    public void onRequestAccountMigrationSucceeded(String publicAddress, RequestAccountMigrationSuccessReason reason) {
    }

    @Override
    public void onRequestAccountMigrationFailed(String publicAddress, Exception exception) {
    }

    @Override
    public void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason) {
    }

    @Override
    public void onCallbackFailed(Exception exception) {
    }
}
