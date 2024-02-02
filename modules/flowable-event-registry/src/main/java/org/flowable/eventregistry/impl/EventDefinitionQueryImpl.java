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

package org.flowable.eventregistry.impl;

import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDefinitionQuery;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class EventDefinitionQueryImpl extends AbstractQuery<EventDefinitionQuery, EventDefinition> implements EventDefinitionQuery {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected Set<String> ids;
    protected String category;
    protected String categoryLike;
    protected String categoryNotEquals;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String deploymentId;
    protected Set<String> deploymentIds;
    protected String parentDeploymentId;
    protected String key;
    protected String keyLike;
    protected String keyLikeIgnoreCase;
    protected Integer version;
    protected Integer versionGt;
    protected Integer versionGte;
    protected Integer versionLt;
    protected Integer versionLte;
    protected boolean latest;
    protected String resourceName;
    protected String resourceNameLike;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public EventDefinitionQueryImpl() {
    }

    public EventDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public EventDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionId(String eventDefinitionId) {
        this.id = eventDefinitionId;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionIds(Set<String> eventDefinitionIds) {
        this.ids = eventDefinitionIds;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (nameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("nameLikeIgnoreCase is null");
        }
        this.nameLikeIgnoreCase = nameLikeIgnoreCase;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl parentDeploymentId(String parentDeploymentId) {
        if (parentDeploymentId == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentId is null");
        }
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }
    
    @Override
    public EventDefinitionQueryImpl eventDefinitionKeyLikeIgnoreCase(String keyLikeIgnoreCase) {
        if (keyLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("keyLikeIgnoreCase is null");
        }
        this.keyLikeIgnoreCase = keyLikeIgnoreCase;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventVersionGreaterThan(Integer eventVersion) {
        checkVersion(eventVersion);
        this.versionGt = eventVersion;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventVersionGreaterThanOrEquals(Integer eventVersion) {
        checkVersion(eventVersion);
        this.versionGte = eventVersion;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventVersionLowerThan(Integer eventVersion) {
        checkVersion(eventVersion);
        this.versionLt = eventVersion;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventVersionLowerThanOrEquals(Integer eventVersion) {
        checkVersion(eventVersion);
        this.versionLte = eventVersion;
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
    public EventDefinitionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl eventDefinitionResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("form tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl tenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("form tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public EventDefinitionQueryImpl withoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    @Override
    public EventDefinitionQuery orderByDeploymentId() {
        return orderBy(EventDefinitionQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public EventDefinitionQuery orderByEventDefinitionKey() {
        return orderBy(EventDefinitionQueryProperty.KEY);
    }

    @Override
    public EventDefinitionQuery orderByEventDefinitionCategory() {
        return orderBy(EventDefinitionQueryProperty.CATEGORY);
    }

    @Override
    public EventDefinitionQuery orderByEventDefinitionId() {
        return orderBy(EventDefinitionQueryProperty.ID);
    }

    @Override
    public EventDefinitionQuery orderByEventDefinitionName() {
        return orderBy(EventDefinitionQueryProperty.NAME);
    }

    @Override
    public EventDefinitionQuery orderByTenantId() {
        return orderBy(EventDefinitionQueryProperty.TENANT_ID);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getEventDefinitionEntityManager(commandContext).findEventDefinitionCountByQueryCriteria(this);
    }

    @Override
    public List<EventDefinition> executeList(CommandContext commandContext) {
        return CommandContextUtil.getEventDefinitionEntityManager(commandContext).findEventDefinitionsByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////

    public String getDeploymentId() {
        return deploymentId;
    }

    public Set<String> getDeploymentIds() {
        return deploymentIds;
    }

    public String getParentDeploymentId() {
        return parentDeploymentId;
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

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public String getKey() {
        return key;
    }

    public String getKeyLike() {
        return keyLike;
    }

    public String getKeyLikeIgnoreCase() {
        return keyLikeIgnoreCase;
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
}
