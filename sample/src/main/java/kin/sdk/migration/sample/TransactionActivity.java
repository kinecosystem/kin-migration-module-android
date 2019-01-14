package kin.sdk.migration.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.math.BigDecimal;

import kin.sdk.migration.interfaces.IKinAccount;
import kin.sdk.migration.interfaces.ITransactionId;
import kin.sdk.migration.exception.AccountDeletedException;
import kin.sdk.migration.exception.OperationFailedException;
import kin.utils.Request;

/**
 * Displays form to enter public address and amount and a button to send a transaction
 */
public class TransactionActivity extends BaseActivity {

    public static final String TAG = TransactionActivity.class.getSimpleName();

    public static Intent getIntent(Context context) {
        return new Intent(context, TransactionActivity.class);
    }

    private View sendTransaction, progressBar;

    private EditText toAddressInput, amountInput, memoInput;
    private Request<ITransactionId> sendTransactionRequest;
    private WhitelistService whitelistService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_activity);
        initWidgets();
        whitelistService = new WhitelistService(new WhitelistServiceListenerImpl());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sendTransactionRequest != null) {
            sendTransactionRequest.cancel(false);
        }
        progressBar = null;
    }

    private void initWidgets() {
        sendTransaction = findViewById(R.id.send_transaction_btn);
        progressBar = findViewById(R.id.transaction_progress);
        toAddressInput = findViewById(R.id.to_address_input);
        amountInput = findViewById(R.id.amount_input);
        memoInput = findViewById(R.id.memo_input);

        if (getKinClient().getEnvironment().isMainNet()) {
            sendTransaction.setBackgroundResource(R.drawable.button_main_network_bg);
        }

        initToAddressInput();
        initAmountInput();
        initSendTransaction();
    }

    private void initToAddressInput() {
        toAddressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!TextUtils.isEmpty(charSequence) && !TextUtils.isEmpty(amountInput.getText()) /*&& !TextUtils.isEmpty(feeInput.getText())*/) {
                    if (!sendTransaction.isEnabled()) {
                        sendTransaction.setEnabled(true);
                    }
                } else if (sendTransaction.isEnabled()) {
                    sendTransaction.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        toAddressInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus && !toAddressInput.hasFocus()) {
                hideKeyboard(view);
            }
        });
    }

    private void initAmountInput() {
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!TextUtils.isEmpty(charSequence) && !TextUtils.isEmpty(toAddressInput.getText())/* && !TextUtils.isEmpty(feeInput.getText())*/) {
                    if (!sendTransaction.isEnabled()) {
                        sendTransaction.setEnabled(true);
                    }
                } else if (sendTransaction.isEnabled()) {
                    sendTransaction.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        amountInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus && !amountInput.hasFocus()) {
                hideKeyboard(view);
            }
        });
    }

    private void initSendTransaction() {
        sendTransaction.setOnClickListener(view -> {
            BigDecimal amount = new BigDecimal(amountInput.getText().toString());
            try {
                handleSendTransaction(toAddressInput.getText().toString(), amount, memoInput.getText().toString());
            } catch (OperationFailedException e) {
                Utils.logError(e, "handleSendTransaction");
                KinAlertDialog.createErrorDialog(TransactionActivity.this, e.getMessage()).show();
            }
        });
    }

    @Override
    Intent getBackIntent() {
        return WalletActivity.getIntent(this);
    }

    @Override
    int getActionBarTitleRes() {
        return R.string.transaction;
    }

    private void handleSendTransaction(String toAddress, BigDecimal amount, String memo) throws OperationFailedException {
        progressBar.setVisibility(View.VISIBLE);
        IKinAccount account = getKinClient().getAccount(0);
        if (account != null) {
            sendTransaction(toAddress, amount, memo, account);
        } else {
            progressBar.setVisibility(View.GONE);
            throw new AccountDeletedException();
        }
    }

    private void sendTransaction(String toAddress, BigDecimal amount, String memo, IKinAccount account) {
        if (memo == null) {
            sendTransactionRequest = account.sendTransaction(toAddress, amount, whitelistService);
        } else {
            sendTransactionRequest = account.sendTransaction(toAddress, amount, whitelistService, memo);
        }
        sendTransactionRequest.run(new SendTransactionCallback());
    }

    private class SendTransactionCallback extends DisplayCallback<ITransactionId> {

        SendTransactionCallback() {
            super(progressBar);
        }

        @Override
        public void displayResult(Context context, View view, ITransactionId transactionId) {
            KinAlertDialog.createErrorDialog(context, "Transaction id " + transactionId.id()).show();
        }
    }

    class WhitelistServiceListenerImpl implements WhitelistServiceListener {

        @Override
        public void onSuccess(String whitelistTransaction) {
            Log.d(TAG, "WhitelistServiceListenerImpl: onSuccess");
        }

        @Override
        public void onFailure(Exception e) {
            Utils.logError(e, "WhitelistServiceListenerImpl - onFailure");
            KinAlertDialog.createErrorDialog(TransactionActivity.this, e.getMessage()).show();
        }
    }

}
