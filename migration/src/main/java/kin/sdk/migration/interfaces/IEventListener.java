package kin.sdk.migration.interfaces;

public interface IEventListener<T> {

    void onEvent(T data);
}
