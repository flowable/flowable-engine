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
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDecisionTableQuery;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DecisionTableQueryImpl extends AbstractQuery<DmnDecisionTableQuery, DmnDecisionTable> implements DmnDecisionTableQuery {

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
    protected String decisionTableId;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public DecisionTableQueryImpl() {
    }

    public DecisionTableQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public DecisionTableQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public DecisionTableQueryImpl decisionTableId(String decisionTableId) {
        this.id = decisionTableId;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableIds(Set<String> decisionTableIds) {
        this.ids = decisionTableIds;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public DecisionTableQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public DecisionTableQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    @Override
    public DecisionTableQueryImpl decisionTableVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableVersionGreaterThan(Integer decisionTableVersion) {
        checkVersion(decisionTableVersion);
        this.versionGt = decisionTableVersion;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableVersionGreaterThanOrEquals(Integer decisionTableVersion) {
        checkVersion(decisionTableVersion);
        this.versionGte = decisionTableVersion;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableVersionLowerThan(Integer decisionTableVersion) {
        checkVersion(decisionTableVersion);
        this.versionLt = decisionTableVersion;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableVersionLowerThanOrEquals(Integer decisionTableVersion) {
        checkVersion(decisionTableVersion);
        this.versionLte = decisionTableVersion;
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
    public DecisionTableQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("decision table tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("decision table tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public DmnDecisionTableQuery decisionTableWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    @Override
    public DmnDecisionTableQuery orderByDeploymentId() {
        return orderBy(DecisionTableQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public DmnDecisionTableQuery orderByDecisionTableKey() {
        return orderBy(DecisionTableQueryProperty.DECISION_TABLE_KEY);
    }

    @Override
    public DmnDecisionTableQuery orderByDecisionTableCategory() {
        return orderBy(DecisionTableQueryProperty.DECISION_TABLE_CATEGORY);
    }

    @Override
    public DmnDecisionTableQuery orderByDecisionTableId() {
        return orderBy(DecisionTableQueryProperty.DECISION_TABLE_ID);
    }

    @Override
    public DmnDecisionTableQuery orderByDecisionTableVersion() {
        return orderBy(DecisionTableQueryProperty.DECISION_TABLE_VERSION);
    }

    @Override
    public DmnDecisionTableQuery orderByDecisionTableName() {
        return orderBy(DecisionTableQueryProperty.DECISION_TABLE_NAME);
    }

    @Override
    public DmnDecisionTableQuery orderByTenantId() {
        return orderBy(DecisionTableQueryProperty.DECISION_TABLE_TENANT_ID);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getDecisionTableEntityManager(commandContext).findDecisionTableCountByQueryCriteria(this);
    }

    @Override
    public List<DmnDecisionTable> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getDecisionTableEntityManager(commandContext).findDecisionTablesByQueryCriteria(this);
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
