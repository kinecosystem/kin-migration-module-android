package kin.sdk.migration.interfaces;

public interface IMigrationManagerCallbacks {

    /**
     * Method is invoked before the migration process itself will start.
     * Meaning this is an optional call which will be called only when actual migration takes place.
     */
    void onMigrationStart();

    /**
     * Method is invoked when kinClient is ready to use(whether migration happened or not).
     * This also means that the migration process is now ended.
     * @param kinClient is the kinClient.
     */
    void onReady(IKinClient kinClient);

    /**
     * Method is invoked when an error occurred in the migration process.
     * This also means that the migration process is now ended.
     * @param e is the exception for that error.
     */
    void onError(Exception e);

}
