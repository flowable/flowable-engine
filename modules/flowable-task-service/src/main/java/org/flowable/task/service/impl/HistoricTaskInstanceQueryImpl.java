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

package org.flowable.task.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.util.TaskVariableUtils;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;
import org.flowable.variable.service.impl.QueryVariableValue;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricTaskInstanceQueryImpl extends AbstractVariableQueryImpl<HistoricTaskInstanceQuery, HistoricTaskInstance>
        implements HistoricTaskInstanceQuery, CacheAwareQuery<HistoricTaskInstanceEntity> {

    private static final long serialVersionUID = 1L;
    
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected VariableServiceConfiguration variableServiceConfiguration;
    
    protected String taskDefinitionId;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String processDefinitionKeyLike;
    protected String processDefinitionKeyLikeIgnoreCase;
    protected Collection<String> processDefinitionKeys;
    protected String processDefinitionName;
    protected String processDefinitionNameLike;
    protected Collection<String> processCategoryInList;
    protected Collection<String> processCategoryNotInList;
    protected String deploymentId;
    protected Collection<String> deploymentIds;
    protected String cmmnDeploymentId;
    protected Collection<String> cmmnDeploymentIds;
    protected String processInstanceId;
    protected Collection<String> processInstanceIds;
    protected boolean withoutProcessInstanceId;
    protected String processInstanceBusinessKey;
    protected String processInstanceBusinessKeyLike;
    protected String processInstanceBusinessKeyLikeIgnoreCase;
    protected String executionId;
    protected String scopeId;
    protected Set<String> scopeIds;
    protected List<List<String>> safeScopeIds;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected boolean withoutScopeId;
    protected String propagatedStageInstanceId;
    protected String processInstanceIdWithChildren;
    protected String caseInstanceIdWithChildren;
    protected String caseDefinitionKey;
    protected String caseDefinitionKeyLike;
    protected String caseDefinitionKeyLikeIgnoreCase;
    protected Collection<String> caseDefinitionKeys;
    protected String taskId;
    protected Collection<String> taskIds;
    protected String taskName;
    protected String taskNameLike;
    protected String taskNameLikeIgnoreCase;
    protected Collection<String> taskNameList;
    protected Collection<String> taskNameListIgnoreCase;
    protected String taskParentTaskId;
    protected String taskDescription;
    protected String taskDescriptionLike;
    protected String taskDescriptionLikeIgnoreCase;
    protected String taskDeleteReason;
    protected String taskDeleteReasonLike;
    protected String taskOwner;
    protected String taskOwnerLike;
    protected String taskOwnerLikeIgnoreCase;
    protected String taskAssignee;
    protected String taskAssigneeLike;
    protected String taskAssigneeLikeIgnoreCase;
    protected String rootScopeId;
    protected String parentScopeId;
    protected boolean withAssignee;
    protected boolean withoutAssignee;
    protected Collection<String> taskAssigneeIds;
    protected String taskDefinitionKey;
    protected String taskDefinitionKeyLike;
    protected Collection<String> taskDefinitionKeys;
    protected String candidateUser;
    protected String candidateGroup;
    protected Collection<String> candidateGroups;
    private List<List<String>> safeCandidateGroups;
    protected String involvedUser;
    protected Collection<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected boolean ignoreAssigneeValue;
    protected Integer taskPriority;
    protected Integer taskMinPriority;
    protected Integer taskMaxPriority;
    protected boolean finished;
    protected boolean unfinished;
    protected boolean processFinished;
    protected boolean processUnfinished;
    protected String state;
    protected Date createTime;
    protected Date createTimeBefore;
    protected Date createTimeAfter;
    protected Date inProgressStartTime;
    protected Date inProgressStartTimeBefore;
    protected Date inProgressStartTimeAfter;
    protected String inProgressStartedBy;
    protected Date claimTime;
    protected Date claimTimeBefore;
    protected Date claimTimeAfter;
    protected String claimedBy;
    protected Date suspendedTime;
    protected Date suspendedTimeBefore;
    protected Date suspendedTimeAfter;
    protected String suspendedBy;
    protected Date completedTime;
    protected Date completedTimeAfter;
    protected Date completedTimeBefore;
    protected String completedBy;
    protected Date inProgressStartDueDate;
    protected Date inProgressStartDueAfter;
    protected Date inProgressStartDueBefore;
    protected boolean withoutInProgressStartDueDate;
    protected Date dueDate;
    protected Date dueAfter;
    protected Date dueBefore;
    protected boolean withoutDueDate;
    protected String category;
    protected Collection<String> categoryInList;
    protected Collection<String> categoryNotInList;
    protected boolean withoutCategory;
    protected boolean withFormKey;
    protected String formKey;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean withoutDeleteReason;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected boolean includeTaskLocalVariables;
    protected boolean includeProcessVariables;
    protected boolean includeCaseVariables;
    protected boolean includeIdentityLinks;
    protected List<HistoricTaskInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected HistoricTaskInstanceQueryImpl currentOrQueryObject;

    protected boolean inOrStatement;

    public HistoricTaskInstanceQueryImpl() {
    }

    public HistoricTaskInstanceQueryImpl(CommandExecutor commandExecutor, TaskServiceConfiguration taskServiceConfiguration, 
            VariableServiceConfiguration variableServiceConfiguration) {
        
        super(commandExecutor, variableServiceConfiguration);
        this.taskServiceConfiguration = taskServiceConfiguration;
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    public HistoricTaskInstanceQueryImpl(CommandExecutor commandExecutor, String databaseType, 
            TaskServiceConfiguration taskServiceConfiguration, VariableServiceConfiguration variableServiceConfiguration) {
        
        super(commandExecutor, variableServiceConfiguration);
        this.databaseType = databaseType;
        this.taskServiceConfiguration = taskServiceConfiguration;
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();

        if (taskServiceConfiguration.getHistoricTaskQueryInterceptor() != null) {
            taskServiceConfiguration.getHistoricTaskQueryInterceptor().beforeHistoricTaskQueryExecute(this);
        }

        return taskServiceConfiguration.getHistoricTaskInstanceEntityManager().findHistoricTaskInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<HistoricTaskInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<HistoricTaskInstance> tasks = null;

        if (taskServiceConfiguration.getHistoricTaskQueryInterceptor() != null) {
            taskServiceConfiguration.getHistoricTaskQueryInterceptor()
                    .beforeHistoricTaskQueryExecute(this);
        }

        if (includeTaskLocalVariables || includeProcessVariables || includeIdentityLinks || includeCaseVariables) {
            tasks = taskServiceConfiguration.getHistoricTaskInstanceEntityManager()
                    .findHistoricTaskInstancesAndRelatedEntitiesByQueryCriteria(this);

            if (taskId != null) {
                if (includeProcessVariables ||includeCaseVariables) {
                    addCachedVariableForQueryById(commandContext, tasks, false);
                } else if (includeTaskLocalVariables) {
                    addCachedVariableForQueryById(commandContext, tasks, true);
                }
            }

        } else {
            tasks = taskServiceConfiguration.getHistoricTaskInstanceEntityManager().findHistoricTaskInstancesByQueryCriteria(this);
        }

        if (tasks != null && taskServiceConfiguration.getInternalTaskLocalizationManager() != null && taskServiceConfiguration.isEnableLocalization()) {
            for (HistoricTaskInstance task : tasks) {
                taskServiceConfiguration.getInternalTaskLocalizationManager().localize(task, locale, withLocalizationFallback);
            }
        }

        if (taskServiceConfiguration.getHistoricTaskQueryInterceptor() != null) {
            taskServiceConfiguration.getHistoricTaskQueryInterceptor().afterHistoricTaskQueryExecute(this, tasks);
        }

        return tasks;
    }

    protected void addCachedVariableForQueryById(CommandContext commandContext, List<HistoricTaskInstance> results, boolean local) {
        for (HistoricTaskInstance task : results) {
            if (Objects.equals(taskId, task.getId())) {

                EntityCache entityCache = commandContext.getSession(EntityCache.class);
                List<HistoricVariableInstanceEntity> cachedVariableEntities = entityCache.findInCache(HistoricVariableInstanceEntity.class);
                for (HistoricVariableInstanceEntity cachedVariableEntity : cachedVariableEntities) {

                    if (local) {
                        if (task.getId().equals(cachedVariableEntity.getTaskId())) {
                            ((HistoricTaskInstanceEntity) task).getQueryVariables().add(cachedVariableEntity);
                        }
                    } else if (TaskVariableUtils.doesVariableBelongToTask(cachedVariableEntity, task)) {
                        ((HistoricTaskInstanceEntity) task).getQueryVariables().add(cachedVariableEntity);
                    }
                }
            }
        }
    }

    @Override
    public void enhanceCachedValue(HistoricTaskInstanceEntity task) {

        if (includeProcessVariables && task.getProcessInstanceId() != null) {
            task.getQueryVariables()
                    .addAll(variableServiceConfiguration.getHistoricVariableInstanceEntityManager()
                            .findHistoricalVariableInstancesByProcessInstanceId(task.getProcessInstanceId()));
        } else if (includeCaseVariables && TaskVariableUtils.isCaseRelated(task)) {
            task.getQueryVariables()
                    .addAll((variableServiceConfiguration.getHistoricVariableInstanceEntityManager()
                            .findHistoricalVariableInstancesByScopeIdAndScopeType(task.getScopeId(), task.getScopeType())));
        } else if (includeTaskLocalVariables) {
            task.getQueryVariables()
                    .addAll(variableServiceConfiguration.getHistoricVariableInstanceEntityManager()
                            .findHistoricalVariableInstancesByTaskId(task.getId()));
        }
    }

    @Override
    public HistoricTaskInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl processInstanceIdIn(Collection<String> processInstanceIds) {
        if (processInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Process instance id list is null");
        }
        if (processInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Process instance id list is empty");
        }
        for (String processInstanceId : processInstanceIds) {
            if (processInstanceId == null) {
                throw new FlowableIllegalArgumentException("None of the given process instance ids can be null");
            }
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceIds = processInstanceIds;
        } else {
            this.processInstanceIds = processInstanceIds;
        }
        return this;
    }
    
    @Override
    public HistoricTaskInstanceQueryImpl withoutProcessInstanceId() {
        if (inOrStatement) {
            currentOrQueryObject.withoutProcessInstanceId = true;
        } else {
            this.withoutProcessInstanceId = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl processInstanceBusinessKey(String processInstanceBusinessKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceBusinessKey = processInstanceBusinessKey;
        } else {
            this.processInstanceBusinessKey = processInstanceBusinessKey;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl processInstanceBusinessKeyLike(String processInstanceBusinessKeyLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceBusinessKeyLike = processInstanceBusinessKeyLike;
        } else {
            this.processInstanceBusinessKeyLike = processInstanceBusinessKeyLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processInstanceBusinessKeyLikeIgnoreCase(String processInstanceBusinessKeyLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceBusinessKeyLikeIgnoreCase = processInstanceBusinessKeyLikeIgnoreCase.toLowerCase();
        } else {
            this.processInstanceBusinessKeyLikeIgnoreCase = processInstanceBusinessKeyLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl executionId(String executionId) {
        if (inOrStatement) {
            this.currentOrQueryObject.executionId = executionId;
        } else {
            this.executionId = executionId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseInstanceId(String caseInstanceId) {
        if (inOrStatement) {
            currentOrQueryObject.scopeId(caseInstanceId);
            currentOrQueryObject.scopeType(ScopeTypes.CMMN);
        } else {
            this.scopeId(caseInstanceId);
            this.scopeType(ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseDefinitionId(String caseDefinitionId) {
        if (inOrStatement) {
            currentOrQueryObject.scopeDefinitionId(caseDefinitionId);
            currentOrQueryObject.scopeType(ScopeTypes.CMMN);
        } else {
            this.scopeDefinitionId(caseDefinitionId);
            this.scopeType(ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseDefinitionKey(String caseDefinitionKey) {
        if (inOrStatement) {
            currentOrQueryObject.caseDefinitionKey = caseDefinitionKey;
        } else {
            this.caseDefinitionKey = caseDefinitionKey;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseDefinitionKeyLike(String caseDefinitionKeyLike) {
        if (inOrStatement) {
            currentOrQueryObject.caseDefinitionKeyLike = caseDefinitionKeyLike;
        } else {
            this.caseDefinitionKeyLike = caseDefinitionKeyLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase) {
        if (inOrStatement) {
            currentOrQueryObject.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
        } else {
            this.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseDefinitionKeyIn(Collection<String> caseDefinitionKeys) {
        if (inOrStatement) {
            currentOrQueryObject.caseDefinitionKeys = caseDefinitionKeys;
        } else {
            this.caseDefinitionKeys = caseDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl processInstanceIdWithChildren(String processInstanceId) {
        if (inOrStatement) {
            currentOrQueryObject.processInstanceIdWithChildren(processInstanceId);
        } else {
            this.processInstanceIdWithChildren = processInstanceId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl caseInstanceIdWithChildren(String caseInstanceId) {
        if (inOrStatement) {
            currentOrQueryObject.caseInstanceIdWithChildren(caseInstanceId);
        } else {
            this.caseInstanceIdWithChildren = caseInstanceId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl planItemInstanceId(String planItemInstanceId) {
        if (inOrStatement) {
            currentOrQueryObject.subScopeId(planItemInstanceId);
            currentOrQueryObject.scopeType(ScopeTypes.CMMN);
        } else {
            this.subScopeId(planItemInstanceId);
            this.scopeType(ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl scopeId(String scopeId) {
        if (inOrStatement) {
            currentOrQueryObject.scopeId = scopeId;
        } else {
            this.scopeId = scopeId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery scopeIds(Set<String> scopeIds) {
        if (inOrStatement) {
            currentOrQueryObject.scopeIds = scopeIds;
        } else {
            this.scopeIds = scopeIds;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl subScopeId(String subScopeId) {
        if (inOrStatement) {
            currentOrQueryObject.subScopeId = subScopeId;
        } else {
            this.subScopeId = subScopeId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl scopeType(String scopeType) {
        if (inOrStatement) {
            currentOrQueryObject.scopeType = scopeType;
        } else {
            this.scopeType = scopeType;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl scopeDefinitionId(String scopeDefinitionId) {
        if (inOrStatement) {
            currentOrQueryObject.scopeDefinitionId = scopeDefinitionId;
        } else {
            this.scopeDefinitionId = scopeDefinitionId;
        }
        return this;
    }
    
    @Override
    public HistoricTaskInstanceQueryImpl withoutScopeId() {
        if (inOrStatement) {
            currentOrQueryObject.withoutScopeId = true;
        } else {
            this.withoutScopeId = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery propagatedStageInstanceId(String propagatedStageInstanceId) {
        if (inOrStatement) {
            currentOrQueryObject.propagatedStageInstanceId = propagatedStageInstanceId;
        } else {
            this.propagatedStageInstanceId = propagatedStageInstanceId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl taskDefinitionId(String taskDefinitionId) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDefinitionId = taskDefinitionId;
        } else {
            this.taskDefinitionId = taskDefinitionId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKey(String processDefinitionKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKeyLike(String processDefinitionKeyLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeyLike = processDefinitionKeyLike;
        } else {
            this.processDefinitionKeyLike = processDefinitionKeyLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase.toLowerCase();
        } else {
            this.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKeyIn(Collection<String> processDefinitionKeys) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeys = processDefinitionKeys;
        } else {
            this.processDefinitionKeys = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionName(String processDefinitionName) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionNameLike(String processDefinitionNameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionNameLike = processDefinitionNameLike;
        } else {
            this.processDefinitionNameLike = processDefinitionNameLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processCategoryIn(Collection<String> processCategoryInList) {
        if (processCategoryInList == null) {
            throw new FlowableIllegalArgumentException("Process category list is null");
        }
        if (processCategoryInList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Process category list is empty");
        }
        for (String processCategory : processCategoryInList) {
            if (processCategory == null) {
                throw new FlowableIllegalArgumentException("None of the given process categories can be null");
            }
        }

        if (inOrStatement) {
            currentOrQueryObject.processCategoryInList = processCategoryInList;
        } else {
            this.processCategoryInList = processCategoryInList;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processCategoryNotIn(Collection<String> processCategoryNotInList) {
        if (processCategoryNotInList == null) {
            throw new FlowableIllegalArgumentException("Process category list is null");
        }
        if (processCategoryNotInList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Process category list is empty");
        }
        for (String processCategory : processCategoryNotInList) {
            if (processCategory == null) {
                throw new FlowableIllegalArgumentException("None of the given process categories can be null");
            }
        }

        if (inOrStatement) {
            currentOrQueryObject.processCategoryNotInList = processCategoryNotInList;
        } else {
            this.processCategoryNotInList = processCategoryNotInList;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery deploymentId(String deploymentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery deploymentIdIn(Collection<String> deploymentIds) {
        if (inOrStatement) {
            currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery cmmnDeploymentId(String cmmnDeploymentId) {
        if (inOrStatement) {
            currentOrQueryObject.cmmnDeploymentId = cmmnDeploymentId;
        } else {
            this.cmmnDeploymentId = cmmnDeploymentId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery cmmnDeploymentIdIn(Collection<String> cmmnDeploymentIds) {
        if (inOrStatement) {
            currentOrQueryObject.cmmnDeploymentIds = cmmnDeploymentIds;
        } else {
            this.cmmnDeploymentIds = cmmnDeploymentIds;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskId(String taskId) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskId = taskId;
        } else {
            this.taskId = taskId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskIds(Collection<String> taskIds) {
        if (taskIds == null) {
            throw new FlowableIllegalArgumentException("Task id list is null");
        }
        if (taskIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task id list is empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.taskIds = taskIds;
        } else {
            this.taskIds = taskIds;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskName(String taskName) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskName = taskName;
        } else {
            this.taskName = taskName;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameIn(Collection<String> taskNameList) {
        if (taskNameList == null) {
            throw new FlowableIllegalArgumentException("Task name list is null");
        }
        if (taskNameList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task name list is empty");
        }

        if (taskName != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameIn and taskName");
        }
        if (taskNameLike != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameIn and taskNameLike");
        }
        if (taskNameLikeIgnoreCase != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameIn and taskNameLikeIgnoreCase");
        }

        if (inOrStatement) {
            currentOrQueryObject.taskNameList = taskNameList;
        } else {
            this.taskNameList = taskNameList;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameInIgnoreCase(Collection<String> taskNameList) {
        if (taskNameList == null) {
            throw new FlowableIllegalArgumentException("Task name list is null");
        }
        if (taskNameList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task name list is empty");
        }
        for (String taskName : taskNameList) {
            if (taskName == null) {
                throw new FlowableIllegalArgumentException("None of the given task names can be null");
            }
        }

        if (taskName != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameInIgnoreCase and name");
        }
        if (taskNameLike != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameInIgnoreCase and nameLike");
        }
        if (taskNameLikeIgnoreCase != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameInIgnoreCase and nameLikeIgnoreCase");
        }

        final int nameListSize = taskNameList.size();
        final Collection<String> caseIgnoredTaskNameList = new ArrayList<>(nameListSize);
        for (String taskName : taskNameList) {
            caseIgnoredTaskNameList.add(taskName.toLowerCase());
        }

        if (inOrStatement) {
            this.currentOrQueryObject.taskNameListIgnoreCase = caseIgnoredTaskNameList;
        } else {
            this.taskNameListIgnoreCase = caseIgnoredTaskNameList;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameLike(String taskNameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskNameLike = taskNameLike;
        } else {
            this.taskNameLike = taskNameLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameLikeIgnoreCase(String taskNameLikeIgnoreCase) {
        if (taskNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Task name is null");
        }
        
        if (inOrStatement) {
            this.currentOrQueryObject.taskNameLikeIgnoreCase = taskNameLikeIgnoreCase.toLowerCase();
        } else {
            this.taskNameLikeIgnoreCase = taskNameLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskParentTaskId(String parentTaskId) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskParentTaskId = parentTaskId;
        } else {
            this.taskParentTaskId = parentTaskId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDescription(String taskDescription) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDescription = taskDescription;
        } else {
            this.taskDescription = taskDescription;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDescriptionLike(String taskDescriptionLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDescriptionLike = taskDescriptionLike;
        } else {
            this.taskDescriptionLike = taskDescriptionLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDescriptionLikeIgnoreCase(String taskDescriptionLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDescriptionLikeIgnoreCase = taskDescriptionLikeIgnoreCase.toLowerCase();
        } else {
            this.taskDescriptionLikeIgnoreCase = taskDescriptionLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDeleteReason(String taskDeleteReason) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDeleteReason = taskDeleteReason;
        } else {
            this.taskDeleteReason = taskDeleteReason;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDeleteReasonLike(String taskDeleteReasonLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDeleteReasonLike = taskDeleteReasonLike;
        } else {
            this.taskDeleteReasonLike = taskDeleteReasonLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssignee(String taskAssignee) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskAssignee = taskAssignee;
        } else {
            this.taskAssignee = taskAssignee;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigneeLike(String taskAssigneeLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskAssigneeLike = taskAssigneeLike;
        } else {
            this.taskAssigneeLike = taskAssigneeLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigneeLikeIgnoreCase(String taskAssigneeLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskAssigneeLikeIgnoreCase = taskAssigneeLikeIgnoreCase.toLowerCase();
        } else {
            this.taskAssigneeLikeIgnoreCase = taskAssigneeLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskUnassigned() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutAssignee = true;
        }
        else {
            this.withoutAssignee = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigned() {
        if (inOrStatement) {
            this.currentOrQueryObject.withAssignee = true;
        }
        else {
            this.withAssignee = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigneeIds(Collection<String> assigneeIds) {
        if (assigneeIds == null) {
            throw new FlowableIllegalArgumentException("Task assignee list is null");
        }
        if (assigneeIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task assignee list is empty");
        }
        for (String assignee : assigneeIds) {
            if (assignee == null) {
                throw new FlowableIllegalArgumentException("None of the given task assignees can be null");
            }
        }

        if (taskAssignee != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskAssigneeIds and taskAssignee");
        }
        if (taskAssigneeLike != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskAssigneeIds and taskAssigneeLike");
        }
        if (taskAssigneeLikeIgnoreCase != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskAssigneeIds and taskAssigneeLikeIgnoreCase");
        }

        if (inOrStatement) {
            currentOrQueryObject.taskAssigneeIds = assigneeIds;
        } else {
            this.taskAssigneeIds = assigneeIds;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskOwner(String taskOwner) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskOwner = taskOwner;
        } else {
            this.taskOwner = taskOwner;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskOwnerLike(String taskOwnerLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskOwnerLike = taskOwnerLike;
        } else {
            this.taskOwnerLike = taskOwnerLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskOwnerLikeIgnoreCase(String taskOwnerLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskOwnerLikeIgnoreCase = taskOwnerLikeIgnoreCase.toLowerCase();
        } else {
            this.taskOwnerLikeIgnoreCase = taskOwnerLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery finished() {
        if (inOrStatement) {
            this.currentOrQueryObject.finished = true;
        } else {
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery unfinished() {
        if (inOrStatement) {
            this.currentOrQueryObject.unfinished = true;
        } else {
            this.unfinished = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue);
            return this;
        } else {
            return variableValueEquals(variableValue);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value);
            return this;
        } else {
            return variableValueGreaterThan(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value);
            return this;
        } else {
            return variableValueLessThan(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value);
            return this;
        } else {
            return variableValueLike(name, value);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, true);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, true);
            return this;
        } else {
            return variableExists(name, true);
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, true);
            return this;
        } else {
            return variableNotExists(name, true);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLessThanOrEqual(name, value, ScopeTypes.BPMN);
            return this;
        } else {
            return scopedVariableValueLessThanOrEqual(name, value, ScopeTypes.BPMN);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLike(name, value, ScopeTypes.BPMN);
            return this;
        } else {
            return scopedVariableValueLike(name, value, ScopeTypes.BPMN);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLikeIgnoreCase(name, value, ScopeTypes.BPMN);
            return this;
        } else {
            return scopedVariableValueLikeIgnoreCase(name, value, ScopeTypes.BPMN);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableExists(name, ScopeTypes.BPMN);
            return this;
        } else {
            return scopedVariableExists(name, ScopeTypes.BPMN);
        }
    }

    @Override
    public HistoricTaskInstanceQuery processVariableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableNotExists(name, ScopeTypes.BPMN);
            return this;
        } else {
            return scopedVariableNotExists(name, ScopeTypes.BPMN);
        }
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueEquals(variableName, variableValue, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueEquals(variableName, variableValue, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueEquals(variableValue, ScopeTypes.CMMN);
        } else {
            scopedVariableValueEquals(variableValue, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueEqualsIgnoreCase(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueEqualsIgnoreCase(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueNotEquals(variableName, variableValue, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueNotEquals(variableName, variableValue, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueNotEqualsIgnoreCase(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueGreaterThan(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueGreaterThan(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueGreaterThanOrEqual(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueGreaterThanOrEqual(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLessThan(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLessThan(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLessThanOrEqual(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLessThanOrEqual(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLike(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLike(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableValueLikeIgnoreCase(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLikeIgnoreCase(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableExists(name, ScopeTypes.CMMN);
        } else {
            this.scopedVariableExists(name, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery caseVariableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.scopedVariableNotExists(name, ScopeTypes.CMMN);
        } else {
            this.scopedVariableNotExists(name, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskRootScopeId(String rootScopeId) {
        if (rootScopeId == null) {
            throw new FlowableIllegalArgumentException("Task parentScopeId is null");
        }
        if (inOrStatement) {
            currentOrQueryObject.rootScopeId = rootScopeId;
        } else {
            this.rootScopeId = rootScopeId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskParentScopeId(String parentScopeId) {
        if (parentScopeId == null) {
            throw new FlowableIllegalArgumentException("Task parentScopeId is null");
        }
        if (inOrStatement) {
            currentOrQueryObject.parentScopeId = parentScopeId;
        } else {
            this.parentScopeId = parentScopeId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDefinitionKey(String taskDefinitionKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDefinitionKey = taskDefinitionKey;
        } else {
            this.taskDefinitionKey = taskDefinitionKey;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDefinitionKeyLike(String taskDefinitionKeyLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDefinitionKeyLike = taskDefinitionKeyLike;
        } else {
            this.taskDefinitionKeyLike = taskDefinitionKeyLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDefinitionKeys(Collection<String> taskDefinitionKeys) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskDefinitionKeys = taskDefinitionKeys;
        } else {
            this.taskDefinitionKeys = taskDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskPriority(Integer taskPriority) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskPriority = taskPriority;
        } else {
            this.taskPriority = taskPriority;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskMinPriority(Integer taskMinPriority) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskMinPriority = taskMinPriority;
        } else {
            this.taskMinPriority = taskMinPriority;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskMaxPriority(Integer taskMaxPriority) {
        if (inOrStatement) {
            this.currentOrQueryObject.taskMaxPriority = taskMaxPriority;
        } else {
            this.taskMaxPriority = taskMaxPriority;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processFinished() {
        if (inOrStatement) {
            this.currentOrQueryObject.processFinished = true;
        } else {
            this.processFinished = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processUnfinished() {
        if (inOrStatement) {
            this.currentOrQueryObject.processUnfinished = true;
        } else {
            this.processUnfinished = true;
        }
        return this;
    }

    @Override
    protected void ensureVariablesInitialized() {
        for (QueryVariableValue var : queryVariableValues) {
            var.initialize(variableValueProvider);
        }

        for (HistoricTaskInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }
    
    @Override
    public HistoricTaskInstanceQuery taskState(String state) {
        if (inOrStatement) {
            this.currentOrQueryObject.state = state;
        } else {
            this.state = state;
        }
        return this;
    }
    
    @Override
    public HistoricTaskInstanceQuery taskCreatedOn(Date creationDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.createTime = creationDate;
        } else {
            this.createTime = creationDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCreatedBefore(Date creationBeforeDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.createTimeBefore = creationBeforeDate;
        } else {
            this.createTimeBefore = creationBeforeDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCreatedAfter(Date creationAfterDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.createTimeAfter = creationAfterDate;
        } else {
            this.createTimeAfter = creationAfterDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInProgressStartTimeOn(Date startTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartTime = startTime;
        } else {
            this.inProgressStartTime = startTime;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInProgressStartTimeBefore(Date before) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartTimeBefore = before;
        } else {
            this.inProgressStartTimeBefore = before;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInProgressStartTimeAfter(Date after) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartTimeAfter = after;
        } else {
            this.inProgressStartTimeAfter = after;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInProgressStartedBy(String startedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartedBy = startedBy;
        } else {
            this.inProgressStartedBy = startedBy;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskClaimedOn(Date claimedTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.claimTime = claimedTime;
        } else {
            this.claimTime = claimedTime;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskClaimedBefore(Date before) {
        if (inOrStatement) {
            this.currentOrQueryObject.claimTimeBefore = before;
        } else {
            this.claimTimeBefore = before;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskClaimedAfter(Date after) {
        if (inOrStatement) {
            this.currentOrQueryObject.claimTimeAfter = after;
        } else {
            this.claimTimeAfter = after;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskClaimedBy(String claimedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.claimedBy = claimedBy;
        } else {
            this.claimedBy = claimedBy;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskSuspendedOn(Date suspendedTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.suspendedTime = suspendedTime;
        } else {
            this.suspendedTime = suspendedTime;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskSuspendedBefore(Date before) {
        if (inOrStatement) {
            this.currentOrQueryObject.suspendedTimeBefore = before;
        } else {
            this.suspendedTimeBefore = before;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskSuspendedAfter(Date after) {
        if (inOrStatement) {
            this.currentOrQueryObject.suspendedTimeAfter = after;
        } else {
            this.suspendedTimeAfter = after;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskSuspendedBy(String suspendedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.suspendedBy = suspendedBy;
        } else {
            this.suspendedBy = suspendedBy;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedOn(Date completedDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.completedTime = completedDate;
        } else {
            this.completedTime = completedDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedBefore(Date completedBeforeDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.completedTimeBefore = completedBeforeDate;
        } else {
            this.completedTimeBefore = completedBeforeDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedAfter(Date completedAfterDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.completedTimeAfter = completedAfterDate;
        } else {
            this.completedTimeAfter = completedAfterDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedBy(String completedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.completedBy = completedBy;
        } else {
            this.completedBy = completedBy;
        }
        return this;
    }
    
    @Override
    public HistoricTaskInstanceQuery taskInProgressStartDueDate(Date dueDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartDueDate = dueDate;
        } else {
            this.inProgressStartDueDate = dueDate;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInProgressStartDueBefore(Date dueBefore) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartDueBefore = dueBefore;
        } else {
            this.inProgressStartDueBefore = dueBefore;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInProgressStartDueAfter(Date dueAfter) {
        if (inOrStatement) {
            this.currentOrQueryObject.inProgressStartDueAfter = dueAfter;
        } else {
            this.inProgressStartDueAfter = dueAfter;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery withoutTaskInProgressStartDueDate() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutInProgressStartDueDate = true;
        } else {
            this.withoutInProgressStartDueDate = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDueDate(Date dueDate) {
        if (inOrStatement) {
            this.currentOrQueryObject.dueDate = dueDate;
        } else {
            this.dueDate = dueDate;
        }
        return this;
    }
    
    @Override
    public HistoricTaskInstanceQuery taskDueBefore(Date dueBefore) {
        if (inOrStatement) {
            this.currentOrQueryObject.dueBefore = dueBefore;
        } else {
            this.dueBefore = dueBefore;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDueAfter(Date dueAfter) {
        if (inOrStatement) {
            this.currentOrQueryObject.dueAfter = dueAfter;
        } else {
            this.dueAfter = dueAfter;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery withoutTaskDueDate() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutDueDate = true;
        } else {
            this.withoutDueDate = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCategory(String category) {
        if (inOrStatement) {
            this.currentOrQueryObject.category = category;
        } else {
            this.category = category;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCategoryIn(Collection<String> taskCategoryInList) {
        checkTaskCategoryList(taskCategoryInList);

        if (inOrStatement) {
            currentOrQueryObject.categoryInList = taskCategoryInList;
        } else {
            this.categoryInList = taskCategoryInList;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCategoryNotIn(Collection<String> taskCategoryNotInList) {
        checkTaskCategoryList(taskCategoryNotInList);
        if (inOrStatement) {
            currentOrQueryObject.categoryNotInList = taskCategoryNotInList;
        } else {
            this.categoryNotInList = taskCategoryNotInList;
        }
        return this;
    }

    protected void checkTaskCategoryList(Collection<String> taskCategoryInList) {
        if (taskCategoryInList == null) {
            throw new FlowableIllegalArgumentException("Task category list is null");
        }
        if (taskCategoryInList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task category list is empty");
        }
        for (String processCategory : taskCategoryInList) {
            if (processCategory == null) {
                throw new FlowableIllegalArgumentException("None of the given task categories can be null");
            }
        }
    }

    @Override
    public HistoricTaskInstanceQuery taskWithoutCategory() {
        if (inOrStatement) {
            currentOrQueryObject.withoutCategory = true;
        } else {
            this.withoutCategory = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskWithFormKey() {
        if (inOrStatement) {
            currentOrQueryObject.withFormKey = true;
        } else {
            this.withFormKey = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskFormKey(String formKey) {
        if (formKey == null) {
            throw new FlowableIllegalArgumentException("Task formKey is null");
        }

        if (inOrStatement) {
            currentOrQueryObject.formKey = formKey;
        } else {
            this.formKey = formKey;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCandidateUser(String candidateUser) {
        if (candidateUser == null) {
            throw new FlowableIllegalArgumentException("Candidate user is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.candidateUser = candidateUser;
        } else {
            this.candidateUser = candidateUser;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCandidateGroup(String candidateGroup) {
        if (candidateGroup == null) {
            throw new FlowableIllegalArgumentException("Candidate group is null");
        }

        if (candidateGroups != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both candidateGroup and candidateGroupIn");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.candidateGroup = candidateGroup;
        } else {
            this.candidateGroup = candidateGroup;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCandidateGroupIn(Collection<String> candidateGroups) {
        if (candidateGroups == null) {
            throw new FlowableIllegalArgumentException("Candidate group list is null");
        }

        if (candidateGroups.isEmpty()) {
            throw new FlowableIllegalArgumentException("Candidate group list is empty");
        }

        if (candidateGroup != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both candidateGroupIn and candidateGroup");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.candidateGroups = candidateGroups;
        } else {
            this.candidateGroups = candidateGroups;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInvolvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("involved user is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInvolvedGroups(Collection<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("Involved groups are null");
        }
        if (involvedGroups.isEmpty()) {
            throw new FlowableIllegalArgumentException("Involved groups are empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroups = involvedGroups;
        } else {
            this.involvedGroups = involvedGroups;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery ignoreAssigneeValue() {
        if (inOrStatement) {
            this.currentOrQueryObject.ignoreAssigneeValue = true;
        } else {
            this.ignoreAssigneeValue = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("task tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("task tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskWithoutDeleteReason() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutDeleteReason = true;
        } else {
            this.withoutDeleteReason = true;
        }
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery withLocalizationFallback() {
        withLocalizationFallback = true;
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery includeTaskLocalVariables() {
        this.includeTaskLocalVariables = true;
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery includeProcessVariables() {
        this.includeProcessVariables = true;
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery includeCaseVariables() {
        this.includeCaseVariables = true;
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery includeIdentityLinks() {
        this.includeIdentityLinks = true;
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        if (databaseType != null) {
            currentOrQueryObject = new HistoricTaskInstanceQueryImpl(commandExecutor, databaseType, taskServiceConfiguration, variableServiceConfiguration);
        } else {
            currentOrQueryObject = new HistoricTaskInstanceQueryImpl(commandExecutor, taskServiceConfiguration, variableServiceConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    // ordering
    // /////////////////////////////////////////////////////////////////

    @Override
    public HistoricTaskInstanceQueryImpl orderByTaskId() {
        orderBy(HistoricTaskInstanceQueryProperty.HISTORIC_TASK_INSTANCE_ID);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByHistoricActivityInstanceId() {
        orderBy(HistoricTaskInstanceQueryProperty.PROCESS_DEFINITION_ID);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByProcessDefinitionId() {
        orderBy(HistoricTaskInstanceQueryProperty.PROCESS_DEFINITION_ID);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByProcessInstanceId() {
        orderBy(HistoricTaskInstanceQueryProperty.PROCESS_INSTANCE_ID);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByExecutionId() {
        orderBy(HistoricTaskInstanceQueryProperty.EXECUTION_ID);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByHistoricTaskInstanceDuration() {
        orderBy(HistoricTaskInstanceQueryProperty.DURATION);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByHistoricTaskInstanceEndTime() {
        orderBy(HistoricTaskInstanceQueryProperty.END);
        return this;
    }

    public HistoricTaskInstanceQueryImpl orderByHistoricActivityInstanceStartTime() {
        orderBy(HistoricTaskInstanceQueryProperty.START);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByHistoricTaskInstanceStartTime() {
        orderBy(HistoricTaskInstanceQueryProperty.START);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskCreateTime() {
        return orderByHistoricTaskInstanceStartTime();
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByTaskName() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_NAME);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByTaskDescription() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_DESCRIPTION);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskAssignee() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_ASSIGNEE);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskOwner() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_OWNER);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskDueDate() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_DUE_DATE);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByDueDateNullsFirst() {
        return orderBy(HistoricTaskInstanceQueryProperty.TASK_DUE_DATE, NullHandlingOnOrder.NULLS_FIRST);
    }

    @Override
    public HistoricTaskInstanceQuery orderByDueDateNullsLast() {
        return orderBy(HistoricTaskInstanceQueryProperty.TASK_DUE_DATE, NullHandlingOnOrder.NULLS_LAST);
    }

    @Override
    public HistoricTaskInstanceQuery orderByCategory() {
        orderBy(HistoricTaskInstanceQueryProperty.CATEGORY);
        return this;
    }

    @Override
    public HistoricTaskInstanceQueryImpl orderByDeleteReason() {
        orderBy(HistoricTaskInstanceQueryProperty.DELETE_REASON);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskDefinitionKey() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_DEFINITION_KEY);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskPriority() {
        orderBy(HistoricTaskInstanceQueryProperty.TASK_PRIORITY);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery orderByTenantId() {
        orderBy(HistoricTaskInstanceQueryProperty.TENANT_ID_);
        return this;
    }

    public Collection<String> getCandidateGroups() {
        if (candidateGroup != null) {
            Collection<String> candidateGroupList = new ArrayList<>(1);
            candidateGroupList.add(candidateGroup);
            return candidateGroupList;

        } else if (candidateGroups != null) {
            return candidateGroups;

        } else if (candidateUser != null) {
            return getGroupsForCandidateUser(candidateUser);
        }
        return null;
    }

    protected Collection<String> getGroupsForCandidateUser(String candidateUser) {
        Collection<String> groupIds = new ArrayList<>();
        IdmIdentityService idmIdentityService = taskServiceConfiguration.getIdmIdentityService();
        if (idmIdentityService != null) {
            List<Group> groups = idmIdentityService.createGroupQuery().groupMember(candidateUser).list();
            for (Group group : groups) {
                groupIds.add(group.getId());
            }
        }
        return groupIds;
    }

    @Override
    public void delete() {
        if (commandExecutor != null) {
            commandExecutor.execute(context -> {
                taskServiceConfiguration.getHistoricTaskInstanceEntityManager().deleteHistoricTaskInstances(this);
                return null;
            });
        } else {
            taskServiceConfiguration.getHistoricTaskInstanceEntityManager().deleteHistoricTaskInstances(this);
        }
    }

    @Override
    @Deprecated
    public void deleteWithRelatedData() {
        delete();
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Collection<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public boolean isWithoutProcessInstanceId() {
        return withoutProcessInstanceId;
    }

    public String getProcessInstanceBusinessKey() {
        return processInstanceBusinessKey;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getScopeId() {
        return scopeId;
    }
    
    public boolean isWithoutScopeId() {
        return withoutScopeId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    public String getTaskDefinitionId() {
        return taskDefinitionId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }

    public Collection<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public String getProcessDefinitionNameLike() {
        return processDefinitionNameLike;
    }

    public Collection<String> getProcessCategoryInList() {
        return processCategoryInList;
    }

    public Collection<String> getProcessCategoryNotInList() {
        return processCategoryNotInList;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public Collection<String> getDeploymentIds() {
        return deploymentIds;
    }

    public String getCmmnDeploymentId() {
        return cmmnDeploymentId;
    }

    public Collection<String> getCmmnDeploymentIds() {
        return cmmnDeploymentIds;
    }

    public String getProcessInstanceBusinessKeyLike() {
        return processInstanceBusinessKeyLike;
    }

    public String getTaskDefinitionKeyLike() {
        return taskDefinitionKeyLike;
    }

    public String getProcessInstanceIdWithChildren() {
        return processInstanceIdWithChildren;
    }

    public String getCaseInstanceIdWithChildren() {
        return caseInstanceIdWithChildren;
    }

    public Integer getTaskPriority() {
        return taskPriority;
    }

    public Integer getTaskMinPriority() {
        return taskMinPriority;
    }

    public Integer getTaskMaxPriority() {
        return taskMaxPriority;
    }

    public boolean isProcessFinished() {
        return processFinished;
    }

    public boolean isProcessUnfinished() {
        return processUnfinished;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Date getDueAfter() {
        return dueAfter;
    }

    public Date getDueBefore() {
        return dueBefore;
    }

    public boolean isWithoutDueDate() {
        return withoutDueDate;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public Date getCreateTimeBefore() {
        return createTimeBefore;
    }

    public Date getCreateTimeAfter() {
        return createTimeAfter;
    }

    public Date getCompletedTime() {
        return completedTime;
    }
    
    public Date getCompletedTimeBefore() {
        return completedTimeBefore;
    }

    public Date getCompletedTimeAfter() {
        return completedTimeAfter;
    }

    public String getCategory() {
        return category;
    }

    public Collection<String> getCategoryInList() {
        return categoryInList;
    }

    public Collection<String> getCategoryNotInList() {
        return categoryNotInList;
    }

    public boolean isWithoutCategory() {
        return withoutCategory;
    }

    public boolean isWithFormKey() {
        return withFormKey;
    }

    public String getFormKey() {
        return formKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public boolean isIncludeTaskLocalVariables() {
        return includeTaskLocalVariables;
    }

    public boolean isIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public boolean isIncludeIdentityLinks() {
        return includeIdentityLinks;
    }

    public boolean isInOrStatement() {
        return inOrStatement;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUnfinished() {
        return unfinished;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskNameLike() {
        return taskNameLike;
    }

    public Collection<String> getTaskNameList() {
        return taskNameList;
    }

    public Collection<String> getTaskNameListIgnoreCase() {
        return taskNameListIgnoreCase;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public String getTaskDescriptionLike() {
        return taskDescriptionLike;
    }

    public String getTaskDeleteReason() {
        return taskDeleteReason;
    }

    public String getTaskDeleteReasonLike() {
        return taskDeleteReasonLike;
    }

    public String getTaskAssignee() {
        return taskAssignee;
    }

    public String getTaskAssigneeLike() {
        return taskAssigneeLike;
    }
    
    public boolean isWithAssignee() {
        return withAssignee;
    }
    
    public boolean isWithoutAssignee() {
        return withoutAssignee;
    }

    public Collection<String> getTaskAssigneeIds() {
        return taskAssigneeIds;
    }

    public String getTaskId() {
        return taskId;
    }

    public Collection<String> getTaskIds() {
        return taskIds;
    }

    @Override
    public String getId() {
        return taskId;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public String getTaskOwnerLike() {
        return taskOwnerLike;
    }

    public String getTaskOwner() {
        return taskOwner;
    }

    public Collection<String> getTaskDefinitionKeys() {
        return taskDefinitionKeys;
    }

    public String getTaskParentTaskId() {
        return taskParentTaskId;
    }

    public String getCandidateUser() {
        return candidateUser;
    }

    public String getCandidateGroup() {
        return candidateGroup;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public Collection<String> getInvolvedGroups() {
        return involvedGroups;
    }

    public boolean isIgnoreAssigneeValue() {
        return ignoreAssigneeValue;
    }

    public String getProcessDefinitionKeyLikeIgnoreCase() {
        return processDefinitionKeyLikeIgnoreCase;
    }

    public String getProcessInstanceBusinessKeyLikeIgnoreCase() {
        return processInstanceBusinessKeyLikeIgnoreCase;
    }

    public String getTaskNameLikeIgnoreCase() {
        return taskNameLikeIgnoreCase;
    }

    public String getTaskDescriptionLikeIgnoreCase() {
        return taskDescriptionLikeIgnoreCase;
    }

    public String getTaskOwnerLikeIgnoreCase() {
        return taskOwnerLikeIgnoreCase;
    }

    public String getTaskAssigneeLikeIgnoreCase() {
        return taskAssigneeLikeIgnoreCase;
    }

    public boolean isWithoutDeleteReason() {
        return withoutDeleteReason;
    }

    public String getLocale() {
        return locale;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public String getCaseDefinitionKeyLike() {
        return caseDefinitionKeyLike;
    }

    public String getCaseDefinitionKeyLikeIgnoreCase() {
        return caseDefinitionKeyLikeIgnoreCase;
    }

    public Collection<String> getCaseDefinitionKeys() {
        return caseDefinitionKeys;
    }

    public boolean isWithLocalizationFallback() {
        return withLocalizationFallback;
    }

    public List<HistoricTaskInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    public List<List<String>> getSafeCandidateGroups() {
        return safeCandidateGroups;
    }

    public void setSafeCandidateGroups(List<List<String>> safeCandidateGroups) {
        this.safeCandidateGroups = safeCandidateGroups;
    }

    public List<List<String>> getSafeInvolvedGroups() {
        return safeInvolvedGroups;
    }

    public void setSafeInvolvedGroups(List<List<String>> safeInvolvedGroups) {
        this.safeInvolvedGroups = safeInvolvedGroups;
    }

    public Set<String> getScopeIds() {
        return scopeIds;
    }

    public List<List<String>> getSafeScopeIds() {
        return safeScopeIds;
    }

    public void setSafeScopeIds(List<List<String>> safeScopeIds) {
        this.safeScopeIds = safeScopeIds;
    }
}
