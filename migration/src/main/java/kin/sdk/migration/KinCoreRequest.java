package kin.sdk.migration;

import kin.core.Request;
import kin.core.ResultCallback;

public class KinCoreRequest<T,R> implements IRequest<T>{

    interface Transformer<T,R> {
        T transform(R r);
    }

    private final Request<R> request;
    private final Transformer<T,R> transformer;

    KinCoreRequest(Request<R> request, Transformer<T,R> transformer) {
        this.request = request;
        this.transformer = transformer;
    }

    @Override
    public void run(final IResultCallback<T> callback) {
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
    }

    @Override
    public synchronized void cancel(boolean mayInterruptIfRunning) {
        request.cancel(mayInterruptIfRunning);
    }

}
