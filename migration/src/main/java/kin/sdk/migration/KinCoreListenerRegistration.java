package kin.sdk.migration;

import kin.core.ListenerRegistration;

public class KinCoreListenerRegistration implements IListenerRegistration {

    private ListenerRegistration listenerRegistration;

    KinCoreListenerRegistration(ListenerRegistration listenerRegistration) {
        this.listenerRegistration = listenerRegistration;
    }

    @Override
    public void remove() {
        listenerRegistration.remove();
    }
}
