package kin.sdk.migration.internal.sdk_related;

import android.content.Context;
import android.support.annotation.NonNull;
import kin.sdk.Environment;
import kin.sdk.KinAccount;
import kin.sdk.KinClient;
import kin.sdk.migration.common.exception.CorruptedDataException;
import kin.sdk.migration.common.exception.CreateAccountException;
import kin.sdk.migration.common.exception.CryptoException;
import kin.sdk.migration.common.exception.DeleteAccountException;
import kin.sdk.migration.common.interfaces.IEnvironment;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IKinClient;

public class KinClientSdkImpl implements IKinClient {

    private final KinSdkEnvironment kinSdkEnvironment;
    private KinClient kinClient;

    public KinClientSdkImpl(Context context, Environment env, String appId) {
        this(context, env, appId, "");
    }

    public KinClientSdkImpl(Context context, Environment env, String appId, String storeKey) {
        kinSdkEnvironment = new KinSdkEnvironment(env);
        kinClient = new KinClient(context, env, appId, storeKey);
    }

    @Override
    public IEnvironment getEnvironment() {
        return kinSdkEnvironment;
    }

    @NonNull
    @Override
    public IKinAccount addAccount() throws CreateAccountException {
        try {
            return new KinAccountSdkImpl(kinClient.addAccount());
        } catch (kin.sdk.exception.CreateAccountException e) {
            throw new CreateAccountException(e.getCause());
        }
    }

    @Override
    public IKinAccount getAccount(int index) {
        KinAccount kinSdkAccount = kinClient.getAccount(index);
        return kinSdkAccount != null ? new KinAccountSdkImpl(kinSdkAccount) : null;
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
        } catch (kin.sdk.exception.DeleteAccountException e) {
           throw new DeleteAccountException(e.getCause());
        }
    }

    @Override
    public void clearAllAccounts() {
        kinClient.clearAllAccounts();
    }

    @NonNull
    @Override
    public IKinAccount importAccount(@NonNull String exportedJson, @NonNull String passphrase)
            throws CryptoException, CreateAccountException, CorruptedDataException {
        try {
            KinAccount kinAccount = kinClient.importAccount(exportedJson, passphrase);
            return new KinAccountSdkImpl(kinAccount);
        } catch (kin.sdk.exception.CryptoException e) {
            throw new CryptoException(e.getMessage(), e.getCause());
        } catch (kin.sdk.exception.CreateAccountException e) {
            throw new CreateAccountException(e.getCause());
        } catch (kin.sdk.exception.CorruptedDataException e) {
            throw new CorruptedDataException(e.getMessage(), e.getCause());
        }
    }
}



