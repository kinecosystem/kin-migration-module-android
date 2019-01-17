package kin.sdk.migration.interfaces;

import kin.sdk.migration.exception.WhitelistTransactionFailedException;
import kin.sdk.migration.sdk_related.WhitelistResult;

public interface IWhitelistService {

    /**
     * Method which sign the transaction so it will be in the whitelist
     * @param whitelistableTransaction is a wrapper object which holds the transaction payload and
     *                                 the network passphrase.
     *                                 in blockchain terms, both of those objects are composing the transaction envelope
     * @return A {@link WhitelistResult} which represent result of the white list transaction operation.
     * @throws WhitelistTransactionFailedException is thrown if there was any failure while trying to "white" list(sign) the transaction.
     */
    WhitelistResult onWhitelistableTransactionReady(IWhitelistableTransaction whitelistableTransaction) throws WhitelistTransactionFailedException;

}
