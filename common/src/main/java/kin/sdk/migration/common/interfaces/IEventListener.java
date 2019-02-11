package kin.sdk.migration.common.interfaces;

public interface IEventListener<T> {

    void onEvent(T data);
}
