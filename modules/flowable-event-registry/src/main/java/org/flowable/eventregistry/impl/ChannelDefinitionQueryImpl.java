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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.ChannelDefinitionQuery;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class ChannelDefinitionQueryImpl extends AbstractQuery<ChannelDefinitionQuery, ChannelDefinition> implements ChannelDefinitionQuery {

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
    protected boolean onlyInbound;
    protected boolean onlyOutbound;
    protected String implementation;
    protected Date createTime;
    protected Date createTimeAfter;
    protected Date createTimeBefore;
    protected String resourceName;
    protected String resourceNameLike;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public ChannelDefinitionQueryImpl() {
    }

    public ChannelDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ChannelDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionId(String channelDefinitionId) {
        this.id = channelDefinitionId;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionIds(Set<String> channelDefinitionIds) {
        this.ids = channelDefinitionIds;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (nameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("nameLikeIgnoreCase is null");
        }
        this.nameLikeIgnoreCase = nameLikeIgnoreCase;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl parentDeploymentId(String parentDeploymentId) {
        if (parentDeploymentId == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentId is null");
        }
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }
    
    @Override
    public ChannelDefinitionQuery channelDefinitionKeyLikeIgnoreCase(String keyLikeIgnoreCase) {
        if (keyLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("keyLikeIgnoreCase is null");
        }
        this.keyLikeIgnoreCase = keyLikeIgnoreCase;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelVersionGreaterThan(Integer channelVersion) {
        checkVersion(channelVersion);
        this.versionGt = channelVersion;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelVersionGreaterThanOrEquals(Integer channelVersion) {
        checkVersion(channelVersion);
        this.versionGte = channelVersion;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelVersionLowerThan(Integer channelVersion) {
        checkVersion(channelVersion);
        this.versionLt = channelVersion;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelVersionLowerThanOrEquals(Integer channelVersion) {
        checkVersion(channelVersion);
        this.versionLte = channelVersion;
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
    public ChannelDefinitionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }
    
    @Override
    public ChannelDefinitionQuery onlyInbound() {
        if (onlyOutbound) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyInbound() with onlyOutbound() in the same query");
        }
        this.onlyInbound = true;
        return this;
    }

    @Override
    public ChannelDefinitionQuery onlyOutbound() {
        if (onlyInbound) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyOutbound() with onlyInbound() in the same query");
        }
        this.onlyOutbound = true;
        return this;
    }

    @Override
    public ChannelDefinitionQuery implementation(String implementation) {
        if (implementation == null) {
            throw new FlowableIllegalArgumentException("implementation is null");
        }
        this.implementation = implementation;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }
    
    @Override
    public ChannelDefinitionQueryImpl channelCreateTimeAfter(Date createTimeAfter) {
        this.createTimeAfter = createTimeAfter;
        return this;
    }
    
    @Override
    public ChannelDefinitionQueryImpl channelCreateTimeBefore(Date createTimeBefore) {
        this.createTimeBefore = createTimeBefore;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl channelDefinitionResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("form tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl tenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("form tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public ChannelDefinitionQueryImpl withoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    @Override
    public ChannelDefinitionQuery orderByDeploymentId() {
        return orderBy(ChannelDefinitionQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public ChannelDefinitionQuery orderByChannelDefinitionKey() {
        return orderBy(ChannelDefinitionQueryProperty.KEY);
    }

    @Override
    public ChannelDefinitionQuery orderByChannelDefinitionCategory() {
        return orderBy(ChannelDefinitionQueryProperty.CATEGORY);
    }

    @Override
    public ChannelDefinitionQuery orderByChannelDefinitionId() {
        return orderBy(ChannelDefinitionQueryProperty.ID);
    }

    @Override
    public ChannelDefinitionQuery orderByChannelDefinitionName() {
        return orderBy(ChannelDefinitionQueryProperty.NAME);
    }
    
    @Override
    public ChannelDefinitionQuery orderByCreateTime() {
        return orderBy(ChannelDefinitionQueryProperty.CREATE_TIME);
    }

    @Override
    public ChannelDefinitionQuery orderByTenantId() {
        return orderBy(ChannelDefinitionQueryProperty.TENANT_ID);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getChannelDefinitionEntityManager(commandContext).findChannelDefinitionCountByQueryCriteria(this);
    }

    @Override
    public List<ChannelDefinition> executeList(CommandContext commandContext) {
        return CommandContextUtil.getChannelDefinitionEntityManager(commandContext).findChannelDefinitionsByQueryCriteria(this);
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

    public boolean isOnlyInbound() {
        return onlyInbound;
    }

    public boolean isOnlyOutbound() {
        return onlyOutbound;
    }

    public String getImplementation() {
        return implementation;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getCreateTimeAfter() {
        return createTimeAfter;
    }

    public Date getCreateTimeBefore() {
        return createTimeBefore;
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
