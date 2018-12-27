package kin.sdk.migration.interfaces;

public interface IEnvironment {

    String getNetworkUrl();
    String getNetworkPassphrase();
    boolean isMainNet();

}
