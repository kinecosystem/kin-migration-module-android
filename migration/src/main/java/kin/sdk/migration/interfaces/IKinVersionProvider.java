package kin.sdk.migration.interfaces;

import java.util.HashMap;
import java.util.Map;

import kin.sdk.migration.exception.FailedToResolveSdkVersionException;

public interface IKinVersionProvider {

    enum SdkVersion {
        OLD_KIN_SDK("2"),
        NEW_KIN_SDK("3");

        private final String version;

        // Lookup table
        private static final Map<String, SdkVersion> lookup = new HashMap<>();

        // Populate the lookup table on loading time
        static
        {
            for(SdkVersion sdkVersion : SdkVersion.values())
            {
                lookup.put(sdkVersion.getVersion(), sdkVersion);
            }
        }

        // This method can be used for reverse lookup purpose
        public static SdkVersion get(String url) {
            return lookup.get(url);
        }

        SdkVersion(String version) {
            this.version = version;
        }

        String getVersion() {
            return version;
        }
    }

    SdkVersion getKinSdkVersion(String appId) throws FailedToResolveSdkVersionException;

}
