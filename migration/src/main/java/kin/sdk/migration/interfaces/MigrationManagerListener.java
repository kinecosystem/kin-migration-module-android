package kin.sdk.migration.interfaces;

public interface MigrationManagerListener {

    /**
     * Method is invoked before the migration process itself will start.
     */
    void onMigrationStart();

    /**
     * Method is invoked when kinClient is ready to use.
     * @param kinClient is the kinClient.
     */
    void onReady(IKinClient kinClient);

    /**
     * Method is invoked when an error occurred in the migration process
     * @param e is the exception for that error.
     */
    void onError(Exception e);

}
