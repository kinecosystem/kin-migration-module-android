package kin.sdk.migration;

public interface IEventListener<T> {

    void onEvent(T data);
}
