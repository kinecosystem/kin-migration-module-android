package kin.sdk.migration;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.math.BigDecimal;
import kin.core.Balance;
import kin.core.EventListener;
import kin.core.KinAccount;
import kin.core.ListenerRegistration;
import kin.core.PaymentInfo;
import kin.core.Request;
import kin.core.TransactionId;
import kin.sdk.migration.exception.AccountNotActivatedException;
import kin.sdk.migration.exception.AccountNotFoundException;
import kin.sdk.migration.exception.CryptoException;
import kin.sdk.migration.exception.InsufficientKinException;
import kin.sdk.migration.exception.OperationFailedException;
import kin.sdk.migration.exception.TransactionFailedException;

public class KinAccountCoreImpl implements IKinAccount {

    private final KinAccount kinAccount;

    KinAccountCoreImpl(KinAccount kinAccount) {
        this.kinAccount = kinAccount;
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
        final Request<TransactionId> request = kinAccount.sendTransaction(publicAddress, amount, memo);
        return new KinCoreRequest<>(request, new KinCoreRequest.Transformer<ITransactionId, TransactionId>() {
            @Override
            public ITransactionId transform(TransactionId transactionId) {
                return new KinCoreTransactionId(transactionId);
            }
        });
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount) throws OperationFailedException {
        return sendTransactionSync(publicAddress, amount, null);
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount, @Nullable String memo) throws OperationFailedException {
        try {
            TransactionId transactionId = kinAccount.sendTransactionSync(publicAddress, amount, memo);
            return new KinCoreTransactionId(transactionId);
        } catch (kin.core.exception.AccountNotFoundException e) {
            throw new AccountNotFoundException(e.getAccountId());
        } catch (kin.core.exception.AccountNotActivatedException e) {
            throw new AccountNotActivatedException(e.getAccountId());
        } catch (kin.core.exception.InsufficientKinException e) {
            throw new InsufficientKinException();
        } catch (kin.core.exception.TransactionFailedException e) {
            throw new TransactionFailedException(e.getTransactionResultCode(), e.getOperationsResultCodes());
        } catch (kin.core.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @NonNull
    @Override
    public IRequest<IBalance> getBalance() {
        final Request<Balance> request = kinAccount.getBalance();
        return new KinCoreRequest<>(request, new KinCoreRequest.Transformer<IBalance, Balance>() {
            @Override
            public IBalance transform(Balance balance) {
                return new KinCoreBalance(balance);
            }
        });
    }

    @NonNull
    @Override
    public IBalance getBalanceSync() throws OperationFailedException {
        try {
            Balance balance = kinAccount.getBalanceSync();
            return new KinCoreBalance(balance);
        } catch (kin.core.exception.AccountNotFoundException e) {
            throw new AccountNotFoundException(e.getAccountId());
        } catch (kin.core.exception.AccountNotActivatedException e) {
            throw new AccountNotActivatedException(e.getAccountId());
        } catch (kin.core.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @NonNull
    @Override
    public IRequest<Void> activate() {
        final Request<Void> request = kinAccount.activate();
        return new KinCoreRequest<>(request, new KinCoreRequest.Transformer<Void, Void>() {
            @Override
            public Void transform(Void nothing) {
                return nothing; // TODO: 06/12/2018 check with Rizgan if that's ok
            }
        });
    }

    @Override
    public void activateSync() throws OperationFailedException {
        try {
            kinAccount.activateSync();
        } catch (kin.core.exception.AccountNotFoundException e) {
            throw new AccountNotFoundException(e.getAccountId());
        } catch (kin.core.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public IRequest<Integer> getStatus() {
        final Request<Integer> request = kinAccount.getStatus();
        return new KinCoreRequest<>(request, new KinCoreRequest.Transformer<Integer, Integer>() {
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
        } catch (kin.core.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public String export(@NonNull String passphrase) throws CryptoException {
        try {
            return kinAccount.export(passphrase);
        } catch (kin.core.exception.CryptoException e) {
            throw new CryptoException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public IListenerRegistration addBalanceListener(@NonNull final IEventListener<IBalance> listener) {
        ListenerRegistration listenerRegistration = kinAccount.blockchainEvents().addBalanceListener(new EventListener<Balance>() {
            @Override
            public void onEvent(Balance balance) {
                listener.onEvent(new KinCoreBalance(balance));
            }
        });
        return new KinCoreListenerRegistration(listenerRegistration);
    }

    @Override
    public IListenerRegistration addPaymentListener(@NonNull final IEventListener<IPaymentInfo> listener) {
        ListenerRegistration listenerRegistration = kinAccount.blockchainEvents().addPaymentListener(new EventListener<PaymentInfo>() {
            @Override
            public void onEvent(PaymentInfo paymentInfo) {
                listener.onEvent(new KinCorePaymentInfo(paymentInfo));
            }
        });
        return new KinCoreListenerRegistration(listenerRegistration);
    }

    @Override
    public IListenerRegistration addAccountCreationListener(final IEventListener<Void> listener) {
        ListenerRegistration listenerRegistration = kinAccount.blockchainEvents().addAccountCreationListener(new EventListener<Void>() {
            @Override
            public void onEvent(Void nothing) {
                listener.onEvent(nothing);
            }
        });
        return new KinCoreListenerRegistration(listenerRegistration);
    }

}
