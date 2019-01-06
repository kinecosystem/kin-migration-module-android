package kin.sdk.migration.interfaces;

import kin.sdk.migration.exception.FailedToResolveSdkVersionException;

public interface IKinVersionProvider {

    enum SdkVersion {
        OLD_KIN_SDK("2"),
        NEW_KIN_SDK("3");

        private final String version;

        SdkVersion(String version) {
            this.version = version;
        }

        String getVersion() {
            return version;
        }
    }

    SdkVersion getKinSdkVersion(String appId) throws FailedToResolveSdkVersionException;

}
