package kin.sdk.migration.core_related;

import kin.core.ServiceProvider;
import kin.sdk.migration.interfaces.IEnvironment;

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
