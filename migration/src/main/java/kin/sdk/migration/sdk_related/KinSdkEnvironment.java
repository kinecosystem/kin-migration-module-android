package kin.sdk.migration.sdk_related;

import kin.sdk.Environment;
import kin.sdk.migration.interfaces.IEnvironment;

public class KinSdkEnvironment implements IEnvironment {

    private final Environment environment;

    KinSdkEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getNetworkUrl() {
        return environment.getNetworkUrl();
    }

    @Override
    public String getNetworkPassphrase() {
        return environment.getNetworkPassphrase();
    }

    @Override
    public boolean isMainNet() {
        return environment.isMainNet();
    }
}