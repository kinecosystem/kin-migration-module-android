package kin.sdk.migration.internal.core_related;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import kin.core.Balance;
import kin.core.EventListener;
import kin.core.KinAccount;
import kin.core.ListenerRegistration;
import kin.core.PaymentInfo;
import kin.core.TransactionId;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.AccountNotActivatedException;
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

public class KinAccountCoreImpl implements IKinAccount {

    private static final String MEMO_APP_ID_VERSION_PREFIX = "1";
    private static final String MEMO_DELIMITER = "-";

    private final String appId;
    private final KinAccount kinAccount;

    KinAccountCoreImpl(String appId, KinAccount kinAccount) {
        this.appId = appId;
        this.kinAccount = kinAccount;
    }


    @Nullable
    @Override
    public String getPublicAddress() {
        return kinAccount.getPublicAddress();
    }

    @NonNull
    @Override
    public Request<ITransactionId> sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount, IWhitelistService whitelistService) {
        return sendTransaction(publicAddress, amount, whitelistService, null);
    }

    @NonNull
    @Override
    public Request<ITransactionId> sendTransaction(final @NonNull String publicAddress, final @NonNull BigDecimal amount,
                                                   final IWhitelistService whitelistService, final @Nullable String memo) {
        return new Request<>(new Callable<ITransactionId>() {
            @Override
            public ITransactionId call() throws Exception {
                return sendTransactionSync(publicAddress, amount, whitelistService, memo);
            }
        });
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount, IWhitelistService whitelistService) throws OperationFailedException {
        return sendTransactionSync(publicAddress, amount, whitelistService, null);
    }

    @NonNull
    @Override
    public ITransactionId sendTransactionSync(@NonNull String publicAddress, @NonNull BigDecimal amount,
                                              IWhitelistService whitelistService, @Nullable String memo) throws OperationFailedException {
        try {
            TransactionId transactionId = kinAccount.sendTransactionSync(publicAddress, amount, addAppIdToMemo(memo, appId));
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

    private String addAppIdToMemo(@Nullable String memo, @NonNull String appId) {
        if (memo == null) {
            memo = "";
        } else {
            memo = memo.trim(); // remove leading and trailing whitespaces.
        }
        StringBuilder sb = new StringBuilder();
        sb.append(MEMO_APP_ID_VERSION_PREFIX)
                .append(MEMO_DELIMITER)
                .append(appId)
                .append(MEMO_DELIMITER)
                .append(memo);
        memo = sb.toString();
        return memo;
    }

    @NonNull
    public ITransactionId sendBurnTransactionSync(@NonNull String publicAddress) throws OperationFailedException {
        try {
            TransactionId transactionId = kinAccount.sendBurnAccountTransactionSync(publicAddress);
            return new KinCoreTransactionId(transactionId);
        } catch (kin.core.exception.AccountNotFoundException e) {
            throw new AccountNotFoundException(e.getAccountId());
        } catch (kin.core.exception.AccountNotActivatedException e) {
            throw new AccountNotActivatedException(e.getAccountId());
        } catch (kin.core.exception.TransactionFailedException e) {
            throw new TransactionFailedException(e.getTransactionResultCode(), e.getOperationsResultCodes());
        } catch (kin.core.exception.OperationFailedException e) {
            throw new OperationFailedException(e.getMessage(), e.getCause());
        }
    }

    @NonNull
    public boolean isAccountBurned() throws OperationFailedException {
        try {
            return kinAccount.isAccountBurnedSync();
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
    public Request<Void> activate() {
        return new Request<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                activateSync();
                return null;
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

    @Override
    public KinSdkVersion getKinSdkVersion() {
        return KinSdkVersion.OLD_KIN_SDK;
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
        KinAccountCoreImpl account = (KinAccountCoreImpl) obj;
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
