package kin.sdk.migration.sample;

import android.util.Log;

import kin.sdk.migration.KinSdkVersion;
import kin.sdk.migration.bi.IMigrationEventsListener;

public class SampleMigrationEventsListener implements IMigrationEventsListener {

    private final static String TAG = SampleMigrationEventsListener.class.getSimpleName();

    @Override
    public void onMethodStarted() {
        Log.d(TAG, "onMethodStarted");
    }

    @Override
    public void onVersionCheckStarted() {
        Log.d(TAG, "onVersionCheckStarted");
    }

    @Override
    public void onVersionCheckSucceeded(KinSdkVersion sdkVersion) {
        Log.d(TAG, "onVersionCheckSucceeded, sdkVersion = " + sdkVersion.name());
    }

    @Override
    public void onVersionCheckFailed(Exception exception) {
        Utils.logError(exception, "onVersionCheckFailed");
    }

    @Override
    public void onCallbackStart() {
        Log.d(TAG, "onCallbackStart");
    }

    @Override
    public void onCheckBurnStarted(String publicAddress) {
        Log.d(TAG, "onCheckBurnStarted, publicAddress = " + publicAddress);
    }

    @Override
    public void onCheckBurnSucceeded(String publicAddress, CheckBurnReason reason) {
        Log.d(TAG, "onCheckBurnSucceeded, publicAddress = " + publicAddress + " reason = " + reason.name());
    }

    @Override
    public void onCheckBurnFailed(String publicAddress, Exception exception) {
        Utils.logError(exception, "onCheckBurnFailed, publicAddress = " + publicAddress);
    }

    @Override
    public void onBurnStarted(String publicAddress) {
        Log.d(TAG, "onBurnStarted, publicAddress = " + publicAddress);
    }

    @Override
    public void onBurnSucceeded(String publicAddress, BurnReason reason) {
        Log.d(TAG, "onBurnSucceeded, publicAddress = " + publicAddress + " reason = " + reason.name());
    }

    @Override
    public void onBurnFailed(String publicAddress, Exception exception) {
        Utils.logError(exception, "onBurnFailed, publicAddress = " + publicAddress);
    }

    @Override
    public void onRequestAccountMigrationStarted(String publicAddress) {
        Log.d(TAG, "onRequestAccountMigrationStarted, publicAddress = " + publicAddress);
    }

    @Override
    public void onRequestAccountMigrationSucceeded(String publicAddress, RequestAccountMigrationSuccessReason reason) {
        Log.d(TAG, "onRequestAccountMigrationSucceeded, publicAddress = " + publicAddress + " reason = " + reason.name());
    }

    @Override
    public void onRequestAccountMigrationFailed(String publicAddress, Exception exception) {
        Utils.logError(exception, "onRequestAccountMigrationFailed, publicAddress = " + publicAddress);
    }

    @Override
    public void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason) {
        Log.d(TAG, "onCallbackReady, sdkVersion = " + sdkVersion.name() + " selectedSdkVersion = " + selectedSdkReason.name());
    }

    @Override
    public void onCallbackFailed(Exception exception) {
        Utils.logError(exception, "onCallbackFailed");
    }
}
