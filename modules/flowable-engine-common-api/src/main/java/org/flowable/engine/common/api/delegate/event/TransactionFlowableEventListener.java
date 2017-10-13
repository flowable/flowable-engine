package org.flowable.engine.common.api.delegate.event;

/**
 * Describes a class that listens for {@link FlowableEvent}s dispatched by the engine.
 *
 * @author Frederik Heremans
 */

public interface TransactionFlowableEventListener extends FlowableEventListener {

    String ON_TRANSACTION_BEFORE_COMMIT = "before-commit";
    String ON_TRANSACTION_COMMITTED = "committed";
    String ON_TRANSACTION_ROLLED_BACK = "rolled-back";


    String getOnTransaction();

    void setOnTransaction(String onTransaction);

}
