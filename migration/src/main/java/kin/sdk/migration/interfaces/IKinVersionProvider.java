package kin.sdk.migration.interfaces;

import kin.sdk.migration.KinSdkVersion;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;

public interface IKinVersionProvider {

    KinSdkVersion getKinSdkVersion(String appId) throws FailedToResolveSdkVersionException;

}
