package kin.sdk.migration;

/**
 * Represents KinAccount method invocation, each request will run sequentially on background thread,
 * and will notify ResultCallback with success or error on main thread.
 *
 * @param <T> request result type
 */
public interface IRequest<T> {

    /**
     * Run request asynchronously, notify {@code callback} with successful result or error
     */
    void run(IResultCallback<T> callback);

    /**
     * Cancel Request and detach its callback,
     * an attempt will be made to cancel ongoing request, if request has not run yet it will never run.
     *
     * @param mayInterruptIfRunning true if the request should be interrupted; otherwise, in-progress requests are
     * allowed to complete
     */
    void cancel(boolean mayInterruptIfRunning);

}
