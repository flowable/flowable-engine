package org.flowable.standalone.event;

import org.flowable.bpmn.model.Process;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * This class...
 */
public class GetProcessOnDefinitionInitializedListener implements FlowableEventListener {

    public static String processId = null;

    @Override
    public void onEvent(FlowableEvent event) {
        if (((FlowableEntityEvent) event).getEntity() instanceof ProcessDefinitionEntity) {
            Process process = ProcessDefinitionUtil.getProcess(((ProcessDefinitionEntity) ((FlowableEntityEvent) event).getEntity()).getId());
            processId = process.getId();
        }

    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
