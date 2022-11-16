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
package org.flowable.engine.data.inmemory.impl.task;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

/**
 * In memory implementation of
 * {@link org.flowable.task.service.impl.persistence.entity.data.TaskDataManager}
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryTaskDataManager extends AbstractMemoryDataManager<TaskEntity> implements TaskDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryTaskDataManager.class);

    private final ProcessEngineConfigurationImpl processEngineConfiguration;

    public MemoryTaskDataManager(MapProvider mapProvider, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration.getTaskServiceConfiguration().getIdGenerator());
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public TaskEntity create() {
        return new TaskEntityImpl();
    }

    @Override
    public void insert(TaskEntity entity) {
        doInsert(entity);
    }

    @Override
    public TaskEntity update(TaskEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public TaskEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksByExecutionId {}", executionId);
        }

        return getData().values().stream().filter(item -> item.getExecutionId() != null && item.getExecutionId().equals(executionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksByProcessInstanceId {}", processInstanceId);
        }

        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<TaskEntity> findTasksByScopeIdAndScopeType(String scopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksByScopeIdAndScopeType {} {}", scopeId, scopeType);
        }

        return getData().values().stream().filter(item -> item.getScopeId() != null && item.getScopeId().equals(scopeId) && item.getScopeType() != null
                        && item.getScopeType().equals(scopeType)).collect(Collectors.toList());
    }

    @Override
    public List<TaskEntity> findTasksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksBySubScopeIdAndScopeType {} {}", subScopeId, scopeType);
        }

        return getData().values().stream().filter(item -> item.getSubScopeId() != null && item.getSubScopeId().equals(subScopeId) && item.getScopeType() != null
                        && item.getScopeType().equals(scopeType)).collect(Collectors.toList());
    }

    @Override
    public List<Task> findTasksByParentTaskId(String parentTaskId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksByParentTaskId {}", parentTaskId);
        }

        return getData().values().stream().filter(item -> item.getParentTaskId() != null && item.getParentTaskId().equals(parentTaskId))
                        .collect(Collectors.toList());
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTaskCountByQueryCriteria {}", taskQuery);
        }
        return findTasksByQueryCriteria(taskQuery).size();
    }

    @Override
    public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksByQueryCriteria {}", taskQuery);
        }

        return sortAndLimit(getData().values().stream().filter(item -> {
            boolean matchesQuery = filterTask(item, taskQuery, false);
            if (!matchesQuery) {
                return false;
            }
            if (taskQuery.getOrQueryObjects() != null && !taskQuery.getOrQueryObjects().isEmpty()) {
                // Nested OR query objects (in reality only one can exist)
                if (taskQuery.getOrQueryObjects().stream().noneMatch(nestedQuery -> filterTask(item, nestedQuery, true))) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList()), taskQuery);
    }

    @Override
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findTasksWithRelatedEntitiesByQueryCriteria {}", taskQuery);
        }
        if (taskQuery == null) {
            return Collections.emptyList();
        }

        List<Task> r = findTasksByQueryCriteria(taskQuery);
        return r.stream().filter(item -> (item instanceof TaskEntityImpl)).map(item -> {
            TaskEntityImpl entity = (TaskEntityImpl) item;

            if (!taskQuery.isIncludeTaskLocalVariables() && !taskQuery.isIncludeProcessVariables() && !taskQuery.isIncludeCaseVariables()
                            && !taskQuery.isIncludeIdentityLinks()) {
                return entity;
            }

            TaskEntityImpl result = new TaskEntityImpl();

            BeanUtils.copyProperties(entity, result, "queryVariables", "queryIdentityLinks", "variableInstances", "usedVariablesCache", "transientVariables",
                            "cachedElContext", "originalPersistentState");

            if (taskQuery.isIncludeIdentityLinks()) {
                List<IdentityLinkEntity> links = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                                .findIdentityLinksByTaskId(item.getId());
                result.setQueryIdentityLinks(links);
            }

            List<VariableInstanceEntity> variables = null;
            if (taskQuery.isIncludeTaskLocalVariables()) {
                variables = processEngineConfiguration.getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery()
                                .taskId(item.getId()).list();
            } else if (taskQuery.isIncludeProcessVariables()) {
                variables = processEngineConfiguration.getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery()
                                .executionId(item.getProcessInstanceId()).list();
            } else if (taskQuery.isIncludeCaseVariables()) {
                variables = processEngineConfiguration.getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery()
                                .withoutTaskId().scopeId(item.getId()).scopeType("cmmn").list();
            }
            result.setQueryVariables(variables);
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this TaskDataManager implementation!");
    }

    @Override
    public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this TaskDataManager implementation!");
    }

    @Override
    public void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId) {
        List<ProcessDefinition> definitions = processEngineConfiguration.getRepositoryService().createProcessDefinitionQuery().deploymentId(deploymentId)
                        .list();
        getData().values().stream().filter(item -> definitions.stream().anyMatch(def -> def.getId().equals(item.getProcessDefinitionId()))).forEach(item -> {
            item.setTenantId(newTenantId);
        });
    }

    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean newValue) {
        getData().values().stream().filter(item -> (item instanceof TaskEntityImpl)).map(item -> (TaskEntityImpl) item)
                        .forEach(item -> item.setIsCountEnabled(newValue));
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(TaskEntity entity) {
        doDelete(entity);
    }

    @Override
    public void deleteTasksByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteTasksByExecutionId {}", executionId);
        }

        getData().entrySet().removeIf(item -> item.getValue().getExecutionId() != null && item.getValue().getExecutionId().equals(executionId));
    }

    private boolean filterTask(TaskEntity item, TaskQueryImpl query, boolean isOrQuery) {
        Boolean retVal = null; // Used to keep track of true/false return for
                               // queries

        if (query.getTaskId() != null) {
            retVal = QueryUtil.matchReturn(query.getTaskId().equals(item.getId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }
        
        if (query.getTaskIds() != null && !query.getTaskIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getTaskIds().stream().anyMatch(tid -> tid.equals(item.getId())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getName() != null) {
            retVal = QueryUtil.matchReturn(query.getName().equals(item.getName()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getNameLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getNameLike(), item.getName()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getNameLikeIgnoreCase() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLikeCaseInsensitive(query.getNameLikeIgnoreCase(), item.getName()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getNameList() != null && !query.getNameList().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getNameList().stream().anyMatch(name -> name.equals(item.getName())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getNameListIgnoreCase() != null && !query.getNameListIgnoreCase().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getNameListIgnoreCase().stream()
                            .anyMatch(name -> item.getName() != null && name.toLowerCase().equals(item.getName().toLowerCase())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDescription() != null) {
            retVal = QueryUtil.matchReturn(query.getDescription().equals(item.getDescription()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDescriptionLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getDescription(), item.getDescription()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDescriptionLikeIgnoreCase() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLikeCaseInsensitive(query.getDescriptionLikeIgnoreCase(), item.getDescription()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getPriority() != null) {
            retVal = QueryUtil.matchReturn(query.getPriority().equals(item.getPriority()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getMinPriority() != null) {
            retVal = QueryUtil.matchReturn(item.getPriority() > query.getPriority(), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.getMaxPriority() != null) {
            retVal = QueryUtil.matchReturn(item.getPriority() < query.getPriority(), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getAssignee() != null) {
            retVal = QueryUtil.matchReturn(query.getAssignee().equals(item.getAssignee()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getAssigneeLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getAssigneeLike(), item.getAssignee()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getAssigneeLikeIgnoreCase() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLikeCaseInsensitive(query.getAssigneeLikeIgnoreCase(), item.getAssignee()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getAssigneeIds() != null && !query.getAssigneeIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getAssigneeIds().stream().anyMatch(assignee -> assignee.equals(item.getAssignee())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getOwner() != null) {
            retVal = QueryUtil.matchReturn(query.getOwner().equals(item.getOwner()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getOwnerLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getOwnerLike(), item.getOwner()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getOwnerLikeIgnoreCase() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLikeCaseInsensitive(query.getOwnerLikeIgnoreCase(), item.getOwner()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isUnassigned()) {
            retVal = QueryUtil.matchReturn(item.getAssignee() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithAssignee()) {
            retVal = QueryUtil.matchReturn(item.getAssignee() != null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isNoDelegationState()) {
            retVal = QueryUtil.matchReturn(item.getDelegationState() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDelegationState() != null) {
            retVal = QueryUtil.matchReturn(query.getDelegationState().equals(item.getDelegationState()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceId().equals(item.getProcessInstanceId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceIds() != null && !query.getProcessInstanceIds().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceIds().stream().anyMatch(pid -> pid.equals(item.getProcessInstanceId())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutProcessInstanceId()) {
            retVal = QueryUtil.matchReturn(item.getProcessInstanceId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getExecutionId() != null) {
            retVal = QueryUtil.matchReturn(query.getExecutionId().equals(item.getExecutionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeId() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeId().equals(item.getScopeId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getSubScopeId() != null) {
            retVal = QueryUtil.matchReturn(query.getSubScopeId().equals(item.getSubScopeId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeType() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeType().equals(item.getScopeType()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getScopeDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getScopeDefinitionId().equals(item.getScopeDefinitionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutScopeId()) {
            retVal = QueryUtil.matchReturn(item.getScopeId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getPropagatedStageInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getPropagatedStageInstanceId().equals(item.getPropagatedStageInstanceId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCreateTime() != null) {
            retVal = QueryUtil.matchReturn(item.getCreateTime() == null ? false : item.getCreateTime().equals(query.getCreateTime()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCreateTimeAfter() != null) {
            retVal = QueryUtil.matchReturn(item.getCreateTime() == null ? false : item.getCreateTime().after(query.getCreateTimeAfter()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCreateTimeBefore() != null) {
            retVal = QueryUtil.matchReturn(item.getCreateTime() == null ? false : item.getCreateTime().before(query.getCreateTimeBefore()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getKey() != null) {
            retVal = QueryUtil.matchReturn(query.getKey().equals(item.getTaskDefinitionKey()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getKeyLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getKeyLike(), item.getTaskDefinitionKey()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getKeys() != null && !query.getKeys().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getKeys().stream().anyMatch(k -> k.equals(item.getTaskDefinitionKey())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionId().equals(item.getProcessDefinitionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTaskDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getTaskDefinitionId().equals(item.getTaskDefinitionId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getDueDate() != null || query.getDueBefore() != null || query.getDueAfter() != null) {
            retVal = QueryUtil.matchReturn(item.getDueDate() != null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }

            if (query.getDueDate() != null) {
                retVal = QueryUtil.matchReturn(query.getDueDate().equals(item.getDueDate()), isOrQuery);
                if (retVal != null) {
                    return retVal;
                }
            }

            if (query.getDueBefore() != null) {
                retVal = QueryUtil.matchReturn(item.getDueDate() == null ? false : item.getDueDate().before(query.getDueBefore()), isOrQuery);
                if (retVal != null) {
                    return retVal;
                }
            }

            if (query.getDueAfter() != null) {
                retVal = QueryUtil.matchReturn(item.getDueDate() == null ? false : item.getDueDate().after(query.getDueAfter()), isOrQuery);
                if (retVal != null) {
                    return retVal;
                }
            }
        }

        if (query.isWithoutDueDate()) {
            retVal = QueryUtil.matchReturn(item.getDueDate() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCategory() != null) {
            retVal = QueryUtil.matchReturn(query.getCategory().equals(item.getCategory()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCategoryInList() != null && !query.getCategoryInList().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getCategoryInList().stream().anyMatch(cat -> cat.equals(item.getCategory())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCategoryNotInList() != null && !query.getCategoryNotInList().isEmpty()) {
            retVal = QueryUtil.matchReturn(query.getCategoryNotInList().stream().noneMatch(cat -> cat.equals(item.getCategory())), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutCategory()) {
            retVal = QueryUtil.matchReturn(item.getCategory() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithFormKey()) {
            retVal = QueryUtil.matchReturn(item.getFormKey() != null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getFormKey() != null) {
            retVal = QueryUtil.matchReturn(query.getFormKey().equals(item.getFormKey()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isExcludeSubtasks()) {
            retVal = QueryUtil.matchReturn(item.getParentTaskId() == null, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getSuspensionState() != null) {
            retVal = QueryUtil.matchReturn(query.getSuspensionState().getStateCode() == item.getSuspensionState(), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantId() != null) {
            retVal = QueryUtil.matchReturn(query.getTenantId().equals(item.getTenantId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantIdLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getTenantId(), item.getTenantId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutTenantId()) {
            retVal = QueryUtil.matchReturn(StringUtils.isEmpty(item.getTenantId()), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        //
        // "Complex" query filters
        //
        if (query.getProcessInstanceIdWithChildren() != null) {
            if (processEngineConfiguration.isEnableEntityLinks()) {
                throw new IllegalStateException("This TaskDataManager does not support EntityLinks");
            }
            // never matches if entitylinks are disabled
            retVal = QueryUtil.matchReturn(false, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCaseInstanceIdWithChildren() != null) {
            if (processEngineConfiguration.isEnableEntityLinks()) {
                throw new IllegalStateException("This TaskDataManager does not support EntityLinks");
            }
            // never matches if entitylinks are disabled
            retVal = QueryUtil.matchReturn(false, isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessInstanceBusinessKey() != null || query.getProcessInstanceBusinessKeyLike() != null
                        || query.getProcessInstanceBusinessKeyLikeIgnoreCase() != null) {
            retVal = QueryUtil.matchReturn(hasRelatedExecutionByBusinessKey(query, item), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionKey() != null || query.getProcessDefinitionKeyLike() != null || query.getProcessDefinitionKeyLikeIgnoreCase() != null
                        || query.getProcessDefinitionName() != null || query.getProcessDefinitionNameLike() != null
                        || (query.getProcessCategoryInList() != null && !query.getProcessCategoryInList().isEmpty())
                        || (query.getProcessCategoryNotInList() != null && !query.getProcessCategoryNotInList().isEmpty()) || query.getDeploymentId() != null
                        || (query.getDeploymentIds() != null && !query.getDeploymentIds().isEmpty())
                        || (query.getProcessDefinitionKeys() != null && !query.getProcessDefinitionKeys().isEmpty())) {
            retVal = QueryUtil.matchReturn(hasRelatedProcessDefinition(query, item), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getCaseDefinitionKey() != null || query.getCaseDefinitionKeyLike() != null || query.getCaseDefinitionKeyLikeIgnoreCase() != null
                        || (query.getCaseDefinitionKeys() != null && !query.getCaseDefinitionKeys().isEmpty()) || query.getCmmnDeploymentId() != null
                        || (query.getCmmnDeploymentIds() != null && !query.getCmmnDeploymentIds().isEmpty())) {
            retVal = QueryUtil.matchReturn(hasRelatedCaseDefinition(query, item), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if ((query.getCandidateUser() != null || query.getCandidateGroup() != null
                        || (query.getCandidateGroups() != null && !query.getCandidateGroups().isEmpty()))) {

            retVal = filterCandidateAssigned(query, item, isOrQuery);
        }

        if (query.getInvolvedUser() != null) {
            retVal = QueryUtil.matchReturn(hasInvolvedUser(query, item), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getInvolvedGroups() != null && !query.getInvolvedGroups().isEmpty()) {
            retVal = QueryUtil.matchReturn(hasInvolvedGroup(query, item), isOrQuery);
            if (retVal != null) {
                return retVal;
            }
        }

        // Variables
        retVal = filterVariables(item, query, isOrQuery);
        if (retVal != null) {
            return retVal;
        }

        // TaskQueryImpl allows a *single* or, so if none of
        // the filters matched (that is, none returned true for an orQuery), we
        // can safely return false here as there cannot be other orQueries to
        // process.
        return isOrQuery ? false : true;
    }

    private boolean filterCandidateAssigned(TaskQueryImpl query, TaskEntity item, boolean isOrQuery) {
        boolean assigneeMatches = query.getUserIdForCandidateAndAssignee() != null ? query.getUserIdForCandidateAndAssignee().equals(item.getAssignee())
                        : false;
        boolean assigneeNull = item.getAssignee() == null;

        if (query.isBothCandidateAndAssigned()) {

            if (assigneeMatches) {
                return true;
            }

            if (query.isIgnoreAssigneeValue() && !assigneeNull) {
                return false;
            }

            // This is a total mess in 'Task.xml' and I'm almost
            // certain it does not work correctly there, this implementation
            // tries to mimic the Flowable SQL however (except for
            // ACT_ID_MEMBERSHIP data which we have no access to)
            if (query.getUserIdForCandidateAndAssignee() != null && query.getCandidateGroups() == null) {
                // This would require data from ACT_ID_MEMBERSHIP (part of
                // Flowable IDM engine) which this data manager does not
                // currently support
                throw new IllegalStateException("This DataManager does not support candidate user lookups from IDM Engine");
            }

            return processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().findIdentityLinksByTaskId(item.getId()).stream()
                            .filter(id -> id.getType() != null && id.getType().equals("candidate")).anyMatch(id -> {
                                if (query.getUserIdForCandidateAndAssignee() != null && !query.getUserIdForCandidateAndAssignee().equals(id.getUserId())) {
                                    return false;
                                }
                                if (query.getCandidateGroup() != null && !query.getCandidateGroup().equals(id.getGroupId())) {
                                    return false;
                                }
                                if (query.getCandidateGroups() != null && !query.getCandidateGroups().isEmpty()
                                                && !query.getCandidateGroups().stream().anyMatch(g -> g.equals(id.getGroupId()))) {
                                    return false;
                                }

                                return true;
                            });
        }

        if (!query.isIgnoreAssigneeValue() && assigneeNull) {
            // assignee must be null when isIgnoreAssigneeValue is not set (yes,
            // it is this way in the
            // Task.xml as well)
            return false;
        }

        return hasCandidateUserOrGroup(query, item);
    }

    private Boolean filterVariables(TaskEntity entity, TaskQueryImpl query, boolean isOrQuery) {
        Boolean retVal = null;

        if (query.getQueryVariableValues() == null || query.getQueryVariableValues().isEmpty()) {
            // No variable filters
            return retVal;
        }

        List<VariableInstanceEntity> taskVariables, processVariables, scopeVariables;
        if (query.getQueryVariableValues().stream().anyMatch(v -> v.isLocal())) {
            taskVariables = processEngineConfiguration.getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery()
                            .taskId(entity.getId()).list();
        } else {
            taskVariables = Collections.emptyList();
        }

        if (query.getQueryVariableValues().stream().anyMatch(v -> v.getScopeType() == null || v.getScopeType().equals(ScopeTypes.BPMN))) {
            processVariables = processEngineConfiguration.getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery()
                            .processInstanceId(entity.getProcessInstanceId()).list();
        } else {
            processVariables = Collections.emptyList();
        }

        if (query.getQueryVariableValues().stream().anyMatch(v -> v.getScopeType() != null && entity.getScopeId() != null)) {
            scopeVariables = processEngineConfiguration.getVariableServiceConfiguration().getVariableService().createInternalVariableInstanceQuery()
                            .scopeId(entity.getScopeId()).list();
        } else {
            scopeVariables = null;
        }

        return QueryUtil.filterVariables(query.getQueryVariableValues(), taskVariables, scopeVariables, processVariables, isOrQuery);
    }

    private boolean hasInvolvedUser(TaskQueryImpl query, TaskEntity item) {
        if (query.getInvolvedUser().equals(item.getAssignee())) {
            return true;
        }
        if (query.getInvolvedUser().equals(item.getOwner())) {
            return true;
        }
        return processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().findIdentityLinksByTaskId(item.getId()).stream()
                        .filter(id -> id.getType() != null && id.getType().equals("candidate")).anyMatch(id -> query.getInvolvedUser().equals(id.getUserId()));
    }

    private boolean hasInvolvedGroup(TaskQueryImpl query, TaskEntity item) {
        return processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().findIdentityLinksByTaskId(item.getId()).stream()
                        .filter(id -> id.getType() != null && id.getType().equals("candidate")).anyMatch(id -> {
                            return query.getInvolvedGroups().stream().anyMatch(g -> g.equals(id.getGroupId()));
                        });
    }

    private boolean hasCandidateUserOrGroup(TaskQueryImpl query, TaskEntity item) {
        return processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().findIdentityLinksByTaskId(item.getId()).stream()
                        .filter(id -> id.getType() != null && id.getType().equals("candidate")).anyMatch(id -> {
                            if (query.getCandidateUser() != null && !query.getCandidateUser().equals(id.getUserId())) {
                                return false;
                            }
                            if (query.getCandidateGroup() != null && !query.getCandidateGroup().equals(id.getGroupId())) {
                                return false;
                            }
                            if (query.getCandidateGroups() != null && !query.getCandidateGroups().isEmpty()
                                            && !query.getCandidateGroups().stream().anyMatch(g -> g.equals(id.getGroupId()))) {
                                return false;
                            }

                            return true;
                        });
    }

    private boolean hasRelatedProcessDefinition(TaskQueryImpl query, TaskEntity item) {
        return processEngineConfiguration.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(item.getProcessDefinitionId()).list()
                        .stream().anyMatch(definition -> {
                            if (!(definition instanceof ProcessDefinitionEntityImpl)) {
                                return false;
                            }
                            ProcessDefinitionEntityImpl d = (ProcessDefinitionEntityImpl) definition;
                            if (query.getProcessDefinitionKey() != null && !query.getProcessDefinitionKey().equals(d.getKey())) {
                                return false;
                            }

                            if (query.getProcessDefinitionKeyLike() != null && !QueryUtil.queryLike(query.getProcessDefinitionKeyLike(), d.getKey())) {
                                return false;
                            }

                            if (query.getProcessDefinitionKeyLikeIgnoreCase() != null
                                            && !QueryUtil.queryLike(query.getProcessDefinitionKeyLikeIgnoreCase(), d.getKey())) {
                                return false;
                            }
                            if (query.getProcessDefinitionName() != null && !query.getProcessDefinitionName().equals(d.getName())) {
                                return false;
                            }

                            if (query.getProcessDefinitionNameLike() != null && !QueryUtil.queryLike(query.getProcessDefinitionNameLike(), d.getName())) {
                                return false;
                            }

                            if (query.getProcessCategoryInList() != null && !query.getProcessCategoryInList().isEmpty()
                                            && !query.getProcessCategoryInList().stream().anyMatch(cat -> cat.equals(d.getCategory()))) {
                                return false;
                            }

                            if (query.getProcessCategoryNotInList() != null && !query.getProcessCategoryNotInList().isEmpty()
                                            && query.getProcessCategoryNotInList().stream().anyMatch(cat -> cat.equals(d.getCategory()))) {
                                return false;
                            }

                            if (query.getDeploymentId() != null && !query.getDeploymentId().equals(d.getDeploymentId())) {
                                return false;
                            }

                            if (query.getDeploymentIds() != null && !query.getDeploymentIds().isEmpty()
                                            && !query.getDeploymentIds().stream().anyMatch(id -> id.equals(d.getDeploymentId()))) {
                                return false;
                            }

                            return true;
                        });
    }

    private boolean hasRelatedCaseDefinition(TaskQueryImpl query, TaskEntity item) {
        // Unsupported for now
        throw new IllegalStateException("This TaskDataManager does not support Case (CMMN) definitions");
    }

    private boolean hasRelatedExecutionByBusinessKey(TaskQueryImpl query, TaskEntity item) {
        return processEngineConfiguration.getRuntimeService().createExecutionQuery().executionId(item.getProcessInstanceId()).list().stream()
                        .anyMatch(execution -> {
                            if (!(execution instanceof ExecutionEntityImpl)) {
                                return false;
                            }
                            ExecutionEntityImpl e = (ExecutionEntityImpl) execution;
                            if (query.getProcessInstanceBusinessKey() != null && !query.getProcessInstanceBusinessKey().equals(e.getBusinessKey())) {
                                return false;
                            }

                            if (query.getProcessInstanceBusinessKeyLike() != null
                                            && !QueryUtil.queryLike(query.getProcessInstanceBusinessKeyLike(), e.getProcessInstanceBusinessKey())) {
                                return false;
                            }

                            if (query.getProcessInstanceBusinessKeyLikeIgnoreCase() != null && !QueryUtil
                                            .queryLikeCaseInsensitive(query.getProcessInstanceBusinessKeyLikeIgnoreCase(), e.getProcessInstanceBusinessKey())) {
                                return false;
                            }

                            return true;
                        });

    }

    private List<Task> sortAndLimit(List<Task> collect, TaskQueryImpl query) {

        if (collect == null || collect.isEmpty()) {
            return collect;
        }

        return sortAndPaginate(collect, TaskComparator.resolve(query.getOrderBy()), query.getFirstResult(), query.getMaxResults());
    }
}
