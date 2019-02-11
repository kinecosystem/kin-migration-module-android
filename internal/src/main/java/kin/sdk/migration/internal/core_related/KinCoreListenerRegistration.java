package kin.sdk.migration.internal.core_related;

import kin.core.ListenerRegistration;
import kin.sdk.migration.common.interfaces.IListenerRegistration;

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
