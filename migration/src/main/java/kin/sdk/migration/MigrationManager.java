package kin.sdk.migration;

import android.content.Context;

import kin.core.ServiceProvider;
import kin.sdk.Environment;
import kin.sdk.migration.core_related.KinClientCoreImpl;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IKinVersionProvider;
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.sdk_related.KinClientSdkImpl;

public class MigrationManager {

    private final Context context;
    private final String appId;
    private final String networkUrl;
    private final String networkId;
    private final String issuer;
    private final IKinVersionProvider kinVersionProvider;
    private final IWhitelistService whitelistService;
    private String storeKey;

    public MigrationManager(Context context, String appId, String networkUrl, String networkId, String issuer,
                            IKinVersionProvider kinVersionProvider, IWhitelistService whitelistService) { // TODO: 06/12/2018 maybe also add the eventLogger
        this(context, appId, networkUrl, networkId, issuer, kinVersionProvider, whitelistService, null);
    }

    public MigrationManager(Context context, String appId, String networkUrl, String networkId, String issuer,
                            IKinVersionProvider kinVersionProvider, IWhitelistService whitelistService, String storeKey) { // TODO: 06/12/2018 maybe also add the eventLogger
        this.context = context;
        this.appId = appId;
        this.networkUrl = networkUrl;
        this.networkId = networkId;
        this.issuer = issuer;
        this.kinVersionProvider = kinVersionProvider;
        this.whitelistService = whitelistService;
        this.storeKey = storeKey;
    }

    public IKinClient initMigration() {
        IKinClient kinClient;
        // TODO: 12/12/2018 this is an api call so we need to make sure the one who implement it knows that
        // TODO: 12/12/2018 Also maybe we should use actual boolean in the constructor or in the init method instead of using it as an interface?
        // TODO: 18/12/2018 maybe validate that we have all the needed stuff in order to init(depends on which version of course)
        boolean kinSdkVersion = kinVersionProvider.isKinSdkVersion();
        storeKey = (storeKey != null) ? storeKey : "";
        if (kinSdkVersion) {
            Environment environment = new Environment(networkUrl, networkId);
            kinClient = new KinClientSdkImpl(context, environment, appId, whitelistService, storeKey);
        } else {
            ServiceProvider environment = new ServiceProvider(networkUrl, networkId) {
                @Override
                protected String getIssuerAccountId() {
                    return issuer;
                }
            };
            kinClient = new KinClientCoreImpl(context, environment, storeKey);
        }
        return kinClient;
    }

}
