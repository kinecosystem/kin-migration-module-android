package kin.sdk.migration;

import kin.core.ServiceProvider;

public class KinCoreEnvironment implements IEnvironment {

    private final ServiceProvider serviceProvider;

    KinCoreEnvironment(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public String getNetworkUrl() {
        return serviceProvider.getProviderUrl();
    }

    @Override
    public String getNetworkPassphrase() {
        return serviceProvider.getNetworkId();
    }

    @Override
    public boolean isMainNet() {
        return serviceProvider.isMainNet();
    }
}
