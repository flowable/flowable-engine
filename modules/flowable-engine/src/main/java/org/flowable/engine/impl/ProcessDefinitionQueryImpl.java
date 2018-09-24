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

package org.flowable.engine.impl;

import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Daniel Meyer
 * @author Saeid Mirzaei
 */
public class ProcessDefinitionQueryImpl extends AbstractQuery<ProcessDefinitionQuery, ProcessDefinition> implements ProcessDefinitionQuery {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected Set<String> ids;
    protected String category;
    protected String categoryLike;
    protected String categoryNotEquals;
    protected String name;
    protected String nameLike;
    protected String deploymentId;
    protected Set<String> deploymentIds;
    protected String key;
    protected String keyLike;
    protected String resourceName;
    protected String resourceNameLike;
    protected Integer version;
    protected Integer versionGt;
    protected Integer versionGte;
    protected Integer versionLt;
    protected Integer versionLte;
    protected boolean latest;
    protected SuspensionState suspensionState;
    protected String authorizationUserId;
    protected String procDefId;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String engineVersion;

    protected String eventSubscriptionName;
    protected String eventSubscriptionType;

    public ProcessDefinitionQueryImpl() {
    }

    public ProcessDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ProcessDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionId(String processDefinitionId) {
        this.id = processDefinitionId;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionIds(Set<String> processDefinitionIds) {
        this.ids = processDefinitionIds;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    @Override
    public ProcessDefinitionQueryImpl processDefinitionVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionVersionGreaterThan(Integer processDefinitionVersion) {
        checkVersion(processDefinitionVersion);
        this.versionGt = processDefinitionVersion;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionVersionGreaterThanOrEquals(Integer processDefinitionVersion) {
        checkVersion(processDefinitionVersion);
        this.versionGte = processDefinitionVersion;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionVersionLowerThan(Integer processDefinitionVersion) {
        checkVersion(processDefinitionVersion);
        this.versionLt = processDefinitionVersion;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionVersionLowerThanOrEquals(Integer processDefinitionVersion) {
        checkVersion(processDefinitionVersion);
        this.versionLte = processDefinitionVersion;
        return this;
    }

    protected void checkVersion(Integer version) {
        if (version == null) {
            throw new FlowableIllegalArgumentException("version is null");
        } else if (version <= 0) {
            throw new FlowableIllegalArgumentException("version must be positive");
        }
    }

    @Override
    public ProcessDefinitionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    @Override
    public ProcessDefinitionQuery active() {
        this.suspensionState = SuspensionState.ACTIVE;
        return this;
    }

    @Override
    public ProcessDefinitionQuery suspended() {
        this.suspensionState = SuspensionState.SUSPENDED;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("processDefinition tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("process definition tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public ProcessDefinitionQuery processDefinitionEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
        return this;
    }

    public ProcessDefinitionQuery messageEventSubscription(String messageName) {
        return eventSubscription("message", messageName);
    }

    @Override
    public ProcessDefinitionQuery messageEventSubscriptionName(String messageName) {
        return eventSubscription("message", messageName);
    }

    public ProcessDefinitionQuery processDefinitionStarter(String procDefId) {
        this.procDefId = procDefId;
        return this;
    }

    public ProcessDefinitionQuery eventSubscription(String eventType, String eventName) {
        if (eventName == null) {
            throw new FlowableIllegalArgumentException("event name is null");
        }
        if (eventType == null) {
            throw new FlowableException("event type is null");
        }
        this.eventSubscriptionType = eventType;
        this.eventSubscriptionName = eventName;
        return this;
    }

    public List<String> getAuthorizationGroups() {
        if (authorizationUserId == null) {
            return null;
        }
        return CommandContextUtil.getProcessEngineConfiguration().getCandidateManager().getGroupsForCandidateUser(authorizationUserId);
    }

    // sorting ////////////////////////////////////////////

    @Override
    public ProcessDefinitionQuery orderByDeploymentId() {
        return orderBy(ProcessDefinitionQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public ProcessDefinitionQuery orderByProcessDefinitionKey() {
        return orderBy(ProcessDefinitionQueryProperty.PROCESS_DEFINITION_KEY);
    }

    @Override
    public ProcessDefinitionQuery orderByProcessDefinitionCategory() {
        return orderBy(ProcessDefinitionQueryProperty.PROCESS_DEFINITION_CATEGORY);
    }

    @Override
    public ProcessDefinitionQuery orderByProcessDefinitionId() {
        return orderBy(ProcessDefinitionQueryProperty.PROCESS_DEFINITION_ID);
    }

    @Override
    public ProcessDefinitionQuery orderByProcessDefinitionVersion() {
        return orderBy(ProcessDefinitionQueryProperty.PROCESS_DEFINITION_VERSION);
    }

    @Override
    public ProcessDefinitionQuery orderByProcessDefinitionName() {
        return orderBy(ProcessDefinitionQueryProperty.PROCESS_DEFINITION_NAME);
    }

    @Override
    public ProcessDefinitionQuery orderByTenantId() {
        return orderBy(ProcessDefinitionQueryProperty.PROCESS_DEFINITION_TENANT_ID);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getProcessDefinitionEntityManager(commandContext).findProcessDefinitionCountByQueryCriteria(this);
    }

    @Override
    public List<ProcessDefinition> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getProcessDefinitionEntityManager(commandContext).findProcessDefinitionsByQueryCriteria(this);
    }

    @Override
    public void checkQueryOk() {
        super.checkQueryOk();
    }

    // getters ////////////////////////////////////////////

    public String getDeploymentId() {
        return deploymentId;
    }

    public Set<String> getDeploymentIds() {
        return deploymentIds;
    }

    public String getId() {
        return id;
    }

    public Set<String> getIds() {
        return ids;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public String getKey() {
        return key;
    }

    public String getKeyLike() {
        return keyLike;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getVersionGt() {
        return versionGt;
    }

    public Integer getVersionGte() {
        return versionGte;
    }

    public Integer getVersionLt() {
        return versionLt;
    }

    public Integer getVersionLte() {
        return versionLte;
    }

    public boolean isLatest() {
        return latest;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryLike() {
        return categoryLike;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceNameLike() {
        return resourceNameLike;
    }

    public SuspensionState getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(SuspensionState suspensionState) {
        this.suspensionState = suspensionState;
    }

    public String getCategoryNotEquals() {
        return categoryNotEquals;
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

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getAuthorizationUserId() {
        return authorizationUserId;
    }

    public String getProcDefId() {
        return procDefId;
    }

    public String getEventSubscriptionName() {
        return eventSubscriptionName;
    }

    public String getEventSubscriptionType() {
        return eventSubscriptionType;
    }

    @Override
    public ProcessDefinitionQueryImpl startableByUser(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("userId is null");
        }
        this.authorizationUserId = userId;
        return this;
    }
}
