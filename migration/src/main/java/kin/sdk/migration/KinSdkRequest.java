package kin.sdk.migration;

import kin.sdk.ResultCallback;
import kin.sdk.Request;

public class KinSdkRequest<T,R> implements IRequest<T>{

    interface Transformer<T,R> {
        T transform(R r);
    }

    private final Request<R> request;
    private final Transformer<T,R> transformer;

    KinSdkRequest(Request<R> request, Transformer<T,R> transformer) {
        this.request = request;
        this.transformer = transformer;
    }

    @Override
    public void run(final IResultCallback<T> callback) {
        if (request != null) {
            request.run(new ResultCallback<R>() {
                @Override
                public void onResult(R result) {
                    callback.onResult(transformer.transform(result));
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        } else {
            callback.onResult(null);
        }
    }

    @Override
    public synchronized void cancel(boolean mayInterruptIfRunning) {
        request.cancel(mayInterruptIfRunning);
    }
}
