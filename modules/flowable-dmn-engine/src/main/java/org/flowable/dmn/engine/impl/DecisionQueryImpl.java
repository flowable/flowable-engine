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

package org.flowable.dmn.engine.impl;

import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDecisionQuery;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public class DecisionQueryImpl extends AbstractQuery<DmnDecisionQuery, DmnDecision> implements DmnDecisionQuery {

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
    protected String parentDeploymentId;
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
    protected String decisionId;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String decisionType;
    protected String decisionTypeLike;

    public DecisionQueryImpl() {
    }

    public DecisionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public DecisionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public DecisionQueryImpl decisionId(String decisionId) {
        this.id = decisionId;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionIds(Set<String> decisionIds) {
        this.ids = decisionIds;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public DecisionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public DecisionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public DecisionQueryImpl parentDeploymentId(String parentDeploymentId) {
        if (parentDeploymentId == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentId is null");
        }
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    @Override
    public DecisionQueryImpl decisionVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionVersionGreaterThan(Integer decisionVersion) {
        checkVersion(decisionVersion);
        this.versionGt = decisionVersion;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionVersionGreaterThanOrEquals(Integer decisionVersion) {
        checkVersion(decisionVersion);
        this.versionGte = decisionVersion;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionVersionLowerThan(Integer decisionVersion) {
        checkVersion(decisionVersion);
        this.versionLt = decisionVersion;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionVersionLowerThanOrEquals(Integer decisionVersion) {
        checkVersion(decisionVersion);
        this.versionLte = decisionVersion;
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
    public DecisionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("decision tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("decision tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionType(String decisionType) {
        this.decisionType = decisionType;
        return this;
    }

    @Override
    public DmnDecisionQuery decisionTypeLike(String decisionTypeLike) {
        this.decisionTypeLike = decisionTypeLike;
        return this;
    }

    // sorting ////////////////////////////////////////////

    @Override
    public DmnDecisionQuery orderByDeploymentId() {
        return orderBy(DecisionQueryProperty.DECISION_DEPLOYMENT_ID);
    }

    @Override
    public DmnDecisionQuery orderByDecisionKey() {
        return orderBy(DecisionQueryProperty.DECISION_KEY);
    }

    @Override
    public DmnDecisionQuery orderByDecisionCategory() {
        return orderBy(DecisionQueryProperty.DECISION_CATEGORY);
    }

    @Override
    public DmnDecisionQuery orderByDecisionId() {
        return orderBy(DecisionQueryProperty.DECISION_ID);
    }

    @Override
    public DmnDecisionQuery orderByDecisionVersion() {
        return orderBy(DecisionQueryProperty.DECISION_VERSION);
    }

    @Override
    public DmnDecisionQuery orderByDecisionName() {
        return orderBy(DecisionQueryProperty.DECISION_NAME);
    }

    @Override
    public DmnDecisionQuery orderByTenantId() {
        return orderBy(DecisionQueryProperty.DECISION_TENANT_ID);
    }

    @Override
    public DmnDecisionQuery orderByDecisionType() {
        return orderBy(DecisionQueryProperty.DECISION_TYPE);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getDecisionEntityManager(commandContext).findDecisionCountByQueryCriteria(this);
    }

    @Override
    public List<DmnDecision> executeList(CommandContext commandContext) {
        return CommandContextUtil.getDecisionEntityManager(commandContext).findDecisionsByQueryCriteria(this);
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

    public String getDecisionId() {
        return decisionId;
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

    public String getDecisionType() {
        return decisionType;
    }

    public String getDecisionTypeLike() {
        return decisionTypeLike;
    }
}
