package kin.sdk.migration.common.interfaces;

public interface IWhitelistServiceCallbacks {
    void onSuccess(String whitelistTransaction);
    void onFailure(Exception e);
}

