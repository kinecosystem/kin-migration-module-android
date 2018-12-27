package kin.sdk.migration.interfaces;

public interface MigrationManagerListener {

    void onComplete(IKinClient kinClient);
    void onError(Exception e);

}
