package kin.sdk.migration.sample;

import android.app.Application;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;

import kin.sdk.migration.MigrationNetworkInfo;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.exception.MigrationFailedException;
import kin.sdk.migration.exception.MigrationInProcessException;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.MigrationManager;
import kin.sdk.migration.interfaces.IKinVersionProvider;
import kin.sdk.migration.interfaces.MigrationManagerListener;

public class KinClientSampleApplication extends Application {

    private static final String SDK_TEST_NETWORK_URL = "http://horizon-testnet.kininfrastructure.com/";
    private static final String SDK_TEST_NETWORK_ID = "Kin Testnet ; December 2018";
    private static final String CORE_TEST_NETWORK_URL = "https://horizon-playground.kininfrastructure.com/";
    private static final String CORE_TEST_NETWORK_ID = "Kin Playground Network ; June 2018";
    private static final String CORE_ISSUER = "GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7";

    private IKinClient kinClient;
    private boolean isKinSdkVersion;

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

    public void createKinClient(NetWorkType type, String appId, MigrationManagerListener migrationManagerListener) {
        MigrationNetworkInfo migrationNetworkInfo = new MigrationNetworkInfo(CORE_TEST_NETWORK_URL, CORE_TEST_NETWORK_ID,
                                                        SDK_TEST_NETWORK_URL, SDK_TEST_NETWORK_ID, CORE_ISSUER);
        if (type == NetWorkType.SDK_TEST) {
            isKinSdkVersion = true;
        } else if (type == NetWorkType.CORE_TEST) {
            isKinSdkVersion = false;
        } else {
             // TODO: 24/12/2018 handle it after we have production urls
        }
        MigrationManager migrationManager = new MigrationManager(this, appId, migrationNetworkInfo,
                x -> isKinSdkVersion);
        try {
            migrationManager.startMigration(new MigrationManagerListener() {

                @Override
                public void onMigrationStart() {
                    migrationManagerListener.onMigrationStart();
                }

                @Override
                public void onReady(IKinClient kinClient) {
                    KinClientSampleApplication.this.kinClient = kinClient;
                    migrationManagerListener.onReady(kinClient);
                }

                @Override
                public void onError(Exception e) {
                    migrationManagerListener.onError(e);
                }
            });
        } catch (MigrationInProcessException e) {
           migrationManagerListener.onError(e);
        }
    }

    public IKinClient getKinClient() {
        return kinClient;
    }

    public boolean isKinSdkVersion() {
        return isKinSdkVersion;
    }

}
