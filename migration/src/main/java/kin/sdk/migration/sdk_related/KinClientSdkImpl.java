package kin.sdk.migration.sdk_related;

import android.content.Context;
import android.support.annotation.NonNull;
import kin.sdk.Environment;
import kin.sdk.KinAccount;
import kin.sdk.KinClient;
import kin.sdk.migration.interfaces.IEnvironment;
import kin.sdk.migration.interfaces.IKinAccount;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.exception.CorruptedDataException;
import kin.sdk.migration.exception.CreateAccountException;
import kin.sdk.migration.exception.CryptoException;
import kin.sdk.migration.exception.DeleteAccountException;

public class KinClientSdkImpl implements IKinClient {

    private final IWhitelistService whitelistService;
    private final KinSdkEnvironment kinSdkEnvironment;
    private KinClient kinClient;

    public KinClientSdkImpl(Context context, Environment env, String appId, IWhitelistService whitelistService) {
        this(context, env, appId, whitelistService, "");
    }

    public KinClientSdkImpl(Context context, Environment env, String appId, IWhitelistService whitelistService, String storeKey) {
        this.whitelistService = whitelistService;
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
            return new KinAccountSdkImpl(kinClient.addAccount(), whitelistService);
        } catch (kin.sdk.exception.CreateAccountException e) {
            throw new CreateAccountException(e.getCause());
        }
    }

    @Override
    public IKinAccount getAccount(int index) {
        KinAccount kinAccount = kinClient.getAccount(index);
        return new KinAccountSdkImpl(kinAccount, whitelistService);
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
            return new KinAccountSdkImpl(kinAccount, whitelistService);
        } catch (kin.sdk.exception.CryptoException e) {
            throw new CryptoException(e.getMessage(), e.getCause());
        } catch (kin.sdk.exception.CreateAccountException e) {
            throw new CreateAccountException(e.getCause());
        } catch (kin.sdk.exception.CorruptedDataException e) {
            throw new CorruptedDataException(e.getMessage(), e.getCause());
        }
    }
}



