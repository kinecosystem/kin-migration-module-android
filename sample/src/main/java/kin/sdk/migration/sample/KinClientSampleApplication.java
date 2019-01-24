package kin.sdk.migration.sample;

import android.app.Application;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;
import kin.sdk.migration.KinSdkVersion;
import kin.sdk.migration.MigrationManager;
import kin.sdk.migration.MigrationNetworkInfo;
import kin.sdk.migration.exception.MigrationInProcessException;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IMigrationManagerCallbacks;

public class KinClientSampleApplication extends Application {

    private static final String SDK_TEST_NETWORK_URL = "http://horizon-testnet.kininfrastructure.com/";
    private static final String SDK_TEST_NETWORK_ID = "Kin Testnet ; December 2018";
    private static final String CORE_TEST_NETWORK_URL = "https://horizon-playground.kininfrastructure.com/";
    private static final String CORE_TEST_NETWORK_ID = "Kin Playground Network ; June 2018";
    private static final String CORE_ISSUER = "GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7";
    private static final String MIGRATE_ACCOUNT_SERVICE_TEST_URL = "https://migration-devplatform-playground.developers.kinecosystem.com/migrate?address=";
    private static final String MIGRATE_ACCOUNT_SERVICE_PRODUCTION_URL = "https://migration-devplatform-production.developers.kinecosystem.com/migrate?address=";


    private IKinClient kinClient;
    private KinSdkVersion sdkVersion;

    public enum NetWorkType {
        CORE_MAIN,
        SDK_MAIN,
        CORE_TEST,
        SDK_TEST
    }

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }

    public void createKinClient(NetWorkType type, String appId, IMigrationManagerCallbacks migrationManagerCallbacks) {
        MigrationNetworkInfo migrationNetworkInfo = new MigrationNetworkInfo(CORE_TEST_NETWORK_URL, CORE_TEST_NETWORK_ID,
            SDK_TEST_NETWORK_URL, SDK_TEST_NETWORK_ID, CORE_ISSUER, MIGRATE_ACCOUNT_SERVICE_TEST_URL);
        if (type == NetWorkType.SDK_TEST) {
            sdkVersion = KinSdkVersion.NEW_KIN_SDK;
        } else if (type == NetWorkType.CORE_TEST) {
            sdkVersion = KinSdkVersion.OLD_KIN_SDK;
        } else {
            // TODO: 24/12/2018 handle it after later
        }
        MigrationManager migrationManager = new MigrationManager(this, appId, migrationNetworkInfo,
                () -> sdkVersion, new SampleMigrationEventsListener());
        migrationManager.enableLogs(true);
        try {
            migrationManager.start(new IMigrationManagerCallbacks() {

                @Override
                public void onMigrationStart() {
                    migrationManagerCallbacks.onMigrationStart();
                }

                @Override
                public void onReady(IKinClient kinClient) {
                    KinClientSampleApplication.this.kinClient = kinClient;
                    migrationManagerCallbacks.onReady(kinClient);
                }

                @Override
                public void onError(Exception e) {
                    migrationManagerCallbacks.onError(e);
                }
            });
        } catch (MigrationInProcessException e) {
            migrationManagerCallbacks.onError(e);
        }
    }

    public IKinClient getKinClient() {
        return kinClient;
    }

    public KinSdkVersion getKinSdkVersion() {
        return sdkVersion;
    }

}
