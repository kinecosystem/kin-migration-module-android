package kin.sdk.migration;

public interface IWhitelistServiceCallbacks {
    void onSuccess(String whitelistTransaction);
    void onFailure(Exception e);
}

