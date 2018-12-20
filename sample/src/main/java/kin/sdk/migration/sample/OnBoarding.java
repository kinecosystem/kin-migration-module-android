package kin.sdk.migration.sample;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import kin.sdk.migration.interfaces.IKinAccount;
import kin.sdk.migration.interfaces.IListenerRegistration;
import kin.sdk.migration.interfaces.IResultCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class OnBoarding {

    // TODO: 13/12/2018 when blockchain team will finish the option to fund account for new kin then fix it here so it won't be local host
    private static final int FUND_KIN_AMOUNT = 6000;
    private static final String TEST_SDK_URL_CREATE_ACCOUNT = "http://10.0.2.2:8000?addr=%s&amount=" + String.valueOf(FUND_KIN_AMOUNT);
    private static final String TEST_CORE_URL_CREATE_ACCOUNT = "http://friendbot-playground.kininfrastructure.com/?addr=";
    private static final String TEST_SDK_URL_FUND = "http://10.0.2.2:8000/fund?addr=%s&amount=" + String.valueOf(FUND_KIN_AMOUNT);
    private static final String TEST_CORE_URL_FUND =
        "http://faucet-playground.kininfrastructure.com/fund?account=%s&amount=" + String.valueOf(FUND_KIN_AMOUNT);
    private final OkHttpClient okHttpClient;
    private final Handler handler;
    private IListenerRegistration listenerRegistration;

    public interface Callbacks {

        void onSuccess();
        void onFailure(Exception e);

    }

    OnBoarding() {
        handler = new Handler(Looper.getMainLooper());
        okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    }

    void onBoard(final boolean isNewKin, @NonNull IKinAccount account, @NonNull Callbacks callbacks) {
        Runnable accountCreationListeningTimeout = () -> {
            listenerRegistration.remove();
            fireOnFailure(callbacks, new TimeoutException("Waiting for account creation event time out"));
        };
        listenerRegistration = account.addAccountCreationListener(data -> {
            listenerRegistration.remove();
            handler.removeCallbacks(accountCreationListeningTimeout);
            if (isNewKin) {
               fireOnSuccess(callbacks);
            } else {
                activateAccount(account, callbacks);
            }
        });
        createAccount(isNewKin, account, callbacks);
    }

    private void activateAccount(@NonNull IKinAccount account, @NonNull Callbacks callbacks) {
        account.activate()
                .run(new IResultCallback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        //This is not mandatory part of onboarding, account is now ready to send/receive kin
                        fundAccountWithKin(account, callbacks);
                    }

                    @Override
                    public void onError(Exception e) {
                        fireOnFailure(callbacks, e);
                    }
                });
    }

    private void createAccount(boolean isNewKin, @NonNull IKinAccount account, @NonNull Callbacks callbacks) {
        String createAccountUrl = isNewKin ?
                String.format(TEST_SDK_URL_CREATE_ACCOUNT, account.getPublicAddress()) : TEST_CORE_URL_CREATE_ACCOUNT + account.getPublicAddress();
        Request request = new Request.Builder()
            .url(createAccountUrl)
            .get()
            .build();
        okHttpClient.newCall(request)
            .enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    fireOnFailure(callbacks, e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    int code = response.code();
                    response.close();
                    if (code != 200) {
                        fireOnFailure(callbacks, new Exception("Create account - response code is " + response.code()));
                    }
                }
            });
    }

    private void fundAccountWithKin(IKinAccount account, @NonNull Callbacks callbacks) {
        Request request = new Request.Builder()
                .url(String.format(TEST_CORE_URL_FUND, account.getPublicAddress()))
                .get()
                .build();
        okHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        fireOnFailure(callbacks, e);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        int code = response.code();
                        response.close();
                        if (code == 200) {
                            //will trigger a call to get updated balance
                            fireOnSuccess(callbacks);
                        } else {
                            fireOnFailure(callbacks, new Exception("Fund account - response code is " + response.code()));
                        }
                    }
                });

    }

    private void fireOnFailure(@NonNull Callbacks callbacks, Exception ex) {
        handler.post(() -> callbacks.onFailure(ex));
    }

    private void fireOnSuccess(@NonNull Callbacks callbacks) {
        handler.post(callbacks::onSuccess);
    }
}
