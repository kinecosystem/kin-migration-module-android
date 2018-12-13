package kin.sdk.migration;

import android.content.Context;

import kin.core.ServiceProvider;
import kin.sdk.Environment;

public class MigrationManager {

    private final Context context;
    private final String appId;
    private final String networkUrl;
    private final String networkId;
    private final KinVersionProvider kinVersionProvider;
    private IWhitelistService whitelistService;

    public MigrationManager(Context context, String appId, String networkUrl, String networkId,
                            KinVersionProvider kinVersionProvider) { // TODO: 06/12/2018 maybe also add here the eventLogger
        this(context, appId, networkUrl, networkId, kinVersionProvider, null);
    }

    public MigrationManager(Context context, String appId, String networkUrl, String networkId,
                            KinVersionProvider kinVersionProvider, IWhitelistService whitelistService) { // TODO: 06/12/2018 maybe also add here the eventLogger
        this.context = context;
        this.appId = appId;
        this.networkUrl = networkUrl;
        this.networkId = networkId;
        this.kinVersionProvider = kinVersionProvider;
        this.whitelistService = whitelistService;
    }

    public IKinClient initMigration() {
        IKinClient kinClient;
        // TODO: 12/12/2018 this is an api call so we need to make sure the one who implement it knows that
        // TODO: 12/12/2018 Also maybe we should the actul boolean in the constructor or in the init method

        boolean kinSdkVersion = kinVersionProvider.isKinSdkVersion();
        if (kinSdkVersion) {
            Environment environment = new Environment(networkUrl, networkId);
            kinClient = new KinClientSdkImpl(context, environment, appId, whitelistService);
        } else {
            ServiceProvider environment = new ServiceProvider(networkUrl, networkId);
            kinClient = new KinClientCoreImpl(context, environment);
        }
        return kinClient;
    }

}
