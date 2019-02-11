package kin.sdk.migration.bi;

import kin.sdk.migration.common.KinSdkVersion;

public interface IMigrationEventsListener {

    enum RequestAccountMigrationSuccessReason {

        MIGRATED("migrated"),
        ALREADY_MIGRATED("already_migrated"),
        ACCOUNT_NOT_FOUND("account_not_found");
        private final String value;

        private RequestAccountMigrationSuccessReason(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

    }

    enum SelectedSdkReason {

        MIGRATED("migrated"),
        ALREADY_MIGRATED("already_migrated"),
        NO_ACCOUNT_TO_MIGRATE("no_account_to_migrate"),
        API_CHECK("api_check");
        private final String value;

        private SelectedSdkReason(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

    }


    enum BurnReason {

        BURNED("burned"),
        ALREADY_BURNED("already_burned"),
        NO_ACCOUNT("no_account"),
        NO_TRUSTLINE("no_trustline");
        private final String value;

        private BurnReason(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }
    }


    enum CheckBurnReason {

        NOT_BURNED("not_burned"),
        ALREADY_BURNED("already_burned"),
        NO_ACCOUNT("no_account"),
        NO_TRUSTLINE("no_trustline");
        private final String value;

        private CheckBurnReason(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

    }


    void onMethodStarted();

    void onVersionCheckStarted();

    void onVersionCheckSucceeded(KinSdkVersion sdkVersion);

    void onVersionCheckFailed(Exception exception);

    void onCallbackStart();

    void onCheckBurnStarted(String publicAddress);

    void onCheckBurnSucceeded(String publicAddress, CheckBurnReason reason);

    void onCheckBurnFailed(String publicAddress, Exception exception);

    void onBurnStarted(String publicAddress);

    void onBurnSucceeded(String publicAddress, BurnReason reason);

    void onBurnFailed(String publicAddress, Exception exception);

    void onRequestAccountMigrationStarted(String publicAddress);

    void onRequestAccountMigrationSucceeded(String publicAddress, RequestAccountMigrationSuccessReason reason);

    void onRequestAccountMigrationFailed(String publicAddress, Exception exception);

    void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason);

    void onCallbackFailed(Exception exception);

}
