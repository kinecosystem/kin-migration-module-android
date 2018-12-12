package kin.sdk.migration;

import kin.sdk.ListenerRegistration;

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
