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

    public ActivityInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ActivityInstanceDataManager activityInstanceDataManager) {
        super(processEngineConfiguration);
        this.activityInstanceDataManager = activityInstanceDataManager;
        this.usePrefixId = processEngineConfiguration.isUsePrefixId();
    }

    @Override
    protected DataManager<ActivityInstanceEntity> getDataManager() {
        return activityInstanceDataManager;
    }

    protected List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return activityInstanceDataManager.findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
    }
    
    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return activityInstanceDataManager.findActivityInstancesByExecutionIdAndActivityId(executionId, activityId);
    }

    @Override
    public void deleteActivityInstancesByProcessInstanceId(String processInstanceId) {
        activityInstanceDataManager.deleteActivityInstancesByProcessInstanceId(processInstanceId);
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
        ActivityInstance activityInstance = recordRuntimeActivityStart(executionEntity);
        if (activityInstance != null) {
            getHistoryManager().recordActivityStart(activityInstance);
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason) {
        ActivityInstance activityInstance = recordActivityInstanceEnd(executionEntity, deleteReason);
        if (activityInstance == null) {
            getHistoryManager().recordActivityEnd(executionEntity, deleteReason);
        } else {
            getHistoryManager().recordActivityEnd(activityInstance);
        }
    }

    @Override
    public void recordSequenceFlowEnd(ExecutionEntity executionEntity) {
        ActivityInstanceEntity activityInstance = findUnfinishedFlowSequenceActivityInstance(executionEntity);
        if (activityInstance != null) {
            activityInstance.markEnded(null);
            getHistoryManager().recordActivityEnd(activityInstance);
        }
    }

    @Override
    public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance) {
        ActivityInstanceEntity activityInstance = findUnfinishedActivityInstance(parentExecution);
        if (activityInstance != null) {
            activityInstance.setCalledProcessInstanceId(subProcessInstance.getProcessInstanceId());
            getHistoryManager().updateHistoricActivityInstance(activityInstance);
        }

        getHistoryManager().recordProcessInstanceStart(subProcessInstance);
    }

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        recordActivityTaskCreated(task, execution);
        getHistoryManager().recordTaskCreated(task, execution);
    }

    protected void recordActivityTaskCreated(TaskEntity task, ExecutionEntity execution) {
        if (execution != null) {
            ActivityInstanceEntity activityInstance = findUnfinishedActivityInstance(execution);
            if (activityInstance != null) {
                activityInstance.setTaskId(task.getId());
                getHistoryManager().updateHistoricActivityInstance(activityInstance);
            }
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        ActivityInstanceEntity activityInstanceEntity = recordActivityTaskInfoChange(taskEntity);
        getHistoryManager().recordTaskInfoChange(taskEntity, activityInstanceEntity != null ? activityInstanceEntity.getId() : null);
    }

    @Override
    public void syncUserTaskExecution(ExecutionEntity executionEntity, FlowElement newFlowElement, String oldActivityId, TaskEntity task) {
        syncUserTaskExecutionActivityInstance(executionEntity, oldActivityId, newFlowElement);
        getHistoryManager().updateActivity(executionEntity, oldActivityId, newFlowElement, task);
    }

    @Override
    public void updateActivityInstancesProcessDefinitionId(String newProcessDefinitionId, String processInstanceId) {
        ActivityInstanceQueryImpl activityQuery = new ActivityInstanceQueryImpl();
        activityQuery.processInstanceId(processInstanceId);
        List<ActivityInstance> activities = getActivityInstanceEntityManager().findActivityInstancesByQueryCriteria(activityQuery);
        if (activities != null) {
            for (ActivityInstance activityInstance : activities) {
                ActivityInstanceEntity activityEntity = (ActivityInstanceEntity) activityInstance;
                activityEntity.setProcessDefinitionId(newProcessDefinitionId);
                getActivityInstanceEntityManager().update(activityEntity);
            }
        }
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

    protected ActivityInstanceEntity recordActivityTaskInfoChange(TaskEntity taskEntity) {
        ActivityInstanceEntity activityInstance = null;
        ExecutionEntity executionEntity = getExecutionEntityManager().findById(taskEntity.getExecutionId());
        if (executionEntity != null) {
            if (!Objects.equals(getOriginalAssignee(taskEntity), taskEntity.getAssignee())) {
                activityInstance = findUnfinishedActivityInstance(executionEntity);
                if (activityInstance == null) {
                    HistoricActivityInstanceEntity historicActivityInstance = getHistoryManager().findHistoricActivityInstance(executionEntity, true);
                    if (historicActivityInstance != null) {
                        activityInstance = createActivityInstance(historicActivityInstance);
                    }
                }
                if (activityInstance != null) {
                    activityInstance.setAssignee(taskEntity.getAssignee());
                    getHistoryManager().updateHistoricActivityInstance(activityInstance);
                }
            }
        }

        return activityInstance;
    }

    protected Object getOriginalAssignee(TaskEntity taskEntity) {
        return ((Map<String, Object>) taskEntity.getOriginalPersistentState()).get("assignee");
    }

    protected ActivityInstance recordRuntimeActivityStart(ExecutionEntity executionEntity) {
        ActivityInstance activityInstance = null;
        if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {
            activityInstance = findUnfinishedActivityInstance(executionEntity);
            if (activityInstance == null) {
                return createActivityInstance(executionEntity);
            }
        }

        return null;
    }

    protected ActivityInstance createActivityInstance(ExecutionEntity executionEntity) {
        ActivityInstance activityInstance = null;
        if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {

            // activity instance could have been created (but only in cache, never persisted)
            // for example when submitting form properties
            activityInstance = getActivityInstanceFromCache(executionEntity.getId(), executionEntity.getActivityId(), true);
            if (activityInstance  == null) {
                activityInstance = createActivityInstanceEntity(executionEntity);
            } else {
                // activityInstance is not null only on its creation time
                activityInstance = null;
            }
        }
        return activityInstance;
    }

    protected ActivityInstance recordActivityInstanceEnd(ExecutionEntity executionEntity, String deleteReason) {
        ActivityInstanceEntity activityInstance = findUnfinishedActivityInstance(executionEntity);
        if (activityInstance != null) {
            activityInstance.markEnded(deleteReason);
        } else {
            // in the case of upgrade from 6.4.1.1 to 6.4.1.2 we have to create activityInstance for all already unfinished historicActivities
            // which are going to be ended
            HistoricActivityInstanceEntity historicActivityInstance = getHistoryManager().findHistoricActivityInstance(executionEntity, true);
            if (historicActivityInstance != null) {
                activityInstance = createActivityInstance(historicActivityInstance);
                activityInstance.markEnded(deleteReason);
            }
        }
        return activityInstance;
    }

    public ActivityInstanceDataManager getActivityInstanceDataManager() {
        return activityInstanceDataManager;
    }

    public void setActivityInstanceDataManager(ActivityInstanceDataManager activityInstanceDataManager) {
        this.activityInstanceDataManager = activityInstanceDataManager;
    }

    @Override
    public ActivityInstanceEntity findUnfinishedActivityInstance(ExecutionEntity execution) {
        String activityId = getActivityIdForExecution(execution);
        if (activityId != null) {
            // No use looking for the ActivityInstance when no activityId is provided.

            String executionId = execution.getId();

            // Check the cache
            ActivityInstanceEntity activityInstanceFromCache = getActivityInstanceFromCache(executionId, activityId, true);
            if (activityInstanceFromCache != null) {
                return activityInstanceFromCache;
            }

            // If the execution was freshly created, there is no need to check the database,
            // there can never be an entry for a activity instance with this execution id.
            if (!execution.isInserted() && !execution.isProcessInstanceType()) {

                // Check the database
                List<ActivityInstanceEntity> activityInstances = findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
                // cache can be updated before DB
                ActivityInstanceEntity activityFromCache = getActivityInstanceFromCache(executionId, activityId, false);
                if (activityFromCache != null && activityFromCache.getEndTime() != null) {
                    activityInstances.remove(activityFromCache);
                }
                if (activityInstances.size() > 0) {
                    return activityInstances.get(0);
                }

            }
        }

        return null;
    }

    public ActivityInstanceEntity findUnfinishedFlowSequenceActivityInstance(ExecutionEntity execution) {
        String activityId = execution.getActivityId();
        if (activityId != null) {
            // No use looking for the ActivityInstance when no activityId is provided.

            String executionId = execution.getId();

            // Check the cache
            ActivityInstanceEntity activityInstanceFromCache = getActivityInstanceFromCache(executionId, activityId, true);
            if (activityInstanceFromCache != null) {
                return activityInstanceFromCache;
            }

            // If the execution was freshly created, there is no need to check the database,
            // there can never be an entry for a activity instance with this execution id.
            if (!execution.isInserted() && !execution.isProcessInstanceType()) {

                // Check the database
                List<ActivityInstanceEntity> activityInstances = findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
                // cache can be updated before DB
                ActivityInstanceEntity activityFromCache = getActivityInstanceFromCache(executionId, activityId, false);
                if (activityFromCache != null && activityFromCache.getEndTime() != null) {
                    activityInstances.remove(activityFromCache);
                }
                if (activityInstances.size() > 0) {
                    return activityInstances.get(0);
                }

            }
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

    protected ActivityInstanceEntity createActivityInstance(HistoricActivityInstance historicActivityInstance) {
        ActivityInstanceEntity activityInstanceEntity = create();
        activityInstanceEntity.setId(historicActivityInstance.getId());

        activityInstanceEntity.setProcessDefinitionId(historicActivityInstance.getProcessDefinitionId());
        activityInstanceEntity.setProcessInstanceId(historicActivityInstance.getProcessInstanceId());
        activityInstanceEntity.setCalledProcessInstanceId(historicActivityInstance.getCalledProcessInstanceId());
        activityInstanceEntity.setExecutionId(historicActivityInstance.getExecutionId());
        activityInstanceEntity.setTaskId(historicActivityInstance.getTaskId());
        activityInstanceEntity.setActivityId(historicActivityInstance.getActivityId());
        activityInstanceEntity.setActivityName(historicActivityInstance.getActivityName());
        activityInstanceEntity.setActivityType(historicActivityInstance.getActivityType());
        activityInstanceEntity.setAssignee(historicActivityInstance.getAssignee());
        activityInstanceEntity.setStartTime(historicActivityInstance.getStartTime());
        activityInstanceEntity.setEndTime(historicActivityInstance.getEndTime());
        activityInstanceEntity.setDeleteReason(historicActivityInstance.getDeleteReason());
        activityInstanceEntity.setDurationInMillis(historicActivityInstance.getDurationInMillis());
        activityInstanceEntity.setTenantId(historicActivityInstance.getTenantId());

        insert(activityInstanceEntity);
        return activityInstanceEntity;
    }

}
