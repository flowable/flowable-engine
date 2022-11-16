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
package org.flowable.engine.data.inmemory.impl.execution;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.IdentityLinkQueryObject;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryValue;
import org.flowable.variable.service.impl.QueryVariableValue;

/**
 * An execution query that wraps either an {@link ExecutionQueryImpl} or a
 * {@link ProcessInstanceQueryImpl} as they both end up targeting 'executions'
 * table.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class CombinedExecutionQueryImpl {

    private ExecutionQueryImpl executionQuery;

    private ProcessInstanceQueryImpl processInstanceQuery;

    private boolean orQuery = false;

    private List<CombinedExecutionQueryImpl> combinedOrQueryObjects;

    public CombinedExecutionQueryImpl(ExecutionQueryImpl executionQuery) {
        this.executionQuery = executionQuery;
    }

    public CombinedExecutionQueryImpl(ProcessInstanceQueryImpl processInstanceQuery) {
        this.processInstanceQuery = processInstanceQuery;
    }

    public CombinedExecutionQueryImpl setOrQuery(boolean orQuery) {
        this.orQuery = orQuery;
        return this;
    }

    public boolean isOrQuery() {
        return orQuery;
    }

    public boolean isProcessInstancesOnly() {
        return executionQuery != null ? executionQuery.isProcessInstancesOnly() : processInstanceQuery.getOnlyProcessInstances();
    }

    public String getProcessDefinitionCategory() {
        return executionQuery != null ? executionQuery.getProcessDefinitionCategory() : processInstanceQuery.getProcessDefinitionCategory();
    }

    public String getExecutionId() {
        return executionQuery != null ? executionQuery.getExecutionId() : processInstanceQuery.getExecutionId();
    }

    public String getProcessInstanceId() {
        return executionQuery != null ? executionQuery.getProcessInstanceId() : processInstanceQuery.getProcessInstanceId();
    }

    public String getActivityId() {
        return executionQuery != null ? executionQuery.getActivityId() : processInstanceQuery.getActivityId();
    }

    public boolean isActive() {
        return executionQuery != null ? executionQuery.isActive() : false;
    }

    public Set<String> getProcessInstanceIds() {
        if (executionQuery != null) {
            if (executionQuery.getProcessInstanceIds() == null) {
                return null;
            }
            Set<String> r = new HashSet<>();
            r.add(executionQuery.getProcessInstanceIds());
            return r;
        }
        return processInstanceQuery.getProcessInstanceIds();
    }

    public String getProcessDefinitionId() {
        return executionQuery != null ? executionQuery.getProcessDefinitionId() : processInstanceQuery.getProcessDefinitionId();
    }

    public Set<String> getProcessDefinitionIds() {
        return executionQuery != null ? executionQuery.getProcessDefinitionIds() : processInstanceQuery.getProcessDefinitionIds();
    }

    public String getProcessDefinitionKey() {
        return executionQuery != null ? executionQuery.getProcessDefinitionKey() : processInstanceQuery.getProcessDefinitionKey();
    }

    public Set<String> getProcessDefinitionKeys() {
        return executionQuery != null ? executionQuery.getProcessDefinitionKeys() : processInstanceQuery.getProcessDefinitionKeys();
    }

    public String getProcessDefinitionName() {
        return executionQuery != null ? executionQuery.getProcessDefinitionName() : processInstanceQuery.getProcessDefinitionName();
    }

    public Integer getProcessDefinitionVersion() {
        return executionQuery != null ? executionQuery.getProcessDefinitionVersion() : processInstanceQuery.getProcessDefinitionVersion();
    }

    public String getProcessDefinitionEngineVersion() {
        return executionQuery != null ? executionQuery.getProcessDefinitionEngineVersion() : processInstanceQuery.getProcessDefinitionEngineVersion();
    }

    public String getBusinessKey() {
        return executionQuery != null ? executionQuery.getBusinessKey() : processInstanceQuery.getBusinessKey();
    }

    public String getBusinessKeyLike() {
        return executionQuery != null ? executionQuery.getBusinessKeyLike() : processInstanceQuery.getBusinessKeyLike();
    }

    public boolean isIncludeChildExecutionsWithBusinessKeyQuery() {
        return executionQuery != null ? executionQuery.isIncludeChildExecutionsWithBusinessKeyQuery()
                        : processInstanceQuery.isIncludeChildExecutionsWithBusinessKeyQuery();
    }

    public String getBusinessStatus() {
        return executionQuery != null ? executionQuery.getBusinessStatus() : processInstanceQuery.getBusinessStatus();
    }

    public String getBusinessStatusLike() {
        return executionQuery != null ? executionQuery.getBusinessStatusLike() : processInstanceQuery.getBusinessStatusLike();
    }

    public String getParentId() {
        return executionQuery != null ? executionQuery.getParentId() : processInstanceQuery.getParentId();
    }

    public boolean isOnlyChildExecutions() {
        return executionQuery != null ? executionQuery.isOnlyChildExecutions() : processInstanceQuery.isOnlyChildExecutions();
    }

    public boolean isOnlySubProcessExecutions() {
        return executionQuery != null ? executionQuery.isOnlySubProcessExecutions() : processInstanceQuery.isOnlySubProcessExecutions();
    }

    public boolean isOnlyProcessInstanceExecutions() {
        return executionQuery != null ? executionQuery.isOnlyProcessInstanceExecutions() : processInstanceQuery.isOnlyProcessInstanceExecutions();
    }

    public String getDeploymentId() {
        return executionQuery != null ? executionQuery.getDeploymentId() : processInstanceQuery.getDeploymentId();
    }

    public List<String> getDeploymentIds() {
        return executionQuery != null ? executionQuery.getDeploymentIds() : processInstanceQuery.getDeploymentIds();
    }

    public String getSuperProcessInstanceId() {
        return executionQuery != null ? executionQuery.getSuperProcessInstanceId() : processInstanceQuery.getSuperProcessInstanceId();
    }

    public String getSubProcessInstanceId() {
        return executionQuery != null ? executionQuery.getSubProcessInstanceId() : processInstanceQuery.getSubProcessInstanceId();
    }

    public String getRootProcessInstanceId() {
        return executionQuery != null ? executionQuery.getRootProcessInstanceId() : processInstanceQuery.getRootProcessInstanceId();
    }

    public boolean isExcludeSubprocesses() {
        return executionQuery != null ? executionQuery.isExcludeSubprocesses() : processInstanceQuery.isExcludeSubprocesses();
    }

    public SuspensionState getSuspensionState() {
        return executionQuery != null ? executionQuery.getSuspensionState() : processInstanceQuery.getSuspensionState();
    }

    public String getCallbackId() {
        return executionQuery != null ? executionQuery.getCallbackId() : processInstanceQuery.getCallbackId();
    }

    public String getCallbackType() {
        return executionQuery != null ? executionQuery.getCallbackType() : processInstanceQuery.getCallbackType();
    }

    public String getReferenceId() {
        return executionQuery != null ? executionQuery.getReferenceId() : processInstanceQuery.getReferenceId();
    }

    public String getReferenceType() {
        return executionQuery != null ? executionQuery.getReferenceType() : processInstanceQuery.getReferenceType();
    }

    public String getTenantId() {
        return executionQuery != null ? executionQuery.getTenantId() : processInstanceQuery.getTenantId();
    }

    public String getTenantIdLike() {
        return executionQuery != null ? executionQuery.getTenantIdLike() : processInstanceQuery.getTenantIdLike();
    }

    public boolean isWithoutTenantId() {
        return executionQuery != null ? executionQuery.isWithoutTenantId() : processInstanceQuery.isWithoutTenantId();
    }

    public String getName() {
        return executionQuery != null ? executionQuery.getName() : processInstanceQuery.getName();
    }

    public String getNameLike() {
        return executionQuery != null ? executionQuery.getNameLike() : processInstanceQuery.getNameLike();
    }

    public String getNameLikeIgnoreCase() {
        return executionQuery != null ? executionQuery.getNameLikeIgnoreCase() : processInstanceQuery.getNameLikeIgnoreCase();
    }

    public String getActiveActivityId() {
        return executionQuery != null ? executionQuery.getActiveActivityId() : processInstanceQuery.getActiveActivityId();
    }

    public Set<String> getActiveActivityIds() {
        return executionQuery != null ? executionQuery.getActiveActivityIds() : processInstanceQuery.getActiveActivityIds();
    }

    public String getInvolvedUser() {
        return executionQuery != null ? executionQuery.getInvolvedUser() : processInstanceQuery.getInvolvedUser();
    }

    public IdentityLinkQueryObject getInvolvedUserIdentityLink() {
        return executionQuery != null ? executionQuery.getInvolvedUserIdentityLink() : processInstanceQuery.getInvolvedUserIdentityLink();
    }

    public Set<String> getInvolvedGroups() {
        return executionQuery != null ? executionQuery.getInvolvedGroups() : processInstanceQuery.getInvolvedGroups();
    }

    public IdentityLinkQueryObject getInvolvedGroupIdentityLink() {
        return executionQuery != null ? executionQuery.getInvolvedGroupIdentityLink() : processInstanceQuery.getInvolvedGroupIdentityLink();
    }

    public List<QueryVariableValue> getQueryVariableValues() {
        return executionQuery != null ? executionQuery.getQueryVariableValues() : processInstanceQuery.getQueryVariableValues();
    }

    public List<EventSubscriptionQueryValue> getEventSubscriptions() {
        return executionQuery != null ? executionQuery.getEventSubscriptions() : processInstanceQuery.getEventSubscriptions();
    }

    public List<CombinedExecutionQueryImpl> getOrQueryObjects() {
        if (combinedOrQueryObjects == null) {
            combinedOrQueryObjects = internalGetOrQueryObjects();
        }
        return combinedOrQueryObjects;
    }

    private List<CombinedExecutionQueryImpl> internalGetOrQueryObjects() {
        if (executionQuery != null) {
            if (executionQuery.getOrQueryObjects() == null || executionQuery.getOrQueryObjects().isEmpty()) {
                return Collections.emptyList();
            }
            return executionQuery.getOrQueryObjects().stream().map(obj -> new CombinedExecutionQueryImpl(obj).setOrQuery(true)).collect(Collectors.toList());
        }
        if (processInstanceQuery.getOrQueryObjects() == null || processInstanceQuery.getOrQueryObjects().isEmpty()) {
            return Collections.emptyList();
        }
        return processInstanceQuery.getOrQueryObjects().stream().map(obj -> new CombinedExecutionQueryImpl(obj).setOrQuery(true)).collect(Collectors.toList());
    }

    public Date getStartedBefore() {
        return executionQuery != null ? executionQuery.getStartedBefore() : processInstanceQuery.getStartedBefore();
    }

    public Date getStartedAfter() {
        return executionQuery != null ? executionQuery.getStartedAfter() : processInstanceQuery.getStartedAfter();
    }

    public String getStartedBy() {
        return executionQuery != null ? executionQuery.getStartedBy() : processInstanceQuery.getStartedBy();
    }

    public String getOrderBy() {
        return executionQuery != null ? executionQuery.getOrderBy() : processInstanceQuery.getOrderBy();
    }

    public int getFirstResult() {
        return executionQuery != null ? executionQuery.getFirstResult() : processInstanceQuery.getFirstResult();
    }

    public int getMaxResults() {
        return executionQuery != null ? executionQuery.getMaxResults() : processInstanceQuery.getMaxResults();
    }
    
    public boolean isWithJobException() {
        // not supported on execution query
        return processInstanceQuery != null ? processInstanceQuery.isWithJobException() : false;
    }

    @Override
    public String toString() {
        return "CombinedExecutionQuery[type=" + (executionQuery == null ? "process" : "execution") + ", orQuery=" + orQuery + ", " + details() + "]";
    }

    private String details() {
        if (executionQuery != null) {
            return "executionQuery[executionId=" + executionQuery + "]";
        }
        return "processQuery[processInstanceId=" + processInstanceQuery + "]";
    }
}
