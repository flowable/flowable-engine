package org.flowable.engine.test.api.history;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@FlowableTest
public class ChangeHistoryLevelTest {

    protected HistoryLevel originalHistoryLevel;

    @BeforeEach
    void getHistoryLevel(ProcessEngineConfiguration configuration) {
        originalHistoryLevel = configuration.getHistoryLevel();
    }

    @AfterEach
    void restoreHistoryLevel(ProcessEngineConfiguration configuration) {
        configuration.setHistoryLevel(originalHistoryLevel);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToActivityTaskComplete(ProcessEngineConfiguration configuration, RuntimeService runtimeService,
                                    TaskService taskService, HistoryService historyService) {
        configuration.setHistoryLevel(HistoryLevel.NONE);
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();
        configuration.setHistoryLevel(HistoryLevel.ACTIVITY);
        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.complete(task.getId());

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId(task.getId()).singleResult();
        assertThat(historicActivityInstance).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToFullVariableSet(ProcessEngineConfiguration configuration, RuntimeService runtimeService,
                                    TaskService taskService, HistoryService historyService) {
        configuration.setHistoryLevel(HistoryLevel.NONE);
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("var", "initialValue").start();
        configuration.setHistoryLevel(HistoryLevel.FULL);
        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.complete(task.getId(), Collections.singletonMap("var", "updatedValue"));

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId(task.getId()).singleResult();
        assertThat(historicActivityInstance).isNull();
        HistoricVariableInstance var = historyService.createHistoricVariableInstanceQuery().processInstanceId(oneTaskProcess.getId()).variableName("var").singleResult();
        assertThat(var).extracting(HistoricVariableInstance::getValue).isEqualTo("updatedValue");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToFullClaimTask(ProcessEngineConfiguration configuration, RuntimeService runtimeService,
                                    TaskService taskService, HistoryService historyService) {
        configuration.setHistoryLevel(HistoryLevel.NONE);
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("var", "initialValue").start();
        configuration.setHistoryLevel(HistoryLevel.FULL);
        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.claim(task.getId(), "kermit");
        taskService.complete(task.getId(), Collections.singletonMap("var", "updatedValue"));

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId(task.getId()).singleResult();
        assertThat(historicActivityInstance).isNull();
    }
}
