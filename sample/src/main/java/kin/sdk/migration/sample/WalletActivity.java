package kin.sdk.migration.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.TextView;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.DeleteAccountException;
import kin.sdk.migration.common.interfaces.IAccountStatus;
import kin.sdk.migration.common.interfaces.IBalance;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IListenerRegistration;
import kin.utils.Request;
import kin.utils.ResultCallback;

/**
 * Responsible for presenting details about the account
 * Public address, account balance, account balance
 * and in future we will add here button to backup the account (show usage of exportKeyStore)
 * In addition there is "Send Transaction" button here that will navigate to TransactionActivity
 */
public class WalletActivity extends BaseActivity {

    public static final String TAG = WalletActivity.class.getSimpleName();

    private TextView balance, status, publicKey;
    private View onboardBtn;
    private View balanceProgress, statusProgress;
    private Request<IBalance> balanceRequest;
    private Request<Integer> statusRequest;
    private IKinAccount account;
    private IListenerRegistration balanceListenerRegistration;

    public static Intent getIntent(Context context) {
        return new Intent(context, WalletActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_activity);
        account = getKinClient().getAccount(0);
        initWidgets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePublicKey();
        updateAccountInfo(false);
    }

    private void initWidgets() {
        balance = findViewById(R.id.balance);
        status = findViewById(R.id.status);
        publicKey = findViewById(R.id.public_key);

        SwitchCompat balanceListenSwitch = findViewById(R.id.auto_refresh_switch);

        balanceProgress = findViewById(R.id.balance_progress);
        statusProgress = findViewById(R.id.status_progress);

        final View transaction = findViewById(R.id.send_transaction_btn);
        final View refresh = findViewById(R.id.refresh_btn);
        onboardBtn = findViewById(R.id.onboard_btn);
        final View deleteAccount = findViewById(R.id.delete_account_btn);
        final View watchPayments = findViewById(R.id.watch_payments_btn);
        final View exportKeyStore = findViewById(R.id.export_wallet);

        if (isMainNet()) {
            transaction.setBackgroundResource(R.drawable.button_main_network_bg);
            refresh.setBackgroundResource(R.drawable.button_main_network_bg);
            onboardBtn.setVisibility(View.GONE);
        } else {
            onboardBtn.setVisibility(View.VISIBLE);
            onboardBtn.setOnClickListener(view -> {
                onboardBtn.setClickable(false);
                onboardAccount();
            });
        }

        deleteAccount.setOnClickListener(view -> showDeleteAlert());

        transaction.setOnClickListener(view -> startActivity(TransactionActivity.getIntent(WalletActivity.this)));
        watchPayments.setOnClickListener(view -> startActivity(PaymentListenerActivity.getIntent(WalletActivity.this)));
        refresh.setOnClickListener(view -> updateAccountInfo(true));
        balanceListenSwitch
            .setOnCheckedChangeListener((buttonView, isChecked) -> handleAutoBalanceSwitchChanges(refresh, isChecked));
        exportKeyStore.setOnClickListener(view -> startActivity(ExportKeystoreActivity.getIntent(this)));
    }

    private void updateAccountInfo(boolean showDialog) {
        updateBalance(showDialog);
        updateStatus(showDialog);
    }

    private void handleAutoBalanceSwitchChanges(View refresh, boolean isChecked) {
        refresh.setEnabled(!isChecked);
        if (isChecked) {
            balanceListenerRegistration = account.addBalanceListener(
                updatedBalance -> runOnUiThread(() -> balance.setText(updatedBalance.value().toPlainString())));
        } else {
            balanceListenerRegistration.remove();
        }
    }

    private void showDeleteAlert() {
        KinAlertDialog.createConfirmationDialog(this, getResources().getString(R.string.delete_wallet_warning),
            getResources().getString(R.string.delete), this::deleteAccount).show();
    }

    private void deleteAccount() {
        try {
            getKinClient().deleteAccount(0);
            onBackPressed();
        } catch (DeleteAccountException e) {
            Utils.logError(e, "deleteAccount");
            KinAlertDialog.createErrorDialog(this, e.getMessage()).show();
        }
    }

    private void onboardAccount() {
        final IKinAccount account = this.account;
        if (account != null) {
            balance.setText(null);
            balanceProgress.setVisibility(View.VISIBLE);
            onboardBtn.setClickable(false);

            OnBoarding onBoarding = new OnBoarding();
            KinSdkVersion kinSdkVersion = ((KinClientSampleApplication) getApplication()).getKinSdkVersion();
            onBoarding.onBoard(kinSdkVersion == KinSdkVersion.NEW_KIN_SDK, account, new OnBoarding.Callbacks() {
                @Override
                public void onSuccess() {
                    updateAccountInfo(true);
                    onboardBtn.setClickable(true);
                }

                @Override
                public void onFailure(Exception e) {
                    Utils.logError(e, "onBoarding");
                    KinAlertDialog.createErrorDialog(WalletActivity.this, e.getMessage()).show();
                    onboardBtn.setClickable(true);
                }
            });
        }
    }

    private void updatePublicKey() {
        String publicKeyStr = "";
        if (account != null) {
            publicKeyStr = account.getPublicAddress();
        }
        publicKey.setText(publicKeyStr);
    }


    private void updateStatus(boolean showDialog) {
        statusProgress.setVisibility(View.VISIBLE);
        if (account != null) {
            statusRequest = account.getStatus();
            if (showDialog) {
                statusRequest.run(new DisplayCallback<Integer>(statusProgress, status) {
                    @Override
                    public void displayResult(Context context, View view, Integer result) {
                        ((TextView) view).setText(accountStatusToString(result));
                    }
                });
            } else {
                statusRequest.run(new ResultCallback<Integer>() {
                    @Override
                    public void onResult(Integer result) {
                        status.setText(accountStatusToString(result));
                        statusProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        Utils.logError(e, "updateStatus");
                        status.setText(R.string.balance_error);
                        statusProgress.setVisibility(View.GONE);
                    }
                });
            }
        } else {
            status.setText(R.string.balance_error);
        }
    }

    private String accountStatusToString(Integer result) {
        String value = "";
        switch (result) {
            case IAccountStatus.CREATED:
                value = "Created";
                break;
            case IAccountStatus.NOT_CREATED:
                value = "Not Created";
                break;
        }
        return value;
    }

    private void updateBalance(boolean showDialog) {
        balanceProgress.setVisibility(View.VISIBLE);
        if (account != null) {
            balanceRequest = account.getBalance();
            if (showDialog) {
                balanceRequest.run(new DisplayCallback<IBalance>(balanceProgress, balance) {
                    @Override
                    public void displayResult(Context context, View view, IBalance result) {
                        if (view != null && result != null) {
                            ((TextView) view).setText(result.value().toPlainString());
                        }
                    }
                });
            } else {
                balanceRequest.run(new ResultCallback<IBalance>() {
                    @Override
                    public void onResult(IBalance result) {
                        balance.setText(result.value().toPlainString());
                        balanceProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        Utils.logError(e, "updateBalance");
                        balance.setText(R.string.balance_error);
                        balanceProgress.setVisibility(View.GONE);
                    }
                });
            }
        } else {
            balance.setText(R.string.balance_error);
        }
    }

    @Override
    Intent getBackIntent() {
        return ChooseNetworkActivity.getIntent(this);
    }

    @Override
    int getActionBarTitleRes() {
        return R.string.balance;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (balanceRequest != null) {
            balanceRequest.cancel(true);
        }
        if (statusRequest != null) {
            statusRequest.cancel(true);
        }
        if (balanceListenerRegistration != null) {
            balanceListenerRegistration.remove();
            balanceListenerRegistration = null;
        }
        balance = null;
    }
}
