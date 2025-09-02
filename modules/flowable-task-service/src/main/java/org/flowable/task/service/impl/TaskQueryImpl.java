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
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.util.TaskVariableUtils;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;
import org.flowable.variable.service.impl.QueryVariableValue;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Tijs Rademakers
 */
public class TaskQueryImpl extends AbstractVariableQueryImpl<TaskQuery, Task> implements TaskQuery, CacheAwareQuery<TaskEntity> {

    private static final long serialVersionUID = 1L;
    
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected IdmIdentityService idmIdentityService;
    protected VariableServiceConfiguration variableServiceConfiguration;

    protected String taskId;
    protected Collection<String> taskIds;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected Collection<String> nameList;
    protected Collection<String> nameListIgnoreCase;
    protected String description;
    protected String descriptionLike;
    protected String descriptionLikeIgnoreCase;
    protected Integer priority;
    protected Integer minPriority;
    protected Integer maxPriority;
    protected String assignee;
    protected String assigneeLike;
    protected String assigneeLikeIgnoreCase;
    protected Collection<String> assigneeIds;
    protected String involvedUser;
    protected Collection<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected String owner;
    protected String ownerLike;
    protected String ownerLikeIgnoreCase;
    protected boolean unassigned;
    protected boolean withAssignee;
    protected boolean noDelegationState;
    protected DelegationState delegationState;
    protected String candidateUser;
    protected String candidateGroup;
    protected Collection<String> candidateGroups;
    private List<List<String>> safeCandidateGroups;
    protected boolean ignoreAssigneeValue;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String processInstanceId;
    protected Collection<String> processInstanceIds;
    protected boolean withoutProcessInstanceId;
    protected String executionId;
    protected String scopeId;
    protected String subScopeId;
    protected Set<String> scopeIds;
    protected List<List<String>> safeScopeIds;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String propagatedStageInstanceId;
    protected String processInstanceIdWithChildren;
    protected String caseInstanceIdWithChildren;
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
    protected String category;
    protected Collection<String> categoryInList;
    protected Collection<String> categoryNotInList;
    protected boolean withoutCategory;
    protected boolean withFormKey;
    protected String formKey;
    protected String taskDefinitionId;
    protected String key;
    protected String keyLike;
    protected Collection<String> keys;
    protected String processDefinitionKey;
    protected String processDefinitionKeyLike;
    protected String processDefinitionKeyLikeIgnoreCase;
    protected Collection<String> processDefinitionKeys;
    protected String processDefinitionId;
    protected String processDefinitionName;
    protected String processDefinitionNameLike;
    protected Collection<String> processCategoryInList;
    protected Collection<String> processCategoryNotInList;
    protected String deploymentId;
    protected Collection<String> deploymentIds;
    protected String cmmnDeploymentId;
    protected Collection<String> cmmnDeploymentIds;
    protected boolean withoutScopeId;
    protected String processInstanceBusinessKey;
    protected String processInstanceBusinessKeyLike;
    protected String processInstanceBusinessKeyLikeIgnoreCase;
    protected String caseDefinitionKey;
    protected String caseDefinitionKeyLike;
    protected String caseDefinitionKeyLikeIgnoreCase;
    protected String rootScopeId;
    protected String parentScopeId;
    protected Collection<String> caseDefinitionKeys;
    protected Date inProgressStartDueDate;
    protected Date inProgressStartDueBefore;
    protected Date inProgressStartDueAfter;
    protected boolean withoutInProgressStartDueDate;
    protected Date dueDate;
    protected Date dueBefore;
    protected Date dueAfter;
    protected boolean withoutDueDate;
    protected SuspensionState suspensionState;
    protected boolean excludeSubtasks;
    protected boolean includeTaskLocalVariables;
    protected boolean includeProcessVariables;
    protected boolean includeCaseVariables;
    protected boolean includeIdentityLinks;
    protected String userIdForCandidateAndAssignee;
    protected boolean bothCandidateAndAssigned;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected boolean orActive;
    protected List<TaskQueryImpl> orQueryObjects = new ArrayList<>();
    protected TaskQueryImpl currentOrQueryObject;

    private Collection<String> cachedCandidateGroups;

    public TaskQueryImpl() {
    }

    public TaskQueryImpl(CommandContext commandContext, TaskServiceConfiguration taskServiceConfiguration,
            VariableServiceConfiguration variableServiceConfiguration, IdmIdentityService idmIdentityService) {
        
        super(commandContext, variableServiceConfiguration);
        this.variableServiceConfiguration = variableServiceConfiguration;
        this.taskServiceConfiguration = taskServiceConfiguration;
        this.idmIdentityService = idmIdentityService;
    }

    public TaskQueryImpl(CommandExecutor commandExecutor, TaskServiceConfiguration taskServiceConfiguration,
            VariableServiceConfiguration variableServiceConfiguration, IdmIdentityService idmIdentityService) {
        
        super(commandExecutor, variableServiceConfiguration);
        this.variableServiceConfiguration = variableServiceConfiguration;
        this.taskServiceConfiguration = taskServiceConfiguration;
        this.idmIdentityService = idmIdentityService;
    }

    public TaskQueryImpl(CommandExecutor commandExecutor, String databaseType, TaskServiceConfiguration taskServiceConfiguration,
            VariableServiceConfiguration variableServiceConfiguration, IdmIdentityService idmIdentityService) {
        
        super(commandExecutor, variableServiceConfiguration);
        this.variableServiceConfiguration = variableServiceConfiguration;
        this.databaseType = databaseType;
        this.taskServiceConfiguration = taskServiceConfiguration;
        this.idmIdentityService = idmIdentityService;
    }

    @Override
    public TaskQueryImpl taskId(String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("Task id is null");
        }

        if (orActive) {
            currentOrQueryObject.taskId = taskId;
        } else {
            this.taskId = taskId;
        }
        return this;
    }

    @Override
    public TaskQuery taskIds(Collection<String> taskIds) {
        if (taskIds == null) {
            throw new FlowableIllegalArgumentException("Task ids are null");
        }
        if (orActive) {
            currentOrQueryObject.taskIds = taskIds;
        } else {
            this.taskIds = taskIds;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Task name is null");
        }

        if (orActive) {
            currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public TaskQuery taskNameIn(Collection<String> nameList) {
        if (nameList == null) {
            throw new FlowableIllegalArgumentException("Task name list is null");
        }
        if (nameList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task name list is empty");
        }
        for (String name : nameList) {
            if (name == null) {
                throw new FlowableIllegalArgumentException("None of the given task names can be null");
            }
        }

        if (name != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameIn and name");
        }
        if (nameLike != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameIn and nameLike");
        }
        if (nameLikeIgnoreCase != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameIn and nameLikeIgnoreCase");
        }

        if (orActive) {
            currentOrQueryObject.nameList = nameList;
        } else {
            this.nameList = nameList;
        }
        return this;
    }

    @Override
    public TaskQuery taskNameInIgnoreCase(Collection<String> nameList) {
        if (nameList == null) {
            throw new FlowableIllegalArgumentException("Task name list is null");
        }
        if (nameList.isEmpty()) {
            throw new FlowableIllegalArgumentException("Task name list is empty");
        }
        for (String name : nameList) {
            if (name == null) {
                throw new FlowableIllegalArgumentException("None of the given task names can be null");
            }
        }

        if (name != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameInIgnoreCase and name");
        }
        if (nameLike != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameInIgnoreCase and nameLike");
        }
        if (nameLikeIgnoreCase != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskNameInIgnoreCase and nameLikeIgnoreCase");
        }

        final int nameListSize = nameList.size();
        final Collection<String> caseIgnoredNameList = new ArrayList<>(nameListSize);
        for (String name : nameList) {
            caseIgnoredNameList.add(name.toLowerCase());
        }

        if (orActive) {
            this.currentOrQueryObject.nameListIgnoreCase = caseIgnoredNameList;
        } else {
            this.nameListIgnoreCase = caseIgnoredNameList;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("Task namelike is null");
        }

        if (orActive) {
            currentOrQueryObject.nameLike = nameLike;
        } else {
            this.nameLike = nameLike;
        }
        return this;
    }

    @Override
    public TaskQuery taskNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (nameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Task nameLikeIgnoreCase is null");
        }

        if (orActive) {
            currentOrQueryObject.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        } else {
            this.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskDescription(String description) {
        if (description == null) {
            throw new FlowableIllegalArgumentException("Description is null");
        }

        if (orActive) {
            currentOrQueryObject.description = description;
        } else {
            this.description = description;
        }
        return this;
    }

    @Override
    public TaskQuery taskDescriptionLike(String descriptionLike) {
        if (descriptionLike == null) {
            throw new FlowableIllegalArgumentException("Task descriptionlike is null");
        }
        if (orActive) {
            currentOrQueryObject.descriptionLike = descriptionLike;
        } else {
            this.descriptionLike = descriptionLike;
        }
        return this;
    }

    @Override
    public TaskQuery taskDescriptionLikeIgnoreCase(String descriptionLikeIgnoreCase) {
        if (descriptionLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Task descriptionLikeIgnoreCase is null");
        }
        if (orActive) {
            currentOrQueryObject.descriptionLikeIgnoreCase = descriptionLikeIgnoreCase.toLowerCase();
        } else {
            this.descriptionLikeIgnoreCase = descriptionLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public TaskQuery taskPriority(Integer priority) {
        if (priority == null) {
            throw new FlowableIllegalArgumentException("Priority is null");
        }
        if (orActive) {
            currentOrQueryObject.priority = priority;
        } else {
            this.priority = priority;
        }
        return this;
    }

    @Override
    public TaskQuery taskMinPriority(Integer minPriority) {
        if (minPriority == null) {
            throw new FlowableIllegalArgumentException("Min Priority is null");
        }
        if (orActive) {
            currentOrQueryObject.minPriority = minPriority;
        } else {
            this.minPriority = minPriority;
        }
        return this;
    }

    @Override
    public TaskQuery taskMaxPriority(Integer maxPriority) {
        if (maxPriority == null) {
            throw new FlowableIllegalArgumentException("Max Priority is null");
        }
        if (orActive) {
            currentOrQueryObject.maxPriority = maxPriority;
        } else {
            this.maxPriority = maxPriority;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskAssignee(String assignee) {
        if (assignee == null) {
            throw new FlowableIllegalArgumentException("Assignee is null");
        }
        if (orActive) {
            currentOrQueryObject.assignee = assignee;
        } else {
            this.assignee = assignee;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskAssigneeLike(String assigneeLike) {
        if (assigneeLike == null) {
            throw new FlowableIllegalArgumentException("AssigneeLike is null");
        }
        if (orActive) {
            currentOrQueryObject.assigneeLike = assigneeLike;
        } else {
            this.assigneeLike = assigneeLike;
        }
        return this;
    }

    @Override
    public TaskQuery taskAssigneeLikeIgnoreCase(String assigneeLikeIgnoreCase) {
        if (assigneeLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("assigneeLikeIgnoreCase is null");
        }
        if (orActive) {
            currentOrQueryObject.assigneeLikeIgnoreCase = assigneeLikeIgnoreCase.toLowerCase();
        } else {
            this.assigneeLikeIgnoreCase = assigneeLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public TaskQuery taskAssigneeIds(Collection<String> assigneeIds) {
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

        if (assignee != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskAssigneeIds and taskAssignee");
        }
        if (assigneeLike != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskAssigneeIds and taskAssigneeLike");
        }
        if (assigneeLikeIgnoreCase != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both taskAssigneeIds and taskAssigneeLikeIgnoreCase");
        }

        if (orActive) {
            currentOrQueryObject.assigneeIds = assigneeIds;
        } else {
            this.assigneeIds = assigneeIds;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskOwner(String owner) {
        if (owner == null) {
            throw new FlowableIllegalArgumentException("Owner is null");
        }
        if (orActive) {
            currentOrQueryObject.owner = owner;
        } else {
            this.owner = owner;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskOwnerLike(String ownerLike) {
        if (ownerLike == null) {
            throw new FlowableIllegalArgumentException("Owner is null");
        }
        if (orActive) {
            currentOrQueryObject.ownerLike = ownerLike;
        } else {
            this.ownerLike = ownerLike;
        }
        return this;
    }

    @Override
    public TaskQuery taskOwnerLikeIgnoreCase(String ownerLikeIgnoreCase) {
        if (ownerLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("OwnerLikeIgnoreCase");
        }
        if (orActive) {
            currentOrQueryObject.ownerLikeIgnoreCase = ownerLikeIgnoreCase.toLowerCase();
        } else {
            this.ownerLikeIgnoreCase = ownerLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public TaskQuery taskUnassigned() {
        if (orActive) {
            currentOrQueryObject.unassigned = true;
        } else {
            this.unassigned = true;
        }
        return this;
    }

    @Override
    public TaskQuery taskAssigned() {
        if (orActive) {
            currentOrQueryObject.withAssignee = true;
        } else {
            this.withAssignee = true;
        }
        return this;
    }

    @Override
    public TaskQuery taskDelegationState(DelegationState delegationState) {
        if (orActive) {
            if (delegationState == null) {
                currentOrQueryObject.noDelegationState = true;
            } else {
                currentOrQueryObject.delegationState = delegationState;
            }
        } else {
            if (delegationState == null) {
                this.noDelegationState = true;
            } else {
                this.delegationState = delegationState;
            }
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskCandidateUser(String candidateUser) {
        if (candidateUser == null) {
            throw new FlowableIllegalArgumentException("Candidate user is null");
        }

        if (orActive) {
            currentOrQueryObject.candidateUser = candidateUser;
        } else {
            this.candidateUser = candidateUser;
        }

        return this;
    }

    @Override
    public TaskQueryImpl taskInvolvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("Involved user is null");
        }
        if (orActive) {
            currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskInvolvedGroups(Collection<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("Involved groups are null");
        }
        if (involvedGroups.isEmpty()) {
            throw new FlowableIllegalArgumentException("Involved groups are empty");
        }
        if (orActive) {
            currentOrQueryObject.involvedGroups = involvedGroups;
        } else {
            this.involvedGroups = involvedGroups;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskCandidateGroup(String candidateGroup) {
        if (candidateGroup == null) {
            throw new FlowableIllegalArgumentException("Candidate group is null");
        }

        if (candidateGroups != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both candidateGroup and candidateGroupIn");
        }

        if (orActive) {
            currentOrQueryObject.candidateGroup = candidateGroup;
        } else {
            this.candidateGroup = candidateGroup;
        }
        return this;
    }

    @Override
    public TaskQuery taskCandidateOrAssigned(String userIdForCandidateAndAssignee) {
        if (candidateGroup != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set candidateGroup");
        }
        if (candidateUser != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both candidateGroup and candidateUser");
        }

        if (orActive) {
            currentOrQueryObject.bothCandidateAndAssigned = true;
            currentOrQueryObject.userIdForCandidateAndAssignee = userIdForCandidateAndAssignee;
        } else {
            this.bothCandidateAndAssigned = true;
            this.userIdForCandidateAndAssignee = userIdForCandidateAndAssignee;
        }

        return this;
    }

    @Override
    public TaskQuery taskCandidateGroupIn(Collection<String> candidateGroups) {
        if (candidateGroups == null) {
            throw new FlowableIllegalArgumentException("Candidate group list is null");
        }

        if (candidateGroups.isEmpty()) {
            throw new FlowableIllegalArgumentException("Candidate group list is empty");
        }

        if (candidateGroup != null) {
            throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both candidateGroupIn and candidateGroup");
        }

        if (orActive) {
            currentOrQueryObject.candidateGroups = candidateGroups;
        } else {
            this.candidateGroups = candidateGroups;
        }
        return this;
    }

    @Override
    public TaskQuery ignoreAssigneeValue() {
        if (orActive) {
            currentOrQueryObject.ignoreAssigneeValue = true;
        } else {
            this.ignoreAssigneeValue = true;
        }
        return this;
    }

    @Override
    public TaskQuery taskTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("task tenant id is null");
        }
        if (orActive) {
            currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public TaskQuery taskTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("task tenant id is null");
        }
        if (orActive) {
            currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }

    @Override
    public TaskQuery taskWithoutTenantId() {
        if (orActive) {
            currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public TaskQueryImpl processInstanceId(String processInstanceId) {
        if (orActive) {
            currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }
        return this;
    }

    @Override
    public TaskQuery processInstanceIdIn(Collection<String> processInstanceIds) {
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

        if (orActive) {
            currentOrQueryObject.processInstanceIds = processInstanceIds;
        } else {
            this.processInstanceIds = processInstanceIds;
        }
        return this;
    }

    @Override
    public TaskQueryImpl withoutProcessInstanceId() {
        if (orActive) {
            currentOrQueryObject.withoutProcessInstanceId = true;
        } else {
            this.withoutProcessInstanceId = true;
        }
        return this;
    }

    @Override
    public TaskQueryImpl processInstanceBusinessKey(String processInstanceBusinessKey) {
        if (orActive) {
            currentOrQueryObject.processInstanceBusinessKey = processInstanceBusinessKey;
        } else {
            this.processInstanceBusinessKey = processInstanceBusinessKey;
        }
        return this;
    }

    @Override
    public TaskQueryImpl processInstanceBusinessKeyLike(String processInstanceBusinessKeyLike) {
        if (orActive) {
            currentOrQueryObject.processInstanceBusinessKeyLike = processInstanceBusinessKeyLike;
        } else {
            this.processInstanceBusinessKeyLike = processInstanceBusinessKeyLike;
        }
        return this;
    }

    @Override
    public TaskQuery processInstanceBusinessKeyLikeIgnoreCase(String processInstanceBusinessKeyLikeIgnoreCase) {
        if (orActive) {
            currentOrQueryObject.processInstanceBusinessKeyLikeIgnoreCase = processInstanceBusinessKeyLikeIgnoreCase.toLowerCase();
        } else {
            this.processInstanceBusinessKeyLikeIgnoreCase = processInstanceBusinessKeyLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public TaskQueryImpl executionId(String executionId) {
        if (orActive) {
            currentOrQueryObject.executionId = executionId;
        } else {
            this.executionId = executionId;
        }
        return this;
    }

    @Override
    public TaskQuery caseInstanceId(String caseInstanceId) {
        if (orActive) {
            currentOrQueryObject.scopeId(caseInstanceId);
            currentOrQueryObject.scopeType(ScopeTypes.CMMN);
        } else {
            this.scopeId(caseInstanceId);
            this.scopeType(ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseDefinitionId(String caseDefinitionId) {
        if (orActive) {
            currentOrQueryObject.scopeDefinitionId(caseDefinitionId);
            currentOrQueryObject.scopeType(ScopeTypes.CMMN);
        } else {
            this.scopeDefinitionId(caseDefinitionId);
            this.scopeType(ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseDefinitionKey(String caseDefinitionKey) {
        if (orActive) {
            currentOrQueryObject.caseDefinitionKey = caseDefinitionKey;
        } else {
            this.caseDefinitionKey = caseDefinitionKey;
        }
        return this;
    }

    @Override
    public TaskQuery caseDefinitionKeyLike(String caseDefinitionKeyLike) {
        if (orActive) {
            currentOrQueryObject.caseDefinitionKeyLike = caseDefinitionKeyLike;
        } else {
            this.caseDefinitionKeyLike = caseDefinitionKeyLike;
        }
        return this;
    }

    @Override
    public TaskQuery caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase) {
        if (orActive) {
            currentOrQueryObject.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
        } else {
            this.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public TaskQuery caseDefinitionKeyIn(Collection<String> caseDefinitionKeys) {
        if (orActive) {
            currentOrQueryObject.caseDefinitionKeys = caseDefinitionKeys;
        } else {
            this.caseDefinitionKeys = caseDefinitionKeys;
        }
        return this;
    }

    @Override
    public TaskQuery processInstanceIdWithChildren(String processInstanceId) {
        if (orActive) {
            currentOrQueryObject.processInstanceIdWithChildren(processInstanceId);
        } else {
            this.processInstanceIdWithChildren = processInstanceId;
        }
        return this;
    }

    @Override
    public TaskQuery caseInstanceIdWithChildren(String caseInstanceId) {
        if (orActive) {
            currentOrQueryObject.caseInstanceIdWithChildren(caseInstanceId);
        } else {
            this.caseInstanceIdWithChildren = caseInstanceId;
        }
        return this;
    }

    @Override
    public TaskQuery planItemInstanceId(String planItemInstanceId) {
        if (orActive) {
            currentOrQueryObject.subScopeId(planItemInstanceId);
            currentOrQueryObject.scopeType(ScopeTypes.CMMN);
        } else {
            this.subScopeId(planItemInstanceId);
            this.scopeType(ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQueryImpl scopeId(String scopeId) {
        if (orActive) {
            currentOrQueryObject.scopeId = scopeId;
        } else {
            this.scopeId = scopeId;
        }
        return this;
    }

    @Override
    public TaskQueryImpl scopeIds(Set<String> scopeIds) {
        if (orActive) {
            currentOrQueryObject.scopeIds = scopeIds;
        } else {
            this.scopeIds = scopeIds;
        }
        return this;
    }

    @Override
    public TaskQueryImpl subScopeId(String subScopeId) {
        if (orActive) {
            currentOrQueryObject.subScopeId = subScopeId;
        } else {
            this.subScopeId = subScopeId;
        }
        return this;
    }

    @Override
    public TaskQueryImpl scopeType(String scopeType) {
        if (orActive) {
            currentOrQueryObject.scopeType = scopeType;
        } else {
            this.scopeType = scopeType;
        }
        return this;
    }

    @Override
    public TaskQueryImpl scopeDefinitionId(String scopeDefinitionId) {
        if (orActive) {
            currentOrQueryObject.scopeDefinitionId = scopeDefinitionId;
        } else {
            this.scopeDefinitionId = scopeDefinitionId;
        }
        return this;
    }

    @Override
    public TaskQuery propagatedStageInstanceId(String propagatedStageInstanceId) {
        if (orActive) {
            currentOrQueryObject.propagatedStageInstanceId = propagatedStageInstanceId;
        } else {
            this.propagatedStageInstanceId = propagatedStageInstanceId;
        }
        return this;
    }
    
    @Override
    public TaskQuery taskState(String state) {
        if (orActive) {
            currentOrQueryObject.state = state;
        } else {
            this.state = state;
        }
        return this;
    }

    @Override
    public TaskQueryImpl taskCreatedOn(Date createTime) {
        if (orActive) {
            currentOrQueryObject.createTime = createTime;
        } else {
            this.createTime = createTime;
        }
        return this;
    }

    @Override
    public TaskQuery taskCreatedBefore(Date before) {
        if (orActive) {
            currentOrQueryObject.createTimeBefore = before;
        } else {
            this.createTimeBefore = before;
        }
        return this;
    }

    @Override
    public TaskQuery taskCreatedAfter(Date after) {
        if (orActive) {
            currentOrQueryObject.createTimeAfter = after;
        } else {
            this.createTimeAfter = after;
        }
        return this;
    }
    
    @Override
    public TaskQueryImpl taskInProgressStartTimeOn(Date inProgressStartTime) {
        if (orActive) {
            currentOrQueryObject.inProgressStartTime = inProgressStartTime;
        } else {
            this.inProgressStartTime = inProgressStartTime;
        }
        return this;
    }

    @Override
    public TaskQuery taskInProgressStartTimeBefore(Date before) {
        if (orActive) {
            currentOrQueryObject.inProgressStartTimeBefore = before;
        } else {
            this.inProgressStartTimeBefore = before;
        }
        return this;
    }

    @Override
    public TaskQuery taskInProgressStartTimeAfter(Date after) {
        if (orActive) {
            currentOrQueryObject.inProgressStartTimeAfter = after;
        } else {
            this.inProgressStartTimeAfter = after;
        }
        return this;
    }
    
    @Override
    public TaskQuery taskInProgressStartedBy(String startedBy) {
        if (orActive) {
            currentOrQueryObject.inProgressStartedBy = startedBy;
        } else {
            this.inProgressStartedBy = startedBy;
        }
        return this;
    }
    
    @Override
    public TaskQueryImpl taskClaimedOn(Date claimTime) {
        if (orActive) {
            currentOrQueryObject.claimTime = claimTime;
        } else {
            this.claimTime = claimTime;
        }
        return this;
    }

    @Override
    public TaskQuery taskClaimedBefore(Date before) {
        if (orActive) {
            currentOrQueryObject.claimTimeBefore = before;
        } else {
            this.claimTimeBefore = before;
        }
        return this;
    }

    @Override
    public TaskQuery taskClaimedAfter(Date after) {
        if (orActive) {
            currentOrQueryObject.claimTimeAfter = after;
        } else {
            this.claimTimeAfter = after;
        }
        return this;
    }
    
    @Override
    public TaskQuery taskClaimedBy(String claimedBy) {
        if (orActive) {
            currentOrQueryObject.claimedBy = claimedBy;
        } else {
            this.claimedBy = claimedBy;
        }
        return this;
    }
    
    @Override
    public TaskQueryImpl taskSuspendedOn(Date suspendedTime) {
        if (orActive) {
            currentOrQueryObject.suspendedTime = suspendedTime;
        } else {
            this.suspendedTime = suspendedTime;
        }
        return this;
    }

    @Override
    public TaskQuery taskSuspendedBefore(Date before) {
        if (orActive) {
            currentOrQueryObject.suspendedTimeBefore = before;
        } else {
            this.suspendedTimeBefore = before;
        }
        return this;
    }

    @Override
    public TaskQuery taskSuspendedAfter(Date after) {
        if (orActive) {
            currentOrQueryObject.suspendedTimeAfter = after;
        } else {
            this.suspendedTimeAfter = after;
        }
        return this;
    }
    
    @Override
    public TaskQuery taskSuspendedBy(String suspendedBy) {
        if (orActive) {
            currentOrQueryObject.suspendedBy = suspendedBy;
        } else {
            this.suspendedBy = suspendedBy;
        }
        return this;
    }

    @Override
    public TaskQuery taskCategory(String category) {
        if (orActive) {
            currentOrQueryObject.category = category;
        } else {
            this.category = category;
        }
        return this;
    }

    @Override
    public TaskQuery taskCategoryIn(Collection<String> taskCategoryInList) {
        checkTaskCategoryList(taskCategoryInList);
        if (orActive) {
            currentOrQueryObject.categoryInList = taskCategoryInList;
        } else {
            this.categoryInList = taskCategoryInList;
        }
        return this;
    }

    @Override
    public TaskQuery taskCategoryNotIn(Collection<String> taskCategoryNotInList) {
        checkTaskCategoryList(taskCategoryNotInList);
        if (orActive) {
            currentOrQueryObject.categoryNotInList = taskCategoryNotInList;
        } else {
            this.categoryNotInList = taskCategoryNotInList;
        }
        return this;
    }

    @Override
    public TaskQuery taskWithoutCategory() {
        if (orActive) {
            currentOrQueryObject.withoutCategory = true;
        } else {
            this.withoutCategory = true;
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
        for (String category : taskCategoryInList) {
            if (category == null) {
                throw new FlowableIllegalArgumentException("None of the given task categories can be null");
            }
        }
    }

    @Override
    public TaskQuery taskWithFormKey() {
        if (orActive) {
            currentOrQueryObject.withFormKey = true;
        } else {
            this.withFormKey = true;
        }
        return this;
    }

    @Override
    public TaskQuery taskFormKey(String formKey) {
        if (formKey == null) {
            throw new FlowableIllegalArgumentException("Task formKey is null");
        }
        if (orActive) {
            currentOrQueryObject.formKey = formKey;
        } else {
            this.formKey = formKey;
        }
        return this;
    }

    @Override
    public TaskQuery taskDefinitionId(String taskDefinitionId) {
        if (orActive) {
            currentOrQueryObject.taskDefinitionId = taskDefinitionId;
        } else {
            this.taskDefinitionId = taskDefinitionId;
        }
        return this;
    }

    @Override
    public TaskQuery taskDefinitionKey(String key) {
        if (orActive) {
            currentOrQueryObject.key = key;
        } else {
            this.key = key;
        }
        return this;
    }

    @Override
    public TaskQuery taskDefinitionKeyLike(String keyLike) {
        if (orActive) {
            currentOrQueryObject.keyLike = keyLike;
        } else {
            this.keyLike = keyLike;
        }
        return this;
    }

    @Override
    public TaskQuery taskDefinitionKeys(Collection<String> keys) {
        if (orActive) {
            currentOrQueryObject.keys = keys;
        } else {
            this.keys = keys;
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueEquals(String variableName, Object variableValue) {
        if (orActive) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue);
        } else {
            this.variableValueEquals(variableName, variableValue);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueEquals(Object variableValue) {
        if (orActive) {
            currentOrQueryObject.variableValueEquals(variableValue);
        } else {
            this.variableValueEquals(variableValue);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueEqualsIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value);
        } else {
            this.variableValueEqualsIgnoreCase(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value);
        } else {
            this.variableValueNotEqualsIgnoreCase(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueNotEquals(String variableName, Object variableValue) {
        if (orActive) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue);
        } else {
            this.variableValueNotEquals(variableName, variableValue);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueGreaterThan(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueGreaterThan(name, value);
        } else {
            this.variableValueGreaterThan(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueGreaterThanOrEqual(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value);
        } else {
            this.variableValueGreaterThanOrEqual(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueLessThan(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueLessThan(name, value);
        } else {
            this.variableValueLessThan(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueLessThanOrEqual(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value);
        } else {
            this.variableValueLessThanOrEqual(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueLike(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueLike(name, value);
        } else {
            this.variableValueLike(name, value);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableValueLikeIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value);
        } else {
            this.variableValueLikeIgnoreCase(name, value);
        }
        return this;
    }


    @Override
    public TaskQuery taskVariableExists(String name) {
        if (orActive) {
            currentOrQueryObject.variableExists(name);
        } else {
            this.variableExists(name);
        }
        return this;
    }

    @Override
    public TaskQuery taskVariableNotExists(String name) {
        if (orActive) {
            currentOrQueryObject.variableNotExists(name);
        } else {
            this.variableNotExists(name);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueEquals(String variableName, Object variableValue) {
        if (orActive) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
        } else {
            this.variableValueEquals(variableName, variableValue, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueNotEquals(String variableName, Object variableValue) {
        if (orActive) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
        } else {
            this.variableValueNotEquals(variableName, variableValue, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueEquals(Object variableValue) {
        if (orActive) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
        } else {
            this.variableValueEquals(variableValue, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueEqualsIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
        } else {
            this.variableValueEqualsIgnoreCase(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
        } else {
            this.variableValueNotEqualsIgnoreCase(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueGreaterThan(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
        } else {
            this.variableValueGreaterThan(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueGreaterThanOrEqual(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
        } else {
            this.variableValueGreaterThanOrEqual(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueLessThan(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
        } else {
            this.variableValueLessThan(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueLessThanOrEqual(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
        } else {
            this.variableValueLessThanOrEqual(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueLike(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueLike(name, value, false);
        } else {
            this.variableValueLike(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableValueLikeIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
        } else {
            this.variableValueLikeIgnoreCase(name, value, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableExists(String name) {
        if (orActive) {
            currentOrQueryObject.variableExists(name, false);
        } else {
            this.variableExists(name, false);
        }
        return this;
    }

    @Override
    public TaskQuery processVariableNotExists(String name) {
        if (orActive) {
            currentOrQueryObject.variableNotExists(name, false);
        } else {
            this.variableNotExists(name, false);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueEquals(String variableName, Object variableValue) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueEquals(variableName, variableValue, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueEquals(variableName, variableValue, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueEquals(Object variableValue) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueEquals(variableValue, ScopeTypes.CMMN);
        } else {
            scopedVariableValueEquals(variableValue, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueEqualsIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueEqualsIgnoreCase(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueEqualsIgnoreCase(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueNotEquals(String variableName, Object variableValue) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueNotEquals(variableName, variableValue, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueNotEquals(variableName, variableValue, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueNotEqualsIgnoreCase(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueGreaterThan(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueGreaterThan(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueGreaterThan(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueGreaterThanOrEqual(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueGreaterThanOrEqual(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueGreaterThanOrEqual(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueLessThan(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueLessThan(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLessThan(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueLessThanOrEqual(String name, Object value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueLessThanOrEqual(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLessThanOrEqual(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueLike(String name, String value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueLike(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLike(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableValueLikeIgnoreCase(String name, String value) {
        if (orActive) {
            currentOrQueryObject.scopedVariableValueLikeIgnoreCase(name, value, ScopeTypes.CMMN);
        } else {
            this.scopedVariableValueLikeIgnoreCase(name, value, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableExists(String name) {
        if (orActive) {
            currentOrQueryObject.scopedVariableExists(name, ScopeTypes.CMMN);
        } else {
            this.scopedVariableExists(name, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery caseVariableNotExists(String name) {
        if (orActive) {
            currentOrQueryObject.scopedVariableNotExists(name, ScopeTypes.CMMN);
        } else {
            this.scopedVariableNotExists(name, ScopeTypes.CMMN);
        }
        return this;
    }

    @Override
    public TaskQuery taskRootScopeId(String rootScopeId) {
        if (rootScopeId == null) {
            throw new FlowableIllegalArgumentException("Task parentScopeId is null");
        }
        if (orActive) {
            currentOrQueryObject.rootScopeId = rootScopeId;
        } else {
            this.rootScopeId = rootScopeId;
        }
        return this;
    }

    @Override
    public TaskQuery taskParentScopeId(String parentScopeId) {
        if (parentScopeId == null) {
            throw new FlowableIllegalArgumentException("Task parentScopeId is null");
        }
        if (orActive) {
            currentOrQueryObject.parentScopeId = parentScopeId;
        } else {
            this.parentScopeId = parentScopeId;
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionKey(String processDefinitionKey) {
        if (orActive) {
            currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionKeyLike(String processDefinitionKeyLike) {
        if (orActive) {
            currentOrQueryObject.processDefinitionKeyLike = processDefinitionKeyLike;
        } else {
            this.processDefinitionKeyLike = processDefinitionKeyLike;
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        if (orActive) {
            currentOrQueryObject.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase.toLowerCase();
        } else {
            this.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase.toLowerCase();
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionKeyIn(Collection<String> processDefinitionKeys) {
        if (orActive) {
            this.currentOrQueryObject.processDefinitionKeys = processDefinitionKeys;
        } else {
            this.processDefinitionKeys = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionId(String processDefinitionId) {
        if (orActive) {
            currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionName(String processDefinitionName) {
        if (orActive) {
            currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }

    @Override
    public TaskQuery processDefinitionNameLike(String processDefinitionNameLike) {
        if (orActive) {
            currentOrQueryObject.processDefinitionNameLike = processDefinitionNameLike;
        } else {
            this.processDefinitionNameLike = processDefinitionNameLike;
        }
        return this;
    }

    @Override
    public TaskQuery processCategoryIn(Collection<String> processCategoryInList) {
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

        if (orActive) {
            currentOrQueryObject.processCategoryInList = processCategoryInList;
        } else {
            this.processCategoryInList = processCategoryInList;
        }
        return this;
    }

    @Override
    public TaskQuery processCategoryNotIn(Collection<String> processCategoryNotInList) {
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

        if (orActive) {
            currentOrQueryObject.processCategoryNotInList = processCategoryNotInList;
        } else {
            this.processCategoryNotInList = processCategoryNotInList;
        }
        return this;
    }

    @Override
    public TaskQuery deploymentId(String deploymentId) {
        if (orActive) {
            currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }

    @Override
    public TaskQuery deploymentIdIn(Collection<String> deploymentIds) {
        if (orActive) {
            currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }

    @Override
    public TaskQuery cmmnDeploymentId(String cmmnDeploymentId) {
        if (orActive) {
            currentOrQueryObject.cmmnDeploymentId = cmmnDeploymentId;
        } else {
            this.cmmnDeploymentId = cmmnDeploymentId;
        }
        return this;
    }

    @Override
    public TaskQuery cmmnDeploymentIdIn(Collection<String> cmmnDeploymentIds) {
        if (orActive) {
            currentOrQueryObject.cmmnDeploymentIds = cmmnDeploymentIds;
        } else {
            this.cmmnDeploymentIds = cmmnDeploymentIds;
        }
        return this;
    }
    
    @Override
    public TaskQuery withoutScopeId() {
        if (orActive) {
            currentOrQueryObject.withoutScopeId = true;
        } else {
            this.withoutScopeId = true;
        }
        return this;
    }

    public TaskQuery dueDate(Date dueDate) {
        if (orActive) {
            currentOrQueryObject.dueDate = dueDate;
            currentOrQueryObject.withoutDueDate = false;
        } else {
            this.dueDate = dueDate;
            this.withoutDueDate = false;
        }
        return this;
    }

    @Override
    public TaskQuery taskDueDate(Date dueDate) {
        return dueDate(dueDate);
    }

    public TaskQuery dueBefore(Date dueBefore) {
        if (orActive) {
            currentOrQueryObject.dueBefore = dueBefore;
            currentOrQueryObject.withoutDueDate = false;
        } else {
            this.dueBefore = dueBefore;
            this.withoutDueDate = false;
        }
        return this;
    }

    @Override
    public TaskQuery taskDueBefore(Date dueDate) {
        return dueBefore(dueDate);
    }

    public TaskQuery dueAfter(Date dueAfter) {
        if (orActive) {
            currentOrQueryObject.dueAfter = dueAfter;
            currentOrQueryObject.withoutDueDate = false;
        } else {
            this.dueAfter = dueAfter;
            this.withoutDueDate = false;
        }
        return this;
    }

    @Override
    public TaskQuery taskDueAfter(Date dueDate) {
        return dueAfter(dueDate);
    }

    public TaskQuery withoutDueDate() {
        if (orActive) {
            currentOrQueryObject.withoutDueDate = true;
        } else {
            this.withoutDueDate = true;
        }
        return this;
    }

    @Override
    public TaskQuery withoutTaskDueDate() {
        return withoutDueDate();
    }
    
    @Override
    public TaskQuery taskInProgressStartDueDate(Date dueDate) {
        if (orActive) {
            currentOrQueryObject.inProgressStartDueDate = dueDate;
            currentOrQueryObject.withoutInProgressStartDueDate = false;
        } else {
            this.inProgressStartDueDate = dueDate;
            this.withoutInProgressStartDueDate = false;
        }
        return this;
    }
    
    @Override
    public TaskQuery taskInProgressStartDueBefore(Date dueBefore) {
        if (orActive) {
            currentOrQueryObject.inProgressStartDueBefore = dueBefore;
            currentOrQueryObject.withoutDueDate = false;
        } else {
            this.inProgressStartDueBefore = dueBefore;
            this.withoutDueDate = false;
        }
        return this;
    }
    
    @Override
    public TaskQuery taskInProgressStartDueAfter(Date dueAfter) {
        if (orActive) {
            currentOrQueryObject.inProgressStartDueAfter = dueAfter;
            currentOrQueryObject.withoutDueDate = false;
        } else {
            this.inProgressStartDueAfter = dueAfter;
            this.withoutDueDate = false;
        }
        return this;
    }
    
    @Override
    public TaskQuery withoutTaskInProgressStartDueDate() {
        if (orActive) {
            currentOrQueryObject.withoutInProgressStartDueDate = true;
        } else {
            this.withoutInProgressStartDueDate = true;
        }
        return this;
    }

    @Override
    public TaskQuery excludeSubtasks() {
        if (orActive) {
            currentOrQueryObject.excludeSubtasks = true;
        } else {
            this.excludeSubtasks = true;
        }
        return this;
    }

    @Override
    public TaskQuery suspended() {
        if (orActive) {
            currentOrQueryObject.suspensionState = SuspensionState.SUSPENDED;
        } else {
            this.suspensionState = SuspensionState.SUSPENDED;
        }
        return this;
    }

    @Override
    public TaskQuery active() {
        if (orActive) {
            currentOrQueryObject.suspensionState = SuspensionState.ACTIVE;
        } else {
            this.suspensionState = SuspensionState.ACTIVE;
        }
        return this;
    }

    @Override
    public TaskQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public TaskQuery withLocalizationFallback() {
        withLocalizationFallback = true;
        return this;
    }

    @Override
    public TaskQuery includeTaskLocalVariables() {
        this.includeTaskLocalVariables = true;
        return this;
    }

    @Override
    public TaskQuery includeProcessVariables() {
        this.includeProcessVariables = true;
        return this;
    }

    @Override
    public TaskQuery includeCaseVariables() {
        this.includeCaseVariables = true;
        return this;
    }

    @Override
    public TaskQuery includeIdentityLinks() {
        this.includeIdentityLinks = true;
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
            if (cachedCandidateGroups == null) {
                cachedCandidateGroups = getGroupsForCandidateUser(candidateUser);
            }
            return cachedCandidateGroups;

        } else if (userIdForCandidateAndAssignee != null) {
            if (cachedCandidateGroups == null) {
                cachedCandidateGroups = getGroupsForCandidateUser(userIdForCandidateAndAssignee);
            }
            return cachedCandidateGroups;
        }
        return null;
    }

    protected Collection<String> getGroupsForCandidateUser(String candidateUser) {
        Collection<String> groupIds = new ArrayList<>();
        if (idmIdentityService != null) {
            List<Group> groups = idmIdentityService.createGroupQuery()
                    .groupMember(candidateUser)
                    .list();
            for (Group group : groups) {
                groupIds.add(group.getId());
            }
        }
        return groupIds;
    }

    @Override
    protected void ensureVariablesInitialized() {
        for (QueryVariableValue var : queryVariableValues) {
            var.initialize(variableValueProvider);
        }

        for (TaskQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    // or query ////////////////////////////////////////////////////////////////

    @Override
    public TaskQuery or() {
        if (orActive) {
            throw new FlowableException("the query is already in an or statement");
        }

        // Create instance of the orQuery
        orActive = true;
        if (commandContext != null) {
            currentOrQueryObject = new TaskQueryImpl(commandContext, taskServiceConfiguration, variableServiceConfiguration, idmIdentityService);
        } else {
            currentOrQueryObject = new TaskQueryImpl(commandExecutor, taskServiceConfiguration, variableServiceConfiguration, idmIdentityService);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public TaskQuery endOr() {
        if (!orActive) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        orActive = false;
        currentOrQueryObject = null;
        return this;
    }

    // ordering ////////////////////////////////////////////////////////////////

    @Override
    public TaskQuery orderByTaskId() {
        return orderBy(TaskQueryProperty.TASK_ID);
    }

    @Override
    public TaskQuery orderByTaskName() {
        return orderBy(TaskQueryProperty.NAME);
    }

    @Override
    public TaskQuery orderByTaskDescription() {
        return orderBy(TaskQueryProperty.DESCRIPTION);
    }

    @Override
    public TaskQuery orderByTaskPriority() {
        return orderBy(TaskQueryProperty.PRIORITY);
    }

    @Override
    public TaskQuery orderByProcessInstanceId() {
        return orderBy(TaskQueryProperty.PROCESS_INSTANCE_ID);
    }

    @Override
    public TaskQuery orderByExecutionId() {
        return orderBy(TaskQueryProperty.EXECUTION_ID);
    }

    @Override
    public TaskQuery orderByProcessDefinitionId() {
        return orderBy(TaskQueryProperty.PROCESS_DEFINITION_ID);
    }

    @Override
    public TaskQuery orderByTaskAssignee() {
        return orderBy(TaskQueryProperty.ASSIGNEE);
    }

    @Override
    public TaskQuery orderByTaskOwner() {
        return orderBy(TaskQueryProperty.OWNER);
    }

    @Override
    public TaskQuery orderByTaskCreateTime() {
        return orderBy(TaskQueryProperty.CREATE_TIME);
    }

    public TaskQuery orderByDueDate() {
        return orderBy(TaskQueryProperty.DUE_DATE);
    }

    @Override
    public TaskQuery orderByTaskDueDate() {
        return orderByDueDate();
    }

    @Override
    public TaskQuery orderByTaskDefinitionKey() {
        return orderBy(TaskQueryProperty.TASK_DEFINITION_KEY);
    }

    @Override
    public TaskQuery orderByDueDateNullsFirst() {
        return orderBy(TaskQueryProperty.DUE_DATE, NullHandlingOnOrder.NULLS_FIRST);
    }

    @Override
    public TaskQuery orderByDueDateNullsLast() {
        return orderBy(TaskQueryProperty.DUE_DATE, NullHandlingOnOrder.NULLS_LAST);
    }

    @Override
    public TaskQuery orderByCategory() {
        return orderBy(TaskQueryProperty.CATEGORY);
    }

    @Override
    public TaskQuery orderByTenantId() {
        return orderBy(TaskQueryProperty.TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<Task> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<Task> tasks = null;
        if (taskServiceConfiguration.getTaskQueryInterceptor() != null) {
            taskServiceConfiguration.getTaskQueryInterceptor()
                    .beforeTaskQueryExecute(this);
        }

        if (includeTaskLocalVariables || includeProcessVariables || includeIdentityLinks || includeCaseVariables) {
            tasks = taskServiceConfiguration.getTaskEntityManager()
                    .findTasksWithRelatedEntitiesByQueryCriteria(this);

            if (taskId != null) {
                if (includeProcessVariables || includeCaseVariables) {
                    addCachedVariableForQueryById(commandContext, tasks, false);
                } else if (includeTaskLocalVariables) {
                    addCachedVariableForQueryById(commandContext, tasks, true);
                }
            }

        } else {
            tasks = taskServiceConfiguration.getTaskEntityManager()
                    .findTasksByQueryCriteria(this);
        }

        if (tasks != null && taskServiceConfiguration.getInternalTaskLocalizationManager() != null && taskServiceConfiguration.isEnableLocalization()) {
            for (Task task : tasks) {
                taskServiceConfiguration.getInternalTaskLocalizationManager()
                        .localize(task, locale, withLocalizationFallback);
            }
        }

        if (taskServiceConfiguration.getTaskQueryInterceptor() != null) {
            taskServiceConfiguration.getTaskQueryInterceptor()
                    .afterTaskQueryExecute(this, tasks);
        }

        return tasks;
    }

    protected void addCachedVariableForQueryById(CommandContext commandContext, List<Task> results, boolean local) {
        for (Task task : results) {
            if (Objects.equals(taskId, task.getId())) {

                EntityCache entityCache = commandContext.getSession(EntityCache.class);
                List<VariableInstanceEntity> cachedVariableEntities = entityCache.findInCache(VariableInstanceEntity.class);
                for (VariableInstanceEntity cachedVariableEntity : cachedVariableEntities) {

                    if (local) {
                        if (task.getId()
                                .equals(cachedVariableEntity.getTaskId())) {
                            ((TaskEntity) task).getQueryVariables()
                                    .add(cachedVariableEntity);
                        }
                    } else if (TaskVariableUtils.doesVariableBelongToTask(cachedVariableEntity, task)) {
                        ((TaskEntity) task).getQueryVariables().add(cachedVariableEntity);
                    }
                }
            }
        }
    }

    @Override
    public void enhanceCachedValue(TaskEntity task) {
        if (includeProcessVariables && task.getProcessInstanceId() != null) {
            task.getQueryVariables()
                    .addAll(variableServiceConfiguration.getVariableService()
                            .findVariableInstancesByExecutionId(task.getProcessInstanceId()));
        } else if (includeCaseVariables && TaskVariableUtils.isCaseRelated(task)) {
            task.getQueryVariables()
                    .addAll((variableServiceConfiguration.getVariableService()
                            .findVariableInstanceByScopeIdAndScopeType(task.getScopeId(), task.getScopeType())));
        } else if (includeTaskLocalVariables) {
            task.getQueryVariables()
                    .addAll(task.getVariableInstanceEntities()
                            .values());
        }
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();

        if (taskServiceConfiguration.getTaskQueryInterceptor() != null) {
            taskServiceConfiguration.getTaskQueryInterceptor()
                    .beforeTaskQueryExecute(this);
        }

        return taskServiceConfiguration.getTaskEntityManager()
                .findTaskCountByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public Collection<String> getNameList() {
        return nameList;
    }

    public Collection<String> getNameListIgnoreCase() {
        return nameListIgnoreCase;
    }

    public String getAssignee() {
        return assignee;
    }

    public boolean getUnassigned() {
        return unassigned;
    }
    
    public boolean isWithAssignee() {
        return withAssignee;
    }

    public DelegationState getDelegationState() {
        return delegationState;
    }

    public boolean getNoDelegationState() {
        return noDelegationState;
    }

    public String getDelegationStateString() {
        return (delegationState != null ? delegationState.toString() : null);
    }

    public String getCandidateUser() {
        return candidateUser;
    }

    public String getCandidateGroup() {
        return candidateGroup;
    }

    public boolean isIgnoreAssigneeValue() {
        return ignoreAssigneeValue;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Collection<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public boolean isWithoutProcessInstanceId() {
        return withoutProcessInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public Set<String> getScopeIds() {
        return scopeIds;
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

    public String getDescription() {
        return description;
    }

    public String getDescriptionLike() {
        return descriptionLike;
    }

    public Integer getPriority() {
        return priority;
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

    public String getTaskDefinitionId() {
        return taskDefinitionId;
    }

    public String getKey() {
        return key;
    }

    public String getKeyLike() {
        return keyLike;
    }

    public Collection<String> getKeys() {
        return keys;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public String getProcessInstanceBusinessKey() {
        return processInstanceBusinessKey;
    }

    public String getProcessInstanceIdWithChildren() {
        return processInstanceIdWithChildren;
    }

    public String getCaseInstanceIdWithChildren() {
        return caseInstanceIdWithChildren;
    }

    public boolean getExcludeSubtasks() {
        return excludeSubtasks;
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

    public String getUserIdForCandidateAndAssignee() {
        return userIdForCandidateAndAssignee;
    }

    public List<TaskQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    public void setOrQueryObjects(List<TaskQueryImpl> orQueryObjects) {
        this.orQueryObjects = orQueryObjects;
    }

    public Integer getMinPriority() {
        return minPriority;
    }

    public Integer getMaxPriority() {
        return maxPriority;
    }

    public String getAssigneeLike() {
        return assigneeLike;
    }

    public Collection<String> getAssigneeIds() {
        return assigneeIds;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public Collection<String> getInvolvedGroups() {
        return involvedGroups;
    }

    public String getOwner() {
        return owner;
    }

    public String getOwnerLike() {
        return ownerLike;
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

    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }

    public Collection<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
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

    public Date getDueDate() {
        return dueDate;
    }

    public Date getDueBefore() {
        return dueBefore;
    }

    public Date getDueAfter() {
        return dueAfter;
    }

    public boolean isWithoutDueDate() {
        return withoutDueDate;
    }

    public SuspensionState getSuspensionState() {
        return suspensionState;
    }

    public boolean isIncludeTaskLocalVariables() {
        return includeTaskLocalVariables;
    }

    public boolean isIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public boolean isIncludeCaseVariables() {
        return includeCaseVariables;
    }

    public boolean isIncludeIdentityLinks() {
        return includeIdentityLinks;
    }

    public boolean isBothCandidateAndAssigned() {
        return bothCandidateAndAssigned;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public String getDescriptionLikeIgnoreCase() {
        return descriptionLikeIgnoreCase;
    }

    public String getAssigneeLikeIgnoreCase() {
        return assigneeLikeIgnoreCase;
    }

    public String getOwnerLikeIgnoreCase() {
        return ownerLikeIgnoreCase;
    }

    public String getProcessInstanceBusinessKeyLikeIgnoreCase() {
        return processInstanceBusinessKeyLikeIgnoreCase;
    }

    public String getProcessDefinitionKeyLikeIgnoreCase() {
        return processDefinitionKeyLikeIgnoreCase;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isOrActive() {
        return orActive;
    }

    public boolean isUnassigned() {
        return unassigned;
    }

    public boolean isNoDelegationState() {
        return noDelegationState;
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

    public boolean isExcludeSubtasks() {
        return excludeSubtasks;
    }

    public boolean isWithLocalizationFallback() {
        return withLocalizationFallback;
    }

    @Override
    public List<Task> list() {
        cachedCandidateGroups = null;
        return super.list();
    }

    @Override
    public List<Task> listPage(int firstResult, int maxResults) {
        cachedCandidateGroups = null;
        return super.listPage(firstResult, maxResults);
    }

    @Override
    public long count() {
        cachedCandidateGroups = null;
        return super.count();
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

    public List<List<String>> getSafeScopeIds() {
        return safeScopeIds;
    }

    public void setSafeScopeIds(List<List<String>> safeScopeIds) {
        this.safeScopeIds = safeScopeIds;
    }
}
