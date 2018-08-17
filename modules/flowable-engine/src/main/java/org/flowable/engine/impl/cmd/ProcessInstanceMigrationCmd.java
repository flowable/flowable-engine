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

package org.flowable.engine.impl.cmd;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationCmd implements Command<Void> {

    //FIXME Remove or clean up log messages
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceMigrationCmd.class);
    protected ProcessInstanceMigrationDocument processInstanceMigrationDocument;

    //TODO make the command to work with a list of processIds, or a single processId instead of the processMigrationDocument?
    //TODO would it make it more cohesive and enable it be called as batch migration or queued for each process instance to migrate
    public ProcessInstanceMigrationCmd(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        this.processInstanceMigrationDocument = processInstanceMigrationDocument;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        LOGGER.debug(">>>> ProcessInstanceMigrationCmd execution start <<<<<");

        //TODO confirm if its correct to retrieve the runtimeService from here
        RuntimeServiceImpl runtimeService = (RuntimeServiceImpl) CommandContextUtil.getProcessEngineConfiguration().getRuntimeService();

        //BpmnModel of the new ProcessDefinition
        BpmnModel bpmnModelOfProcessDefinitionToMigrateTo = findBpmnModelOfProcessDefinitionToMigrateTo(commandContext, processInstanceMigrationDocument);

        if (bpmnModelOfProcessDefinitionToMigrateTo == null) {
            throw new FlowableException(String.format("Cannot find the Bpmn model of the process definition to migrate to, with [ID:%s] or [KEY:%s / VERSION:%s]",
                processInstanceMigrationDocument.getMigrateToProcessDefinitionId(),
                processInstanceMigrationDocument.getMigrateToProcessDefinitionKey(),
                processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion()));
        }

        String processDefinitionIdOfProcessDefinitionToMigrateTo = resolveProcessDefinitionIdOfProcessDefinitionToMigrateTo(commandContext, processInstanceMigrationDocument);
        String tenantOfProcessDefinitionToMigrateTo = resolveTenantIdOfProcessDefinitionToMigrateTo(processInstanceMigrationDocument);

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        //TODO right now if any of the processInstance migration fail, the whole command fails
        for (String processInstanceId : processInstanceMigrationDocument.getProcessInstancesIdsToMigrate()) {
            ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);
            if (!isSameTenant(processExecution.getTenantId(), tenantOfProcessDefinitionToMigrateTo)) {
                throw new FlowableException(String.format("Tenant mismatch between Process Instance ('%s') and Process Definition ('%s') to migrate to", processExecution.getTenantId(), tenantOfProcessDefinitionToMigrateTo));
            }

            ChangeActivityStateBuilder changeActivityStateBuilder = new ChangeActivityStateBuilderImpl(runtimeService);
            changeActivityStateBuilder.processInstanceId(processInstanceId);

            LOGGER.debug(">>>> Checking executions for processInstance Id:'" + processInstanceId + "' <<<<<");
            List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
            LOGGER.debug(">>>> Found '" + executions.size() + "' executions <<<<<");
            for (ExecutionEntity execution : executions) {
                LOGGER.debug(">>>> Checking execution - activityId:'" + execution.getCurrentActivityId() + "' id:'" + execution.getId() + "'  <<<<<");
                if (execution.getCurrentActivityId() != null) {
                    //If there's no specific mapping, we check if the new process definition contains it already
                    if (processInstanceMigrationDocument.getActivityMigrationMappings().containsKey(execution.getCurrentActivityId())) {
                        LOGGER.debug(">>>> Found mapping for activity '" + execution.getCurrentActivityId() + "' -> '" + processInstanceMigrationDocument.getActivityMigrationMappings().get(execution.getCurrentActivityId() + "' <<<<<"));
                        changeActivityStateBuilder.moveExecutionToActivityId(execution.getId(), processInstanceMigrationDocument.getActivityMigrationMappings().get(execution.getCurrentActivityId()));
                    } else if (isActivityInProcessDefinitionModel(execution.getCurrentActivityId(), bpmnModelOfProcessDefinitionToMigrateTo)) {
                        LOGGER.debug(">>>> Auto mapping activity '" + execution.getCurrentActivityId() + "' <<<<<");
                        changeActivityStateBuilder.moveExecutionToActivityId(execution.getId(), execution.getCurrentActivityId());
                    } else {
                        throw new FlowableException(String.format("Migration Activity mapping missing for activityId:'%s'"));
                    }
                } else {
                    //SPECIAL EXECUTION - NOT A LEAF (eg concurrent parent)
                }
                //Update processDefinition reference on the fly
                LOGGER.debug(">>>> Update processDefinitionId of execution to'" + processDefinitionIdOfProcessDefinitionToMigrateTo + "' <<<<<");
                execution.setProcessDefinitionId(processDefinitionIdOfProcessDefinitionToMigrateTo);
                execution.forceUpdate();
            }
            //Update root processExecution
            processExecution.setProcessDefinitionId(processDefinitionIdOfProcessDefinitionToMigrateTo);
            processExecution.forceUpdate();

            //Update process definition reference for TASKS
            changeProcessDefinitionReferenceOfTasks(commandContext, processInstanceId, processDefinitionIdOfProcessDefinitionToMigrateTo);

            //TODO UPDATE REFERENCE IN HISTORY
            //TODO VARIABLES?
            //TODO JOBS?
            //TODO TIMERS?

            //Run changeStateBuilder for the current processInstance
            changeActivityStateBuilder.changeState();
        }

        LOGGER.debug(">>>> ProcessInstanceMigrationCmd execution ended <<<<<");
        return null;

    }

    protected static BpmnModel findBpmnModelOfProcessDefinitionToMigrateTo(CommandContext commandContext, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        BpmnModel bpmnModel = null;
        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionId() != null) {

            bpmnModel = ProcessDefinitionUtil.getBpmnModel(processInstanceMigrationDocument.getMigrateToProcessDefinitionId());

        } else if (processInstanceMigrationDocument.getMigrateToProcessDefinitionKey() != null && processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion() != null) {

            ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
            ProcessDefinition processDefinition = processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(
                processInstanceMigrationDocument.getMigrateToProcessDefinitionKey(),
                Integer.valueOf(processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion()),
                processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId()
            );

            if (processDefinition != null) {
                bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            }
        }
        return bpmnModel;
    }

    protected static String resolveProcessDefinitionIdOfProcessDefinitionToMigrateTo(CommandContext commandContext, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionId() == null) {
            ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
            ProcessDefinition processDefinition = processDefinitionEntityManager.findProcessDefinitionByKeyAndVersionAndTenantId(
                processInstanceMigrationDocument.getMigrateToProcessDefinitionKey(),
                Integer.valueOf(processInstanceMigrationDocument.getMigrateToProcessDefinitionVersion()),
                processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId()
            );
            return processDefinition.getId();
        } else {
            return processInstanceMigrationDocument.getMigrateToProcessDefinitionId();
        }
    }

    protected static String resolveTenantIdOfProcessDefinitionToMigrateTo(ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        if (processInstanceMigrationDocument.getMigrateToProcessDefinitionId() != null) {
            ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processInstanceMigrationDocument.getMigrateToProcessDefinitionId());
            return processDefinition.getTenantId();
        }
        return processInstanceMigrationDocument.getMigrateToProcessDefinitionTenantId();
    }

    protected static boolean isSameTenant(String tenantId1, String tenantId2) {

        if (tenantId1 != null && tenantId2 != null) {
            return tenantId1.equals(tenantId2);
        } else if (tenantId1 == null && tenantId2 == null) {
            return true;
        }
        return false;
    }

    protected static boolean isActivityInProcessDefinitionModel(String activityId, BpmnModel model) {
        return model.getFlowElement(activityId) != null;
    }

    protected static void changeProcessDefinitionReferenceOfTasks(CommandContext commandContext, String processInstanceId, String processDefinitionId) {
        List<Task> tasks = CommandContextUtil.getTaskService(commandContext).createTaskQuery().processInstanceId(processInstanceId).list();
        tasks.stream().map(t -> (TaskEntity) t).forEach(t -> {
            t.setProcessDefinitionId(processDefinitionId);
            t.forceUpdate();
        });
    }

}
