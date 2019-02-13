package kin.sdk.migration.internal.sdk_related;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

import kin.sdk.Balance;
import kin.sdk.EventListener;
import kin.sdk.KinAccount;
import kin.sdk.ListenerRegistration;
import kin.sdk.PaymentInfo;
import kin.sdk.Transaction;
import kin.sdk.TransactionId;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.WhitelistResult;
import kin.sdk.migration.common.exception.AccountNotFoundException;
import kin.sdk.migration.common.exception.CryptoException;
import kin.sdk.migration.common.exception.InsufficientKinException;
import kin.sdk.migration.common.exception.OperationFailedException;
import kin.sdk.migration.common.exception.TransactionFailedException;
import kin.sdk.migration.common.interfaces.IBalance;
import kin.sdk.migration.common.interfaces.IEventListener;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IListenerRegistration;
import kin.sdk.migration.common.interfaces.IPaymentInfo;
import kin.sdk.migration.common.interfaces.ITransactionId;
import kin.sdk.migration.common.interfaces.IWhitelistService;
import kin.utils.Request;

public class KinAccountSdkImpl implements IKinAccount {

    private final KinAccount kinAccount;

    KinAccountSdkImpl(KinAccount kinAccount) {
        this.kinAccount = kinAccount;
    }

    @Nullable
    @Override
    public String getPublicAddress() {
        return kinAccount.getPublicAddress();
    }

    @NonNull
    @Override
    public Request<ITransactionId> sendTransaction(@NonNull String publicAddress,
                                                   @NonNull BigDecimal amount, @NonNull IWhitelistService whitelistService) {
        return sendTransaction(publicAddress, amount, whitelistService, null);
    }

    @NonNull
    @Override
    public Request<ITransactionId> sendTransaction(final @NonNull String publicAddress, final @NonNull BigDecimal amount,
                                                   final @NonNull IWhitelistService whitelistService, final @Nullable String memo) {
        return new Request<>(new Callable<ITransactionId>() {
            @Override
            public ITransactionId call() throws Exception {
                return sendTransactionSync(publicAddress, amount, whitelistService, memo);
            }
        });
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount,
                                              @NonNull IWhitelistService whitelistService) throws OperationFailedException {
        return sendTransactionSync(publicAddress, amount, whitelistService, null);
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount,
                                              @NonNull IWhitelistService whitelistService, @Nullable String memo) throws OperationFailedException {
        try {
            Transaction transaction = kinAccount.buildTransactionSync(publicAddress, amount, 0, memo);
            if (whitelistService != null) {
                TransactionId transactionId = null;
                WhitelistResult whitelistTransactionResult = whitelistService.onWhitelistableTransactionReady(new KinSdkTransaction(transaction).getWhitelistableTransaction());
                if (whitelistTransactionResult.shouldSendTransaction()) {
                    transactionId = kinAccount.sendWhitelistTransactionSync(whitelistTransactionResult.getWhitelistedTransaction());
                }
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
    public Request<IBalance> getBalance() {
        return new Request<>(new Callable<IBalance>() {

            @Override
            public IBalance call() throws Exception {
                return getBalanceSync();
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
    public Request<Void> activate() {
        return new Request<>(null);
    }

    @Override
    public void activateSync() throws OperationFailedException {

    }

    @Override
    public Request<Integer> getStatus() {
        return new Request<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getStatusSync();
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

    @Override
    public KinSdkVersion getKinSdkVersion() {
        return KinSdkVersion.NEW_KIN_SDK;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        KinAccount account = (KinAccount) obj;
        if (getPublicAddress() == null || account.getPublicAddress() == null) {
            return false;
        }
        return getPublicAddress().equals(account.getPublicAddress());
    }

    @Override
    public int hashCode() {
        return kinAccount.getPublicAddress() != null ?
                kinAccount.getPublicAddress().hashCode() : super.hashCode();
    }
}