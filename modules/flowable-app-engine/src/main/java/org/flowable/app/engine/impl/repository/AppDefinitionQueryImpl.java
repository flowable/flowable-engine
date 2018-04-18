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

package org.flowable.app.engine.impl.repository;

import java.util.List;
import java.util.Set;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Tijs Rademakers
 */
public class AppDefinitionQueryImpl extends AbstractQuery<AppDefinitionQuery, AppDefinition> implements AppDefinitionQuery {

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
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public AppDefinitionQueryImpl() {
    }

    public AppDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public AppDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public AppDefinitionQueryImpl appDefinitionId(String appDefinitionId) {
        this.id = appDefinitionId;
        return this;
    }

    @Override
    public AppDefinitionQuery appDefinitionIds(Set<String> appsDefinitionIds) {
        if (appsDefinitionIds == null) {
            throw new FlowableIllegalArgumentException("appsDefinitionIds is null");
        } else if (appsDefinitionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Empty appsDefinitionIds");
        }
        this.ids = appsDefinitionIds;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    public AppDefinitionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    public AppDefinitionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        } else if (deploymentIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("ids is an empty collection");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    public AppDefinitionQueryImpl appDefinitionVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    public AppDefinitionQuery appDefinitionVersionGreaterThan(Integer appDefinitionVersion) {
        checkVersion(appDefinitionVersion);
        this.versionGt = appDefinitionVersion;
        return this;
    }

    public AppDefinitionQuery appDefinitionVersionGreaterThanOrEquals(Integer appDefinitionVersion) {
        checkVersion(appDefinitionVersion);
        this.versionGte = appDefinitionVersion;
        return this;
    }

    public AppDefinitionQuery appDefinitionVersionLowerThan(Integer appDefinitionVersion) {
        checkVersion(appDefinitionVersion);
        this.versionLt = appDefinitionVersion;
        return this;
    }

    public AppDefinitionQuery appDefinitionVersionLowerThanOrEquals(Integer appDefinitionVersion) {
        checkVersion(appDefinitionVersion);
        this.versionLte = appDefinitionVersion;
        return this;
    }

    protected void checkVersion(Integer version) {
        if (version == null) {
            throw new FlowableIllegalArgumentException("version is null");
        } else if (version <= 0) {
            throw new FlowableIllegalArgumentException("version must be positive");
        }
    }

    public AppDefinitionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    public AppDefinitionQuery appDefinitionTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("app definition tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    public AppDefinitionQuery appDefinitionTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("app definition tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    public AppDefinitionQuery appDefinitionWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    public AppDefinitionQuery orderByDeploymentId() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_DEPLOYMENT_ID);
    }

    public AppDefinitionQuery orderByAppDefinitionKey() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_KEY);
    }

    public AppDefinitionQuery orderByAppDefinitionCategory() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_CATEGORY);
    }

    public AppDefinitionQuery orderByAppDefinitionId() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_ID);
    }

    public AppDefinitionQuery orderByAppDefinitionVersion() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_VERSION);
    }

    public AppDefinitionQuery orderByAppDefinitionName() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_NAME);
    }

    public AppDefinitionQuery orderByTenantId() {
        return orderBy(AppDefinitionQueryProperty.APP_DEFINITION_TENANT_ID);
    }

    // results ////////////////////////////////////////////

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getAppDefinitionEntityManager(commandContext).findAppDefinitionCountByQueryCriteria(this);
    }

    public List<AppDefinition> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getAppDefinitionEntityManager(commandContext).findAppDefinitionsByQueryCriteria(this);
    }

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
