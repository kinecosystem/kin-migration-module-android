package kin.sdk.migration.interfaces;

public interface IWhitelistServiceCallbacks {
    void onSuccess(String whitelistTransaction);
    void onFailure(Exception e);
}

