package kin.sdk.migration.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import android.widget.Toast;
import kin.sdk.migration.common.interfaces.IKinClient;
import kin.sdk.migration.common.interfaces.IMigrationManagerCallbacks;

/**
 * User is given a choice to create or use an account on the MAIN or TEST(test) networks
 */
public class ChooseNetworkActivity extends BaseActivity {

    public static final String TAG = ChooseNetworkActivity.class.getSimpleName();
    private static final String KIN_FOUNDATION_URL = "https://github.com/kinecosystem";

    public static Intent getIntent(Context context) {
        return new Intent(context, ChooseNetworkActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_network_activity);
        initWidgets();
    }

    @Override
    protected boolean hasBack() {
        return false;
    }

    private void initWidgets() {
        TextView urlTextView = findViewById(R.id.kin_foundation_url);
        urlTextView.setPaintFlags(urlTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        urlTextView.setText(Html.fromHtml(KIN_FOUNDATION_URL));
        urlTextView.setOnClickListener(view -> startWebWrapperActivity());
        findViewById(R.id.kin_icon).setOnClickListener(view -> startWebWrapperActivity());
        findViewById(R.id.btn_core_main_net).setOnClickListener(
            view -> KinAlertDialog.createErrorDialog(this, "Currently not available").show());//createKinClient(KinClientSampleApplication.NetWorkType.CORE_MAIN));
        findViewById(R.id.btn_sdk_main_net).setOnClickListener(
            view -> KinAlertDialog.createErrorDialog(this, "Currently not available").show());//createKinClient(KinClientSampleApplication.NetWorkType.SDK_MAIN));

        findViewById(R.id.btn_core_test_net).setOnClickListener(
            view -> createKinClient(KinClientSampleApplication.NetWorkType.CORE_TEST));
        findViewById(R.id.btn_sdk_test_net).setOnClickListener(
            view -> createKinClient(KinClientSampleApplication.NetWorkType.SDK_TEST));
    }

    private void createKinClient(KinClientSampleApplication.NetWorkType netWorkType) {
        KinClientSampleApplication application = (KinClientSampleApplication) getApplication();
        application.createKinClient(netWorkType, "test", new IMigrationManagerCallbacks() {

            @Override
            public void onMigrationStart() {
                // TODO: 02/01/2019 maybe add some progress bar later on
            }

            @Override
            public void onReady(IKinClient kinClient) {
                if (kinClient.hasAccount()) {
                    startActivity(WalletActivity.getIntent(ChooseNetworkActivity.this));
                } else {
                    startActivity(CreateWalletActivity.getIntent(ChooseNetworkActivity.this));
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ChooseNetworkActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startWebWrapperActivity(){
        startActivity(WebWrapperActivity.getIntent(this, KIN_FOUNDATION_URL));
    }

    @Override
    Intent getBackIntent() {
        return null;
    }

    @Override
    int getActionBarTitleRes() {
        return R.string.app_name;
    }
}
