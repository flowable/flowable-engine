/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.DefaultHistoryManager;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.HistoryJob;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Assert;

import junit.framework.AssertionFailedError;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class AbstractFlowableTestCase extends AbstractTestCase {

    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = new ArrayList<>();

    static {
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_GE_PROPERTY");
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_ID_PROPERTY");
    }

    protected ProcessEngine processEngine;

    protected String deploymentIdFromDeploymentAnnotation;
    protected List<String> deploymentIdsForAutoCleanup = new ArrayList<>();
    protected Throwable exception;

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;
    protected FormService formService;
    protected HistoryService historyService;
    protected IdentityService identityService;
    protected ManagementService managementService;
    protected DynamicBpmnService dynamicBpmnService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Always reset authenticated user to avoid any mistakes
        identityService.setAuthenticatedUserId(null);
    }

    protected abstract void initializeProcessEngine();

    // Default: do nothing
    protected void closeDownProcessEngine() {
    }

    protected void nullifyServices() {
        processEngineConfiguration = null;
        repositoryService = null;
        runtimeService = null;
        taskService = null;
        formService = null;
        historyService = null;
        identityService = null;
        managementService = null;
        dynamicBpmnService = null;
    }

    @Override
    public void runBare() throws Throwable {
        initializeProcessEngine();
        initializeServices();

        try {

            deploymentIdFromDeploymentAnnotation = TestHelper.annotationDeploymentSetUp(processEngine, getClass(), getName());

            super.runBare();

            validateHistoryData();

        } catch (AssertionFailedError e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            exception = e;
            throw e;

        } catch (Throwable e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("EXCEPTION: {}", e, e);
            exception = e;
            throw e;

        } finally {
            
            boolean isAsyncHistoryEnabled = processEngineConfiguration.isAsyncHistoryEnabled();
            
            if (isAsyncHistoryEnabled) {
                List<HistoryJob> jobs = managementService.createHistoryJobQuery().list();
                for (HistoryJob job : jobs) {
                    managementService.deleteHistoryJob(job.getId());
                }
            }
            
            HistoryManager asyncHistoryManager = null;
            try {
                if (isAsyncHistoryEnabled) {
                    processEngineConfiguration.setAsyncHistoryEnabled(false);
                    asyncHistoryManager = processEngineConfiguration.getHistoryManager();
                    processEngineConfiguration.setHistoryManager(new DefaultHistoryManager(processEngineConfiguration, processEngineConfiguration.getHistoryLevel()));
                }
    
                if (deploymentIdFromDeploymentAnnotation != null) {
                    TestHelper.annotationDeploymentTearDown(processEngine, deploymentIdFromDeploymentAnnotation, getClass(), getName());
                    deploymentIdFromDeploymentAnnotation = null;
                }
    
                for (String autoDeletedDeploymentId : deploymentIdsForAutoCleanup) {
                    repositoryService.deleteDeployment(autoDeletedDeploymentId, true);
                }
                deploymentIdsForAutoCleanup.clear();
    
                assertAndEnsureCleanDb();
                
            } finally {
            
                if (isAsyncHistoryEnabled) {
                    processEngineConfiguration.setAsyncHistoryEnabled(true);
                    processEngineConfiguration.setHistoryManager(asyncHistoryManager);
                }
                
                processEngineConfiguration.getClock().reset();
            }

            // Can't do this in the teardown, as the teardown will be called as part of the super.runBare
            closeDownProcessEngine();
        }
    }

    protected void validateHistoryData() {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {

            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().finished().list();

            for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {

                assertNotNull("Historic process instance has no process definition id", historicProcessInstance.getProcessDefinitionId());
                assertNotNull("Historic process instance has no process definition key", historicProcessInstance.getProcessDefinitionKey());
                assertNotNull("Historic process instance has no process definition version", historicProcessInstance.getProcessDefinitionVersion());
                assertNotNull("Historic process instance has no deployment id", historicProcessInstance.getDeploymentId());
                assertNotNull("Historic process instance has no start activity id", historicProcessInstance.getStartActivityId());
                assertNotNull("Historic process instance has no start time", historicProcessInstance.getStartTime());
                assertNotNull("Historic process instance has no end time", historicProcessInstance.getEndTime());

                String processInstanceId = historicProcessInstance.getId();

                // tasks
                List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstanceId).list();
                
                if (historicTaskInstances != null && historicTaskInstances.size() > 0) {
                    for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                        assertEquals(processInstanceId, historicTaskInstance.getProcessInstanceId());
                        if (historicTaskInstance.getClaimTime() != null) {
                            assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no work time", historicTaskInstance.getWorkTimeInMillis());
                        }
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no id", historicTaskInstance.getId());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no process instance id", historicTaskInstance.getProcessInstanceId());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no execution id", historicTaskInstance.getExecutionId());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no process definition id", historicTaskInstance.getProcessDefinitionId());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no task definition key", historicTaskInstance.getTaskDefinitionKey());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no create time", historicTaskInstance.getCreateTime());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no start time", historicTaskInstance.getStartTime());
                        assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no end time", historicTaskInstance.getEndTime());
                    }
                }

                // activities
                List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstanceId).list();
                if (historicActivityInstances != null && historicActivityInstances.size() > 0) {
                    for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                        assertEquals(processInstanceId, historicActivityInstance.getProcessInstanceId());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no activity id", historicActivityInstance.getActivityId());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no activity type", historicActivityInstance.getActivityType());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no process definition id", historicActivityInstance.getProcessDefinitionId());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no process instance id", historicActivityInstance.getProcessInstanceId());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no execution id", historicActivityInstance.getExecutionId());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no start time", historicActivityInstance.getStartTime());
                        assertNotNull("Historic activity instance " + historicActivityInstance.getId() + " / " + historicActivityInstance.getActivityId() + " has no end time", historicActivityInstance.getEndTime());
                    }
                }
            }

        }
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        LOGGER.debug("verifying that db is clean after test");
        Map<String, Long> tableCounts = managementService.getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(processEngineConfiguration.getDatabaseTablePrefix(), "");
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  ").append(tableName).append(": ").append(count).append(" record(s) ");
                }
            }
        }
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            LOGGER.error(EMPTY_LINE);
            LOGGER.error(outputMessage.toString());

            LOGGER.info("dropping and recreating db");

            CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
            CommandConfig config = new CommandConfig().transactionNotSupported();
            commandExecutor.execute(config, new Command<Object>() {
                @Override
                public Object execute(CommandContext commandContext) {
                    DbSchemaManager dbSchemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDbSchemaManager();
                    dbSchemaManager.dbSchemaDrop();
                    dbSchemaManager.dbSchemaCreate();
                    return null;
                }
            });

            if (exception != null) {
                throw exception;
            } else {
                Assert.fail(outputMessage.toString());
            }
        } else {
            LOGGER.info("database was clean");
        }
    }

    protected void initializeServices() {
        processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
        taskService = processEngine.getTaskService();
        formService = processEngine.getFormService();
        historyService = processEngine.getHistoryService();
        identityService = processEngine.getIdentityService();
        managementService = processEngine.getManagementService();
        dynamicBpmnService = processEngine.getDynamicBpmnService();
    }
    
    public void assertProcessEnded(final String processInstanceId) {
        assertProcessEnded(processInstanceId, 10000);
    }

    public void assertProcessEnded(final String processInstanceId, long timeout) {
        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

        if (processInstance != null) {
            throw new AssertionFailedError("Expected finished process instance '" + processInstanceId + "' but it was still in the db");
        }

        // Verify historical data if end times are correctly set
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration, timeout)) {

            // process instance
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            assertEquals(processInstanceId, historicProcessInstance.getId());
            assertNotNull("Historic process instance has no start time", historicProcessInstance.getStartTime());
            assertNotNull("Historic process instance has no end time", historicProcessInstance.getEndTime());

            // tasks
            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId).list();
            if (historicTaskInstances != null && historicTaskInstances.size() > 0) {
                for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                    assertEquals(processInstanceId, historicTaskInstance.getProcessInstanceId());
                    assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no start time", historicTaskInstance.getStartTime());
                    assertNotNull("Historic task " + historicTaskInstance.getTaskDefinitionKey() + " has no end time", historicTaskInstance.getEndTime());
                }
            }

            // activities
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId).list();
            if (historicActivityInstances != null && historicActivityInstances.size() > 0) {
                for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                    assertEquals(processInstanceId, historicActivityInstance.getProcessInstanceId());
                    assertNotNull(historicActivityInstance.getId() + " Historic activity instance '" + historicActivityInstance.getActivityId() +"' has no start time", historicActivityInstance.getStartTime());
                    assertNotNull(historicActivityInstance.getId() + " Historic activity instance '" + historicActivityInstance.getActivityId() + "' has no end time", historicActivityInstance.getEndTime());
                }
            }
        }
    }

    public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis);
    }

    public void waitForJobExecutorOnCondition(long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
        JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, maxMillisToWait, intervalMillis, condition);
    }

    public void executeJobExecutorForTime(long maxMillisToWait, long intervalMillis) {
        JobTestHelper.executeJobExecutorForTime(processEngineConfiguration, maxMillisToWait, intervalMillis);
    }

    public void waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(long maxMillisToWait, long intervalMillis) {
        JobTestHelper.waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis);
    }
    
    public void waitForJobExecutorToProcessAllHistoryJobs(long maxMillisToWait, long intervalMillis) {
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis);
    }
    
    public void waitForHistoryJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, maxMillisToWait, intervalMillis);
    }

    /**
     * Since the 'one task process' is used everywhere the actual process content doesn't matter, instead of copying around the BPMN 2.0 xml one could use this method which gives a {@link BpmnModel}
     * version of the same process back.
     */
    public BpmnModel createOneTaskTestProcess() {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        model.addProcess(process);
        process.setId("oneTaskProcess");
        process.setName("The one task process");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        startEvent.setName("The start");
        process.addFlowElement(startEvent);

        UserTask userTask = new UserTask();
        userTask.setName("The Task");
        userTask.setId("theTask");
        userTask.setAssignee("kermit");
        process.addFlowElement(userTask);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("theEnd");
        endEvent.setName("The end");
        process.addFlowElement(endEvent);

        process.addFlowElement(new SequenceFlow("start", "theTask"));
        process.addFlowElement(new SequenceFlow("theTask", "theEnd"));

        return model;
    }

    public BpmnModel createTwoTasksTestProcess() {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        model.addProcess(process);
        process.setId("twoTasksProcess");
        process.setName("The two tasks process");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        process.addFlowElement(startEvent);

        UserTask userTask = new UserTask();
        userTask.setName("The First Task");
        userTask.setId("task1");
        userTask.setAssignee("kermit");
        process.addFlowElement(userTask);

        UserTask userTask2 = new UserTask();
        userTask2.setName("The Second Task");
        userTask2.setId("task2");
        userTask2.setAssignee("kermit");
        process.addFlowElement(userTask2);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("theEnd");
        process.addFlowElement(endEvent);

        process.addFlowElement(new SequenceFlow("start", "task1"));
        process.addFlowElement(new SequenceFlow("start", "task2"));
        process.addFlowElement(new SequenceFlow("task1", "theEnd"));
        process.addFlowElement(new SequenceFlow("task2", "theEnd"));

        return model;
    }

    /**
     * Creates and deploys the one task process. See {@link #createOneTaskTestProcess()}.
     * 
     * @return The process definition id (NOT the process definition key) of deployed one task process.
     */
    public String deployOneTaskTestProcess() {
        BpmnModel bpmnModel = createOneTaskTestProcess();
        Deployment deployment = repositoryService.createDeployment().addBpmnModel("oneTasktest.bpmn20.xml", bpmnModel).deploy();

        deploymentIdsForAutoCleanup.add(deployment.getId()); // For auto-cleanup

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        return processDefinition.getId();
    }

    public String deployTwoTasksTestProcess() {
        BpmnModel bpmnModel = createTwoTasksTestProcess();
        Deployment deployment = repositoryService.createDeployment().addBpmnModel("twoTasksTestProcess.bpmn20.xml", bpmnModel).deploy();

        deploymentIdsForAutoCleanup.add(deployment.getId()); // For auto-cleanup

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        return processDefinition.getId();
    }

    //
    // HELPERS
    //
    
    protected void deleteDeployments() {
        boolean isAsyncHistoryEnabled = processEngineConfiguration.isAsyncHistoryEnabled();
        HistoryManager asyncHistoryManager = null;
        if (isAsyncHistoryEnabled) {
            processEngineConfiguration.setAsyncHistoryEnabled(false);
            asyncHistoryManager = processEngineConfiguration.getHistoryManager();
            processEngineConfiguration.setHistoryManager(new DefaultHistoryManager(processEngineConfiguration, processEngineConfiguration.getHistoryLevel()));
        }
        
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
        
        if (isAsyncHistoryEnabled) {
            processEngineConfiguration.setAsyncHistoryEnabled(true);
            processEngineConfiguration.setHistoryManager(asyncHistoryManager);
        }
    }
    
    protected void deleteDeployment(String deploymentId) {
        boolean isAsyncHistoryEnabled = processEngineConfiguration.isAsyncHistoryEnabled();
        HistoryManager asyncHistoryManager = null;
        if (isAsyncHistoryEnabled) {
            processEngineConfiguration.setAsyncHistoryEnabled(false);
            asyncHistoryManager = processEngineConfiguration.getHistoryManager();
            processEngineConfiguration.setHistoryManager(new DefaultHistoryManager(processEngineConfiguration, processEngineConfiguration.getHistoryLevel()));
        }
        
        repositoryService.deleteDeployment(deploymentId, true);
        
        if (isAsyncHistoryEnabled) {
            processEngineConfiguration.setAsyncHistoryEnabled(true);
            processEngineConfiguration.setHistoryManager(asyncHistoryManager);
        }
    }

    protected void assertHistoricTasksDeleteReason(ProcessInstance processInstance, String expectedDeleteReason, String... taskNames) {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            for (String taskName : taskNames) {
                List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId()).taskName(taskName).list();
                assertTrue(historicTaskInstances.size() > 0);
                for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                    assertNotNull(historicTaskInstance.getEndTime());
                    if (expectedDeleteReason == null) {
                        assertNull(historicTaskInstance.getDeleteReason());
                    } else {
                        assertTrue(historicTaskInstance.getDeleteReason().startsWith(expectedDeleteReason));
                    }
                }
            }
        }
    }

    protected void assertHistoricActivitiesDeleteReason(ProcessInstance processInstance, String expectedDeleteReason, String... activityIds) {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            for (String activityId : activityIds) {
                List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                        .activityId(activityId).processInstanceId(processInstance.getId()).list();
                assertTrue("Could not find historic activities", historicActivityInstances.size() > 0);
                for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                    assertNotNull(historicActivityInstance.getEndTime());
                    if (expectedDeleteReason == null) {
                        assertNull(historicActivityInstance.getDeleteReason());
                    } else {
                        assertTrue(historicActivityInstance.getDeleteReason().startsWith(expectedDeleteReason));
                    }
                }
            }
        }
    }

}
