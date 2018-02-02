package org.flowable.engine.common.api.delegate.event;

/**
 * Describes a class that listens for {@link FlowableEvent}s dispatched by the engine.
 *
 * @author Frederik Heremans
 */

public interface TransactionFlowableEventListener extends FlowableEventListener {

    /*
     *
     * org.flowable.engine.common.impl.cfg.TransactionState
     */
    String getOnTransaction();

    void setOnTransaction(String onTransaction);

}
