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

package org.flowable.engine.impl.persistence.entity;

import static org.flowable.engine.impl.util.CommandContextUtil.getEntityCache;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author martin.grofcik
 */
public class ActivityInstanceEntityManagerImpl extends AbstractEntityManager<ActivityInstanceEntity> implements ActivityInstanceEntityManager {

    protected ActivityInstanceDataManager activityInstanceDataManager;

    protected final boolean usePrefixId;
    protected final boolean recordRuntimeActivities;

    public ActivityInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ActivityInstanceDataManager activityInstanceDataManager) {
        super(processEngineConfiguration);
        this.activityInstanceDataManager = activityInstanceDataManager;
        this.usePrefixId = processEngineConfiguration.isUsePrefixId();
        this.recordRuntimeActivities = processEngineConfiguration.isRecordRuntimeActivities();
    }

    @Override
    protected DataManager<ActivityInstanceEntity> getDataManager() {
        return activityInstanceDataManager;
    }

    @Override
    public List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return activityInstanceDataManager.findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
    }
    
    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return activityInstanceDataManager.findActivityInstancesByExecutionIdAndActivityId(executionId, activityId);
    }

    @Override
    public List<ActivityInstanceEntity> findUnfinishedActivityInstancesByProcessInstanceId(String processInstanceId) {
        return activityInstanceDataManager.findUnfinishedActivityInstancesByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteActivityInstancesByProcessInstanceId(String processInstanceId) {
        if (recordRuntimeActivities) {
            activityInstanceDataManager.deleteActivityInstancesByProcessInstanceId(processInstanceId);
        }
    }
    @Override
    public void deleteActivityInstancesByProcessDefinitionId(String processDefinitionId) {
        if (recordRuntimeActivities) {
            activityInstanceDataManager.deleteActivityInstancesByProcessDefinitionId(processDefinitionId);
        }
    }

    @Override
    public long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return activityInstanceDataManager.findActivityInstanceCountByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return activityInstanceDataManager.findActivityInstancesByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return activityInstanceDataManager.findActivityInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return activityInstanceDataManager.findActivityInstanceCountByNativeQuery(parameterMap);
    }

    @Override
    public void recordActivityStart(ExecutionEntity executionEntity) {
        HistoricActivityInstance historicActivityInstanceEntity = getHistoryManager().recordActivityStart(executionEntity);
        recordRuntimeActivityStart(executionEntity, historicActivityInstanceEntity);
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason) {
        HistoricActivityInstance historicActivityInstance = getHistoryManager().recordActivityEnd(executionEntity, deleteReason);
        recordActivityInstanceEnd(executionEntity, deleteReason, historicActivityInstance);
    }

    @Override
    public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance) {
        if (recordRuntimeActivities) {
            ActivityInstanceEntity activityInstance = findActivityInstance(parentExecution, false, true);
            if (activityInstance != null) {
                activityInstance.setCalledProcessInstanceId(subProcessInstance.getProcessInstanceId());
            }
        }

        getHistoryManager().recordSubProcessInstanceStart(parentExecution, subProcessInstance);
    }

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        recordActivityTaskCreated(task, execution);
        getHistoryManager().recordTaskCreated(task, execution);
    }

    protected void recordActivityTaskCreated(TaskEntity task, ExecutionEntity execution) {
        if (recordRuntimeActivities && execution != null) {
            ActivityInstanceEntity activityInstance = findActivityInstance(execution, false, true);
            if (activityInstance != null) {
                activityInstance.setTaskId(task.getId());
            }
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        recordActivityTaskInfoChange(taskEntity);
        getHistoryManager().recordTaskInfoChange(taskEntity);
    }

    @Override
    public void syncUserTaskExecution(ExecutionEntity executionEntity, FlowElement newFlowElement, String oldActivityId, TaskEntity task) {
        syncUserTaskExecutionActivityInstance(executionEntity, oldActivityId, newFlowElement);
        getHistoryManager().syncUserTaskExecution(executionEntity, oldActivityId, newFlowElement, task);
    }

    protected void syncUserTaskExecutionActivityInstance(ExecutionEntity childExecution, String oldActivityId,
        FlowElement newFlowElement) {
        ActivityInstanceEntityManager activityInstanceEntityManager = CommandContextUtil.getActivityInstanceEntityManager();
        List<ActivityInstanceEntity> activityInstances = activityInstanceEntityManager.findActivityInstancesByExecutionAndActivityId(childExecution.getId(), oldActivityId);
        for (ActivityInstanceEntity activityInstance : activityInstances) {
            activityInstance.setProcessDefinitionId(childExecution.getProcessDefinitionId());
            activityInstance.setActivityId(childExecution.getActivityId());
            activityInstance.setActivityName(newFlowElement.getName());
        }
    }

    protected void recordActivityTaskInfoChange(TaskEntity taskEntity) {
        if (recordRuntimeActivities) {
            ExecutionEntity executionEntity = getExecutionEntityManager().findById(taskEntity.getExecutionId());
            if (executionEntity != null) {
                ActivityInstanceEntity activityInstance = findActivityInstance(executionEntity, false, true);
                if (activityInstance != null && !Objects.equals(activityInstance.getAssignee(), taskEntity.getAssignee())) {
                    activityInstance.setAssignee(taskEntity.getAssignee());
                }
            }
        }
    }

    protected void recordRuntimeActivityStart(ExecutionEntity executionEntity, HistoricActivityInstance historicActivityInstance) {
        if (this.recordRuntimeActivities &&
           executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {
            ActivityInstanceEntity activityInstanceEntity = findActivityInstance(executionEntity, executionEntity.getActivityId(), false,  true);
            if (activityInstanceEntity == null) {
                if (historicActivityInstance != null) {
                    cloneActivityInstanceFrom(historicActivityInstance);
                } else {
                    createActivityInstance(executionEntity);
                }
            }
        }
    }

    protected void cloneActivityInstanceFrom(HistoricActivityInstance historicActivityInstance) {
        ActivityInstanceEntity activityInstanceEntity = create();
        activityInstanceEntity.setId(historicActivityInstance.getId());

        activityInstanceEntity.setProcessDefinitionId(historicActivityInstance.getProcessDefinitionId());
        activityInstanceEntity.setProcessInstanceId(historicActivityInstance.getProcessInstanceId());
        activityInstanceEntity.setExecutionId(historicActivityInstance.getExecutionId());
        activityInstanceEntity.setActivityId(historicActivityInstance.getActivityId());
        activityInstanceEntity.setActivityName(historicActivityInstance.getActivityName());
        activityInstanceEntity.setActivityType(historicActivityInstance.getActivityType());
        activityInstanceEntity.setStartTime(historicActivityInstance.getStartTime());
        activityInstanceEntity.setTenantId(historicActivityInstance.getTenantId());

        insert(activityInstanceEntity);
    }


    protected void createActivityInstance(ExecutionEntity executionEntity) {
        if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {

            // activity instance could have been created (but only in cache, never persisted)
            // for example when submitting form properties
            ActivityInstanceEntity activityInstanceEntityFromCache = getActivityInstanceFromCache(executionEntity.getId(),
                executionEntity.getActivityId(), true);
            if (activityInstanceEntityFromCache == null) {
                createActivityInstanceEntity(executionEntity);
            }
        }
    }

    protected void recordActivityInstanceEnd(ExecutionEntity executionEntity, String deleteReason, HistoricActivityInstance historicActivityInstance) {
        if (this.recordRuntimeActivities) {
            ActivityInstanceEntity activityInstance = findActivityInstance(executionEntity, false, true);
            if (activityInstance != null) {
                if (historicActivityInstance != null) {
                    activityInstance.setDeleteReason(deleteReason);
                    activityInstance.setEndTime(historicActivityInstance.getEndTime());
                    activityInstance.setDurationInMillis(historicActivityInstance.getDurationInMillis());
                } else {
                    activityInstance.markEnded(deleteReason);
                }
            }
        }
    }

    public ActivityInstanceDataManager getActivityInstanceDataManager() {
        return activityInstanceDataManager;
    }

    public void setActivityInstanceDataManager(ActivityInstanceDataManager activityInstanceDataManager) {
        this.activityInstanceDataManager = activityInstanceDataManager;
    }

    public ActivityInstanceEntity findActivityInstance(ExecutionEntity execution, boolean createOnNotFound, boolean endTimeMustBeNull) {
        String activityId = getActivityIdForExecution(execution);
        return activityId != null ? findActivityInstance(execution, activityId, createOnNotFound, endTimeMustBeNull) : null;
    }

    protected ActivityInstanceEntity findActivityInstance(ExecutionEntity execution, String activityId, boolean createOnNotFound, boolean endTimeMustBeNull) {

        // No use looking for the HistoricActivityInstance when no activityId is provided.
        if (activityId == null) {
            return null;
        }

        String executionId = execution.getId();

        // Check the cache
        ActivityInstanceEntity activityInstanceFromCache = getActivityInstanceFromCache(executionId, activityId, endTimeMustBeNull);
        if (activityInstanceFromCache != null) {
            return activityInstanceFromCache;
        }

        // If the execution was freshly created, there is no need to check the database,
        // there can never be an entry for a activity instance with this execution id.
        if (!execution.isInserted() && !execution.isProcessInstanceType()) {

            // Check the database
            List<ActivityInstanceEntity> activityInstances = getActivityInstanceEntityManager()
                .findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
            if (endTimeMustBeNull) {
                // cache can be updated before DB
                ActivityInstanceEntity activityFromCache = getActivityInstanceFromCache(executionId, activityId, false);
                if (activityFromCache != null && activityFromCache.getEndTime() != null) {
                    activityInstances.remove(activityFromCache);
                }
            }
            if (activityInstances.size() > 0) {
                return activityInstances.get(0);
            }

        }

        if (createOnNotFound
            && ((execution.getCurrentFlowElement() != null && execution.getCurrentFlowElement() instanceof FlowNode) || execution.getCurrentFlowElement() == null)) {
            return createActivityInstanceEntity(execution);
        }

        return null;
    }

    protected ActivityInstanceEntity createActivityInstanceEntity(ExecutionEntity execution) {
        IdGenerator idGenerator = getProcessEngineConfiguration().getIdGenerator();

        String processDefinitionId = execution.getProcessDefinitionId();
        String processInstanceId = execution.getProcessInstanceId();

        ActivityInstanceEntity activityInstanceEntity = create();
        if (usePrefixId) {
            activityInstanceEntity.setId(activityInstanceEntity.getIdPrefix() + idGenerator.getNextId());
        } else {
            activityInstanceEntity.setId(idGenerator.getNextId());
        }

        activityInstanceEntity.setProcessDefinitionId(processDefinitionId);
        activityInstanceEntity.setProcessInstanceId(processInstanceId);
        activityInstanceEntity.setExecutionId(execution.getId());
        activityInstanceEntity.setActivityId(execution.getActivityId());
        if (execution.getCurrentFlowElement() != null) {
            activityInstanceEntity.setActivityName(execution.getCurrentFlowElement().getName());
            activityInstanceEntity.setActivityType(parseActivityType(execution.getCurrentFlowElement()));
        }
        Date now = getClock().getCurrentTime();
        activityInstanceEntity.setStartTime(now);

        if (execution.getTenantId() != null) {
            activityInstanceEntity.setTenantId(execution.getTenantId());
        }

        getActivityInstanceEntityManager().insert(activityInstanceEntity);
        return activityInstanceEntity;
    }

    protected ActivityInstanceEntity getActivityInstanceFromCache(String executionId, String activityId, boolean endTimeMustBeNull) {
        List<ActivityInstanceEntity> cachedActivityInstances = getEntityCache().findInCache(ActivityInstanceEntity.class);
        for (ActivityInstanceEntity cachedActivityInstance : cachedActivityInstances) {
            if (activityId != null
                && activityId.equals(cachedActivityInstance.getActivityId())
                && (!endTimeMustBeNull || cachedActivityInstance.getEndTime() == null)) {
                if (executionId.equals(cachedActivityInstance.getExecutionId())) {
                    return cachedActivityInstance;
                }
            }
        }

        return null;
    }

    protected String parseActivityType(FlowElement element) {
        String elementType = element.getClass().getSimpleName();
        elementType = elementType.substring(0, 1).toLowerCase() + elementType.substring(1);
        return elementType;
    }

    protected String getActivityIdForExecution(ExecutionEntity execution) {
        String activityId = null;
        if (execution.getCurrentFlowElement() instanceof FlowNode) {
            activityId = execution.getCurrentFlowElement().getId();
        } else if (execution.getCurrentFlowElement() instanceof SequenceFlow
            && execution.getCurrentFlowableListener() == null) { // while executing sequence flow listeners, we don't want historic activities
            activityId = ((SequenceFlow) execution.getCurrentFlowElement()).getSourceFlowElement().getId();
        }
        return activityId;
    }

}
