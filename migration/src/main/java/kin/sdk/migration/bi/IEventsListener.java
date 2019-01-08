package kin.sdk.migration.bi;

import kin.sdk.migration.interfaces.IKinVersionProvider;

public interface IEventsListener {

    void onVersionCheckStart();
    void onVesrsionReceived(IKinVersionProvider.SdkVersion sdkVersion);
    void onVersionCheckFailed(Exception exception);
}
