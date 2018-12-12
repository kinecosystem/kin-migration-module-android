package kin.sdk.migration;

import android.content.Context;

public class MigrationManager {

    private final Context context;
    private final String appId;
    private final String networkUrl;
    private final String networkId;
    private final KinVersionProvider kinVersionProvider;
    private final IWhitelistService whitelistService;

    public MigrationManager(Context context, String appId, String networkUrl, String networkId,
                            KinVersionProvider kinVersionProvider, IWhitelistService whitelistService) { // TODO: 06/12/2018 maybe also add here the eventLogger
        this.context = context;
        this.appId = appId;
        this.networkUrl = networkUrl;
        this.networkId = networkId;
        this.kinVersionProvider = kinVersionProvider;
        this.whitelistService = whitelistService;
    }

}
