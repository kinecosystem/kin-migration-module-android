package kin.sdk.migration.interfaces;

import kin.sdk.migration.exception.FailedToResolveSdkVersionException;

public interface IKinVersionProvider {

    boolean isKinSdkVersion() throws FailedToResolveSdkVersionException;

}
