package org.flowable.app.service.runtime;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.flowable.engine.impl.bpmn.behavior.SimulationSubProcessActivityBehavior.VIRTUAL_PROCESS_ENGINE_VARIABLE_NAME;

public class TestHelper {

    @SuppressWarnings("unused")
    public static List<String> getSubProcessInstanceIds(String rootProcessInstanceId, DelegateExecution execution) {
        List<Execution> processInstances = getVirtualProcessEngine(execution)
                .getRuntimeService().createExecutionQuery().rootProcessInstanceId(rootProcessInstanceId).onlyProcessInstanceExecutions().list();

        List<String> processInstanceIds = new ArrayList<>(processInstances.size());
        for (Execution processInstance : processInstances) {
            processInstanceIds.add(processInstance.getId());
        }
        return processInstanceIds;
    }

    public static ProcessEngine getVirtualProcessEngine(DelegateExecution execution) {
        return ProcessEngines.getProcessEngine((String) execution.getVariable(VIRTUAL_PROCESS_ENGINE_VARIABLE_NAME));
    }

    @SuppressWarnings("unused")
    public String getKeyFromProcessDefinitionId(String processDefinitionId, DelegateExecution execution) {
        return getVirtualProcessEngine(execution).getRepositoryService().
                createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult().getKey();
    }

}
