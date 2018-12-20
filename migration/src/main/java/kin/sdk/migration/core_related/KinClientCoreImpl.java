package kin.sdk.migration.core_related;

import android.content.Context;
import android.support.annotation.NonNull;

import kin.core.KinAccount;
import kin.core.KinClient;
import kin.core.ServiceProvider;
import kin.sdk.migration.interfaces.IEnvironment;
import kin.sdk.migration.interfaces.IKinAccount;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IRequest;
import kin.sdk.migration.exception.CorruptedDataException;
import kin.sdk.migration.exception.CreateAccountException;
import kin.sdk.migration.exception.CryptoException;
import kin.sdk.migration.exception.DeleteAccountException;

public class KinClientCoreImpl implements IKinClient {

    private final KinCoreEnvironment kinCoreEnvironment;
    private KinClient kinClient;

    public KinClientCoreImpl(Context context, ServiceProvider serviceProvider) {
        this(context, serviceProvider, "");
    }

    public KinClientCoreImpl(Context context, ServiceProvider serviceProvider, String storeKey) {
        kinCoreEnvironment = new KinCoreEnvironment(serviceProvider);
        kinClient = new KinClient(context, serviceProvider, storeKey);
    }

    @Override
    public IEnvironment getEnvironment() {
        return kinCoreEnvironment;
    }

    @NonNull
    @Override
    public IKinAccount addAccount() throws CreateAccountException {
        try {
            KinAccount kinCoreAccount = kinClient.addAccount();
            return new KinAccountCoreImpl(kinCoreAccount);
        } catch (kin.core.exception.CreateAccountException e) {
            throw new CreateAccountException( e.getCause());
        }
    }

    @Override
    public IKinAccount getAccount(int index) {
        KinAccount kinCoreAccount = kinClient.getAccount(index);
        return new KinAccountCoreImpl(kinCoreAccount);
    }

    @Override
    public boolean hasAccount() {
        return kinClient.hasAccount();
    }

    @Override
    public int getAccountCount() {
        return kinClient.getAccountCount();
    }

    @Override
    public void deleteAccount(int index) throws DeleteAccountException {
        try {
            kinClient.deleteAccount(index);
        } catch (kin.core.exception.DeleteAccountException e) {
            throw new DeleteAccountException(e.getCause());
        }
    }

    @Override
    public void clearAllAccounts() {
        kinClient.clearAllAccounts();
    }

    @Override
    public IRequest<Long> getMinimumFee() {
        return null;
    }

    @Override
    public long getMinimumFeeSync() {
        return 0;
    }

    @NonNull
    @Override
    public IKinAccount importAccount(@NonNull String exportedJson, @NonNull String passphrase)
        throws CryptoException, CreateAccountException, CorruptedDataException {
        try {
            KinAccount kinAccount = kinClient.importAccount(exportedJson, passphrase);
            // TODO: 06/12/2018 handle import
            return new KinAccountCoreImpl(kinAccount);
        } catch (kin.core.exception.CryptoException e) {
            throw new CryptoException(e.getMessage(), e.getCause());
        } catch (kin.core.exception.CreateAccountException e) {
            throw new CreateAccountException(e.getCause());
        } catch (kin.core.exception.CorruptedDataException e) {
            throw new CorruptedDataException(e.getMessage(), e.getCause());
        }
    }

}
