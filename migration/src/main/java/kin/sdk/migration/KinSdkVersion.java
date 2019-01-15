package kin.sdk.migration;

import java.util.HashMap;
import java.util.Map;

public enum KinSdkVersion {

	OLD_KIN_SDK("2"),
	NEW_KIN_SDK("3");

	private final String version;

	// Lookup table
	private static final Map<String, KinSdkVersion> lookup = new HashMap<>();

	// Populate the lookup table on loading time
	static
	{
		for(KinSdkVersion sdkVersion : KinSdkVersion.values())
		{
			lookup.put(sdkVersion.getVersion(), sdkVersion);
		}
	}

	// This method can be used for reverse lookup purpose
	public static KinSdkVersion get(String version) {
		return lookup.get(version);
	}

	KinSdkVersion(String version) {
		this.version = version;
	}

	String getVersion() {
		return version;
	}

}

