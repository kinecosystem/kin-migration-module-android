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

import kin.sdk.migration.IKinAccount;
import kin.sdk.migration.IRequest;
import kin.sdk.migration.ITransaction;
import kin.sdk.migration.ITransactionId;
import kin.sdk.migration.exception.AccountDeletedException;
import kin.sdk.migration.exception.OperationFailedException;

/**
 * Displays form to enter public address and amount and a button to send a transaction
 */
public class TransactionActivity extends BaseActivity {

    public static final String TAG = TransactionActivity.class.getSimpleName();

    public static Intent getIntent(Context context) {
        return new Intent(context, TransactionActivity.class);
    }

    private View sendTransaction/*, retrieveMinimumFee*/, progressBar;

    private EditText toAddressInput, amountInput/*, feeInput*/, memoInput;
//    private IRequest<Long> gertMinimumFeeRequest;
    private IRequest<ITransactionId> sendTransactionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_activity);
        initWidgets();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (gertMinimumFeeRequest != null) {
//            gertMinimumFeeRequest.cancel(false);
//        }
        if (sendTransactionRequest != null) {
            sendTransactionRequest.cancel(false);
        }
        progressBar = null;
    }

    private void initWidgets() {
        sendTransaction = findViewById(R.id.send_transaction_btn);
//        retrieveMinimumFee = findViewById(R.id.retrieve_minimum_fee_btn);
        progressBar = findViewById(R.id.transaction_progress);
        toAddressInput = findViewById(R.id.to_address_input);
        amountInput = findViewById(R.id.amount_input);
//        feeInput = findViewById(R.id.fee_input);
        memoInput = findViewById(R.id.memo_input);

        if (getKinClient().getEnvironment().isMainNet()) {
            sendTransaction.setBackgroundResource(R.drawable.button_main_network_bg);
        }

        initToAddressInput();
        initAmountInput();
//        initFeeInput();

//        initMinimumFee();
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

//    private void initFeeInput() {
//        feeInput.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!TextUtils.isEmpty(charSequence) && !TextUtils.isEmpty(toAddressInput.getText()) && !TextUtils.isEmpty(amountInput.getText())) {
//                    if (!sendTransaction.isEnabled()) {
//                        sendTransaction.setEnabled(true);
//                    }
//                } else if (sendTransaction.isEnabled()) {
//                    sendTransaction.setEnabled(false);
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//        });
//
//        feeInput.setOnFocusChangeListener((view, hasFocus) -> {
//            if (!hasFocus && !feeInput.hasFocus()) {
//                hideKeyboard(view);
//            }
//        });
//    }

//    private void initMinimumFee() {
//        retrieveMinimumFee.setOnClickListener(v -> {
//            retrieveMinimumFee.setEnabled(false);
//            gertMinimumFeeRequest = getKinClient().getMinimumFee();
//            gertMinimumFeeRequest.run(new IResultCallback<Long>() {
//                @Override
//                public void onResult(Long minimumFee) {
//                    Log.d(TAG, "handleSendTransaction: minimum fee = " + minimumFee);
//                    feeInput.setText(minimumFee != null ? String.valueOf(minimumFee) : "");
//                    retrieveMinimumFee.setEnabled(true);
//                }
//
//                @Override
//                public void onError(Exception e) {
//                    retrieveMinimumFee.setEnabled(true);
//                    Utils.logError(e, "handleSendTransaction");
//                    KinAlertDialog.createErrorDialog(TransactionActivity.this, e.getMessage()).show();
//                }
//            });
//        });
//    }

    private void initSendTransaction() {
        sendTransaction.setOnClickListener(view -> {
            BigDecimal amount = new BigDecimal(amountInput.getText().toString());
            try {
                handleSendTransaction(toAddressInput.getText().toString(), amount/*, Integer.valueOf(feeInput.getText().toString())*/, memoInput.getText().toString());
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

    private void handleSendTransaction(String toAddress, BigDecimal amount/*, int fee*/, String memo) throws OperationFailedException {
        progressBar.setVisibility(View.VISIBLE);
        IKinAccount account = getKinClient().getAccount(0);
        if (account != null) {
            sendTransaction(toAddress, amount/*, fee*/, memo, account);
        } else {
            progressBar.setVisibility(View.GONE);
            throw new AccountDeletedException();
        }
    }

    private void sendTransaction(String toAddress, BigDecimal amount/*, int fee*/, String memo, IKinAccount account) {
        ((KinClientSampleApplication) getApplication()).setWhitelistServiceListener(new WhitelistServiceListenerImpl());
        if (memo == null) {
            sendTransactionRequest = account.sendTransaction(toAddress, amount/*, fee*/);
        } else {
            sendTransactionRequest = account.sendTransaction(toAddress, amount/*, fee*/, memo);
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
            Log.d(TAG, "WhitelistServiceListener: onSuccess");
        }

        @Override
        public void onFailure(Exception e) {
            Utils.logError(e, "whitelistTransaction");
            KinAlertDialog.createErrorDialog(TransactionActivity.this, e.getMessage()).show();
        }
    }

}
