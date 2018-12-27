package kin.sdk.migration.sample;

import android.app.Application;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.MigrationManager;

public class KinClientSampleApplication extends Application {

    private static final String SDK_TEST_NETWORK_URL = "http://horizon-testnet.kininfrastructure.com/";
    private static final String SDK_TEST_NETWORK_ID = "Kin Testnet ; December 2018";
    private static final String CORE_TEST_NETWORK_URL = "https://horizon-playground.kininfrastructure.com/";
    private static final String CORE_TEST_NETWORK_ID = "Kin Playground Network ; June 2018";

    private IKinClient kinClient = null;
    private WhitelistService whitelistService;
    private MigrationManager migrationManager;
    private boolean isKinSdkVersion;

    public enum NetWorkType {
        CORE_MAIN,
        SDK_MAIN,
        CORE_TEST,
        SDK_TEST
    }

    public IKinClient createKinClient(NetWorkType type, String appId) {
        final String networkUrl;
        final String networkId;
        if (type == NetWorkType.SDK_TEST) {
            networkUrl = SDK_TEST_NETWORK_URL;
            networkId = SDK_TEST_NETWORK_ID;
            isKinSdkVersion = true;
        } else if (type == NetWorkType.CORE_TEST) {
            networkUrl = CORE_TEST_NETWORK_URL;
            networkId = CORE_TEST_NETWORK_ID;
            isKinSdkVersion = false;
        } else {
            return null;
        }
        whitelistService = new WhitelistService();
        migrationManager = new MigrationManager(this, appId, networkUrl, networkId, null,
                () -> isKinSdkVersion, whitelistService);
        kinClient = migrationManager.initMigration(); // could start the migration itself later.

        return kinClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());
    }

    public IKinClient getKinClient() {
        return kinClient;
    }

    public void setWhitelistServiceListener(WhitelistServiceListener whitelistServiceListener) {
        whitelistService.setWhitelistServiceListener(whitelistServiceListener);
    }

    public boolean isKinSdkVersion() {
        return isKinSdkVersion;
    }

}
