package kin.sdk.migration;

import kin.sdk.Environment;

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
