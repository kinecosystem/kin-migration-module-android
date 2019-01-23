package kin.sdk.migration;

import android.support.annotation.NonNull;

public class MigrationNetworkInfo {

    private final String coreNetworkUrl;
    private final String coreNetworkId;
    private final String sdkNetworkUrl;
    private final String sdkNetworkId;
    private final String issuer;
    private final String migrationServiceUrl;

    public MigrationNetworkInfo(@NonNull String coreNetworkUrl, @NonNull String coreNetworkId,
        @NonNull String sdkNetworkUrl,
        @NonNull String sdkNetworkId, @NonNull String issuer, @NonNull String migrationServiceUrl) {
        this.coreNetworkUrl = coreNetworkUrl;
        this.coreNetworkId = coreNetworkId;
        this.sdkNetworkUrl = sdkNetworkUrl;
        this.sdkNetworkId = sdkNetworkId;
        this.issuer = issuer;
        this.migrationServiceUrl = migrationServiceUrl;
    }

    public String getCoreNetworkUrl() {
        return coreNetworkUrl;
    }

    public String getCoreNetworkId() {
        return coreNetworkId;
    }

    public String getSdkNetworkUrl() {
        return sdkNetworkUrl;
    }

    public String getSdkNetworkId() {
        return sdkNetworkId;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getMigrationServiceUrl() {
        return migrationServiceUrl;
    }
}
