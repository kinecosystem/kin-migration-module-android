package kin.sdk.migration.sdk_related;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;
import java.math.BigDecimal;

import kin.sdk.Balance;
import kin.sdk.EventListener;
import kin.sdk.KinAccount;
import kin.sdk.ListenerRegistration;
import kin.sdk.PaymentInfo;
import kin.sdk.Request;
import kin.sdk.ResultCallback;
import kin.sdk.Transaction;
import kin.sdk.TransactionId;
import kin.sdk.migration.exception.InsufficientKinException;
import kin.sdk.migration.exception.TransactionFailedException;
import kin.sdk.migration.interfaces.IBalance;
import kin.sdk.migration.interfaces.IEventListener;
import kin.sdk.migration.interfaces.IKinAccount;
import kin.sdk.migration.interfaces.IListenerRegistration;
import kin.sdk.migration.interfaces.IPaymentInfo;
import kin.sdk.migration.interfaces.IRequest;
import kin.sdk.migration.interfaces.IResultCallback;
import kin.sdk.migration.interfaces.ITransactionId;
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.interfaces.IWhitelistServiceCallbacks;
import kin.sdk.migration.exception.AccountNotFoundException;
import kin.sdk.migration.exception.CryptoException;
import kin.sdk.migration.exception.OperationFailedException;

public class KinAccountSdkImpl implements IKinAccount {

    private final KinAccount kinAccount;
    private final IWhitelistService whitelistService;

    KinAccountSdkImpl(KinAccount kinAccount, IWhitelistService whitelistService) {
        this.kinAccount = kinAccount;
        this.whitelistService = whitelistService;
    }

    @Nullable
    @Override
    public String getPublicAddress() {
        return kinAccount.getPublicAddress();
    }

    @NonNull
    @Override
    public IRequest<ITransactionId> sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount) {
        return sendTransaction(publicAddress, amount, null);
    }

    @NonNull
    @Override
    public IRequest<ITransactionId> sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount, @Nullable String memo) {
        final Request<Transaction> buildTransactionRequest = kinAccount.buildTransaction(publicAddress, amount, 0, memo);// 0 because currently in the migration module we are supporting only whitelist
        return new IRequest<ITransactionId> () {

            @Override
            public void run(final IResultCallback<ITransactionId> callback) {
                buildTransactionRequest.run(new BuildTransactionCallback(callback));
            }

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                buildTransactionRequest.cancel(mayInterruptIfRunning);
            }
        };
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount) throws OperationFailedException, IOException, JSONException {
        return sendTransactionSync(publicAddress, amount, null);
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount, @Nullable String memo) throws OperationFailedException, IOException, JSONException {
        try {
            Transaction transaction = kinAccount.buildTransactionSync(publicAddress, amount, 0, memo);
            if (whitelistService != null) {
                String whitelistTransaction = whitelistService.whitelistTransactionSync(new KinSdkTransaction(transaction).getWhitelistableTransaction());
                TransactionId transactionId = kinAccount.sendWhitelistTransactionSync(whitelistTransaction);
                return new KinSdkTransactionId(transactionId);
            } else {
                throw new IllegalArgumentException("whitelist service listener is null");
            }
        } catch (kin.sdk.exception.AccountNotFoundException e) {
                throw new AccountNotFoundException(e.getAccountId());
        } catch (kin.sdk.exception.InsufficientKinException e) {
            throw new InsufficientKinException();
        } catch (kin.sdk.exception.TransactionFailedException e) {
            throw new TransactionFailedException(e.getTransactionResultCode(), e.getOperationsResultCodes());
        } catch (kin.sdk.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @NonNull
    @Override
    public IRequest<IBalance> getBalance() {
        final Request<Balance> request = kinAccount.getBalance();
        return new KinSdkRequest<>(request, new KinSdkRequest.Transformer<IBalance, Balance>() {
            @Override
            public IBalance transform(Balance balance) {
                return new KinSdkBalance(balance);
            }
        });
    }

    @NonNull
    @Override
    public IBalance getBalanceSync() throws OperationFailedException {
        try {
            Balance balance = kinAccount.getBalanceSync();
            return new KinSdkBalance(balance);
        } catch (kin.sdk.exception.AccountNotFoundException e) {
            throw new AccountNotFoundException(e.getAccountId());
        } catch (kin.sdk.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @NonNull
    @Override
    public IRequest<Void> activate() {
        return new KinSdkRequest<>(null, null);
    }

    @Override
    public void activateSync() throws OperationFailedException {

    }

    @Override
    public IRequest<Integer> getStatus() {
        final Request<Integer> request = kinAccount.getStatus();
        return new KinSdkRequest<>(request, new KinSdkRequest.Transformer<Integer, Integer>() {
            @Override
            public Integer transform(Integer status) {
                return status;
            }
        });
    }

    @Override
    public int getStatusSync() throws OperationFailedException {
        try {
            return kinAccount.getStatusSync();
        } catch (kin.sdk.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public String export(@NonNull String passphrase) throws CryptoException {
        try {
            return kinAccount.export(passphrase);
        } catch (kin.sdk.exception.CryptoException e) {
            throw new CryptoException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public IListenerRegistration addBalanceListener(@NonNull final IEventListener<IBalance> listener) {
        ListenerRegistration listenerRegistration = kinAccount.addBalanceListener(new EventListener<Balance>() {
            @Override
            public void onEvent(Balance balance) {
                listener.onEvent(new KinSdkBalance(balance));
            }
        });
        return new KinSdkListenerRegistration(listenerRegistration);
    }

    @Override
    public IListenerRegistration addPaymentListener(@NonNull final IEventListener<IPaymentInfo> listener) {
        ListenerRegistration listenerRegistration = kinAccount.addPaymentListener(new EventListener<PaymentInfo>() {
            @Override
            public void onEvent(PaymentInfo paymentInfo) {
                listener.onEvent(new KinSdkPaymentInfo(paymentInfo));
            }
        });
        return new KinSdkListenerRegistration(listenerRegistration);
    }

    @Override
    public IListenerRegistration addAccountCreationListener(final IEventListener<Void> listener) {
        ListenerRegistration listenerRegistration = kinAccount.addAccountCreationListener(new EventListener<Void>() {
            @Override
            public void onEvent(Void nothing) {
                listener.onEvent(nothing);
            }
        });
        return new KinSdkListenerRegistration(listenerRegistration);
    }

    private class BuildTransactionCallback implements ResultCallback<Transaction> {

        private final IResultCallback<ITransactionId> callback;

        BuildTransactionCallback(IResultCallback<ITransactionId> callback) {
            this.callback = callback;
        }

        @Override
        public void onResult(Transaction transaction) {
            if (whitelistService != null) {
                try {
                    whitelistService.whitelistTransaction(new KinSdkTransaction(transaction).getWhitelistableTransaction(), new WhitelistServiceListener(callback));
                } catch (JSONException e) {
                    onError(e);
                }
            } else {
                onError(new IllegalArgumentException("whitelist service listener is null"));
            }
        }

        // TODO: 06/12/2018  check for 4045 error from whitelist service
        @Override
        public void onError(Exception e) {
            callback.onError(e);
        }
    }

    private class WhitelistServiceListener implements IWhitelistServiceCallbacks {

        private final IResultCallback<ITransactionId> callback;

        WhitelistServiceListener(IResultCallback<ITransactionId> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(String whitelistTransaction) {
            if (kinAccount != null) {
                Request<TransactionId> request = kinAccount.sendWhitelistTransaction(whitelistTransaction);
                new KinSdkRequest<>(request, new KinSdkRequest.Transformer<ITransactionId, TransactionId>() {
                    @Override
                    public ITransactionId transform(TransactionId transactionId) {
                        return new KinSdkTransactionId(transactionId);
                    }
                }).run(callback);
            } else {
                callback.onError(new AccountNotFoundException("no account found"));
            }

        }

        @Override
        public void onFailure(Exception e) {
            callback.onError(e);
        }
    }

}