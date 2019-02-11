package kin.sdk.migration.common.interfaces;

import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.FailedToResolveSdkVersionException;

public interface IKinVersionProvider {

    KinSdkVersion getKinSdkVersion() throws FailedToResolveSdkVersionException;

}
