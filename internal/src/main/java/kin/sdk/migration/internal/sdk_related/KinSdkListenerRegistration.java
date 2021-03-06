package kin.sdk.migration.internal.sdk_related;

import kin.sdk.ListenerRegistration;
import kin.sdk.migration.common.interfaces.IListenerRegistration;

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
