package kin.sdk.migration.sdk_related;

import kin.sdk.ListenerRegistration;
import kin.sdk.migration.interfaces.IListenerRegistration;

public class KinSdkListenerRegistration implements IListenerRegistration {

    private ListenerRegistration listenerRegistration;

    KinSdkListenerRegistration(ListenerRegistration listenerRegistration) {
        this.listenerRegistration = listenerRegistration;
    }

    @Override
    public void remove() {
        listenerRegistration.remove();
    }
}
