package kin.sdk.migration;


import android.support.annotation.NonNull;
import java.util.ArrayList;
import kin.sdk.migration.exception.TransactionFailedException;
import org.stellar.sdk.responses.SubmitTransactionResponse;

final class Utils {

	static final int MAX_RETRIES = 3;

    private Utils() {
        //no instances
    }

    static TransactionFailedException createTransactionException(@NonNull SubmitTransactionResponse response)
        throws TransactionFailedException {
        ArrayList<String> operationsResultCodes = null;
        String transactionResultCode = null;
        if (response.getExtras() != null && response.getExtras().getResultCodes() != null) {
            SubmitTransactionResponse.Extras.ResultCodes resultCodes = response.getExtras().getResultCodes();
            operationsResultCodes = resultCodes.getOperationsResultCodes();
            transactionResultCode = resultCodes.getTransactionResultCode();
        }
        return new TransactionFailedException(transactionResultCode, operationsResultCodes);
    }

    static void checkNotNull(Object obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(paramName + " == null");
        }
    }
}
