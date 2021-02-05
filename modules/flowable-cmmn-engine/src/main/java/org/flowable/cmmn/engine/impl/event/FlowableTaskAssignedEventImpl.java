package org.flowable.cmmn.engine.impl.event;

import org.flowable.cmmn.api.event.FlowableTaskAssignedEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;
import org.flowable.task.api.Task;

/**
 * @author David Lamas
 */
public class FlowableTaskAssignedEventImpl extends FlowableEngineEventImpl implements FlowableTaskAssignedEvent {
    protected Task task;

    public FlowableTaskAssignedEventImpl(Task task) {
        super(FlowableEngineEventType.TASK_ASSIGNED, ScopeTypes.CMMN, task.getScopeId(), task.getId(), task.getScopeDefinitionId());
        this.task = task;
    }

    @Override
    public Task getEntity() {
        return task;
    }
}
