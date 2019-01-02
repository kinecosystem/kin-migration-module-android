package kin.sdk.migration.interfaces;

import kin.sdk.migration.exception.FailedToResolveSdkVersionException;

public interface IKinVersionProvider {

    boolean isNewKinSdkVersion(String appId) throws FailedToResolveSdkVersionException;

}
