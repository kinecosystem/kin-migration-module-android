package kin.sdk.migration;

public interface IEnvironment {

    String getNetworkUrl();
    String getNetworkPassphrase();
    boolean isMainNet();

}
