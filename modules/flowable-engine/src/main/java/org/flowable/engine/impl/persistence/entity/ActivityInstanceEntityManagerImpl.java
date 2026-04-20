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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author martin.grofcik
 */
public class ActivityInstanceEntityManagerImpl
    extends AbstractProcessEngineEntityManager<ActivityInstanceEntity, ActivityInstanceDataManager>
    implements ActivityInstanceEntityManager {

    protected static final String NO_ACTIVITY_ID_PREFIX = "_flow_";
    protected static final String NO_ACTIVITY_ID_SEPARATOR = "__";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final boolean usePrefixId;

    public ActivityInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ActivityInstanceDataManager activityInstanceDataManager) {
        super(processEngineConfiguration, activityInstanceDataManager);
        this.usePrefixId = processEngineConfiguration.isUsePrefixId();
    }

    protected List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return dataManager.findUnfinishedActivityInstancesByExecutionAndActivityId(executionId, activityId);
    }
    
    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        return dataManager.findActivityInstancesByExecutionIdAndActivityId(executionId, activityId);
    }
    
    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByProcessInstanceId(String processInstanceId, boolean includeDeleted) {
        return dataManager.findActivityInstancesByProcessInstanceId(processInstanceId, includeDeleted);
    }

    @Override
    public ActivityInstanceEntity findActivityInstanceByTaskId(String taskId) {
        return dataManager.findActivityInstanceByTaskId(taskId);
    }

    @Override
    public void deleteActivityInstancesByProcessInstanceId(String processInstanceId) {
        dataManager.deleteActivityInstancesByProcessInstanceId(processInstanceId);
    }

    @Override
    public long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return dataManager.findActivityInstanceCountByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return dataManager.findActivityInstancesByQueryCriteria(historicActivityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findActivityInstancesByNativeQuery(parameterMap);
    }

    @Override
    public long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findActivityInstanceCountByNativeQuery(parameterMap);
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

        // If the activity instance is null, this means that no runtime activity instance nor historic activity instance exists.
        // In the DefaultHistoryManager implementation, the end is ignored (which it needs to be, this is for example when an execution
        // has reached a certain step, but hasn't gone into the behavior yet. When the execution is deleted, this method is called,
        // but no historic activity instance should be created for this use case).
        // However, in the async history manager, this leads to the creation of an activity-end history job.
        // To have this consistent, the recordActivityEnd is thus only called when there is a runtime activity available.

        if (activityInstance != null) {
            getHistoryManager().recordActivityEnd(activityInstance);
        }
    }

    @Override
    public void recordSequenceFlowTaken(ExecutionEntity executionEntity) {
        ActivityInstanceEntity activityInstance = createActivityInstanceEntity(executionEntity);
        activityInstance.setDurationInMillis(0L);
        activityInstance.setEndTime(activityInstance.getStartTime());
        getHistoryManager().createHistoricActivityInstance(activityInstance);
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
    public void recordActivityTaskEnd(TaskEntity task, ExecutionEntity execution, String completerUserId, String deleteReason, Date endTime) {
        if (execution != null) { // only when there's an execution, there will be an activity instance. Otherwise, it's a standalone task.
            ActivityInstanceEntity activityInstanceEntity = internalFindOrCreateActivityInstance(task, execution);
            if (activityInstanceEntity != null) {
                if (StringUtils.isNotEmpty(completerUserId)) {
                    activityInstanceEntity.setCompletedBy(completerUserId);
                } else {
                    activityInstanceEntity.setCompletedBy(task.getAssignee());
                }

                getHistoryManager().updateHistoricActivityInstance(activityInstanceEntity);
            }
        }

        getHistoryManager().recordTaskEnd(task, execution, completerUserId, deleteReason, endTime);
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity, Date changeTime) {
        ActivityInstanceEntity activityInstanceEntity = recordActivityTaskInfoChange(taskEntity);
        getHistoryManager().recordTaskInfoChange(taskEntity, activityInstanceEntity != null ? activityInstanceEntity.getId() : null, changeTime);
    }

    @Override
    public void syncUserTaskExecution(ExecutionEntity executionEntity, FlowElement newFlowElement, String oldActivityId, TaskEntity task) {
        syncUserTaskExecutionActivityInstance(executionEntity, oldActivityId, newFlowElement);
        getHistoryManager().updateActivity(executionEntity, oldActivityId, newFlowElement, task, getClock().getCurrentTime());
    }

    @Override
    public void updateActivityInstancesProcessDefinitionId(String newProcessDefinitionId, String processInstanceId) {
        ActivityInstanceQueryImpl activityQuery = new ActivityInstanceQueryImpl();
        activityQuery.processInstanceId(processInstanceId);
        List<ActivityInstance> activities = findActivityInstancesByQueryCriteria(activityQuery);
        if (activities != null) {
            for (ActivityInstance activityInstance : activities) {
                ActivityInstanceEntity activityEntity = (ActivityInstanceEntity) activityInstance;
                activityEntity.setProcessDefinitionId(newProcessDefinitionId);
                update(activityEntity);
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
                activityInstance = internalFindOrCreateActivityInstance(taskEntity, executionEntity);
                if (activityInstance != null) {
                    activityInstance.setAssignee(taskEntity.getAssignee());
                    getHistoryManager().updateHistoricActivityInstance(activityInstance);
                }
            }
        }

        return activityInstance;
    }

    protected ActivityInstanceEntity internalFindOrCreateActivityInstance(TaskEntity taskEntity, ExecutionEntity executionEntity) {
        ActivityInstanceEntity activityInstance = findActivityInstanceByTaskId(taskEntity.getId());
        if (activityInstance == null) {
            HistoricActivityInstanceEntity historicActivityInstance = getHistoryManager().findHistoricActivityInstance(executionEntity, true);
            if (historicActivityInstance != null) {
                activityInstance = createActivityInstance(historicActivityInstance);
            }
        }
        return activityInstance;
    }

    @SuppressWarnings("unchecked")
    protected Object getOriginalAssignee(TaskEntity taskEntity) {
        if (taskEntity.getOriginalPersistentState() != null) {
            return ((Map<String, Object>) taskEntity.getOriginalPersistentState()).get("assignee");
        } else {
            return null;
        }
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

        // It is possible that we record the activity instance end twice,
        // which could lead to having no finished activity instance in the DB.
        // if there is a finished runtime one, we should use the runtime one.
        //
        // It is also OK for pre 6.4.1.2 activity instances (when the runtime activities were added).
        // Since the first time we go through here we won't find anything in the cache and the DB,
        // so we will create one from the history (this will add one to the cache).
        //
        // The second time we go through here, there will be nothing in the DB, but one finished one in the cache.
        // This one should be used, in order to avoid going to the historic tables again.

        ActivityInstanceEntity activityInstance = findUnfinishedActivityInstance(executionEntity, true);
        if (activityInstance != null) {
            if (activityInstance.getEndTime() == null) {
                activityInstance.markEnded(deleteReason);
            }

        } else {
            // in the case of upgrade from 6.4.1.1 to 6.4.1.2 we have to create the runtime activityInstance (when a matching historicActivityInstance is found)
            HistoricActivityInstanceEntity historicActivityInstance = getHistoryManager().findHistoricActivityInstance(executionEntity, true);
            if (historicActivityInstance != null) {
                activityInstance = createActivityInstance(historicActivityInstance);
                activityInstance.markEnded(deleteReason);
            }

        }

        return activityInstance;
    }

    @Override
    public ActivityInstanceEntity findUnfinishedActivityInstance(ExecutionEntity execution) {
        return findUnfinishedActivityInstance(execution, false);
    }

    protected ActivityInstanceEntity findUnfinishedActivityInstance(ExecutionEntity execution, boolean returnNotFinishedFromCacheIfNothingInDb) {
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
                } else if (returnNotFinishedFromCacheIfNothingInDb) {
                    return activityFromCache;
                }

            }
        }

        return null;
    }

    protected ActivityInstanceEntity createActivityInstanceEntity(ExecutionEntity execution) {
        IdGenerator idGenerator = engineConfiguration.getIdGenerator();

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
        if (execution.getActivityId() != null ) {
            activityInstanceEntity.setActivityId(execution.getActivityId());
        } else {
            // sequence flow activity id can be null
            if (execution.getCurrentFlowElement() instanceof SequenceFlow currentFlowElement) {
                activityInstanceEntity.setActivityId(getArtificialSequenceFlowId(currentFlowElement));
            }
        }

        if (execution.getCurrentFlowElement() != null) {
            String currentFlowName = execution.getCurrentFlowElement().getName();
            if (StringUtils.isNotEmpty(currentFlowName) && (currentFlowName.contains("${") || currentFlowName.contains("#{"))) {
                Expression activityNameExpression = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager()
                        .createExpression(currentFlowName);

                String nameValue = null;
                try {
                    Object expressionValue = activityNameExpression.getValue(execution);
                    if (expressionValue != null) {
                        nameValue = expressionValue.toString();
                    }
                } catch (FlowableException e) {
                    nameValue = currentFlowName;
                    logger.warn("property not found in task name expression {} for execution {}", e.getMessage(), execution);
                }
                if (nameValue != null) {
                    execution.setCurrentActivityName(nameValue);
                    activityInstanceEntity.setActivityName(nameValue);
                }
            } else {
                activityInstanceEntity.setActivityName(currentFlowName);
            }

            activityInstanceEntity.setActivityType(parseActivityType(execution.getCurrentFlowElement()));
        }

        Date now = getClock().getCurrentTime();
        activityInstanceEntity.setStartTime(now);
        
        activityInstanceEntity.setTransactionOrder(getTransactionOrderFromCache(processInstanceId));

        if (execution.getTenantId() != null) {
            activityInstanceEntity.setTenantId(execution.getTenantId());
        }

        insert(activityInstanceEntity);
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
    
    protected int getTransactionOrderFromCache(String processInstanceId) {
        int transactionOrder = 1;
        List<ActivityInstanceEntity> cachedActivityInstances = getEntityCache().findInCache(ActivityInstanceEntity.class);
        for (ActivityInstanceEntity cachedActivityInstance : cachedActivityInstances) {
            if (processInstanceId.equals(cachedActivityInstance.getProcessInstanceId())) {
                
                if (cachedActivityInstance.isInserted() && cachedActivityInstance.getTransactionOrder() != null && 
                        cachedActivityInstance.getTransactionOrder() >= transactionOrder) {
                    
                    transactionOrder = cachedActivityInstance.getTransactionOrder() + 1;
                }
            }
        }

        return transactionOrder;
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
        activityInstanceEntity.setTransactionOrder(historicActivityInstance.getTransactionOrder());
        activityInstanceEntity.setDeleteReason(historicActivityInstance.getDeleteReason());
        activityInstanceEntity.setDurationInMillis(historicActivityInstance.getDurationInMillis());
        activityInstanceEntity.setTenantId(historicActivityInstance.getTenantId());

        insert(activityInstanceEntity);
        return activityInstanceEntity;
    }

    protected String getArtificialSequenceFlowId(SequenceFlow sequenceFlow) {
        return NO_ACTIVITY_ID_PREFIX + sequenceFlow.getSourceRef() + NO_ACTIVITY_ID_SEPARATOR + sequenceFlow.getTargetRef();
    }

    protected HistoryManager getHistoryManager() {
        return engineConfiguration.getHistoryManager();
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return engineConfiguration.getExecutionEntityManager();
    }

}
