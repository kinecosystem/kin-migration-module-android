package kin.sdk.migration;

public interface WhitelistServiceCallbacks {
    void onSuccess(String whitelistTransaction);
    void onFailure(Exception e);
}

