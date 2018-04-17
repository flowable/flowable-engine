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

package org.flowable.cmmn.engine.impl.repository;

import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Joram Barrez
 */
public class CaseDefinitionQueryImpl extends AbstractQuery<CaseDefinitionQuery, CaseDefinition> implements CaseDefinitionQuery {

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

    public CaseDefinitionQueryImpl() {
    }

    public CaseDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public CaseDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public CaseDefinitionQueryImpl caseDefinitionId(String caseDefinitionId) {
        this.id = caseDefinitionId;
        return this;
    }

    @Override
    public CaseDefinitionQuery caseDefinitionIds(Set<String> caseDefinitionIds) {
        if (caseDefinitionIds == null) {
            throw new FlowableIllegalArgumentException("caseDefinitionIds is null");
        } else if (caseDefinitionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Empty caseDefinitionIds");
        }
        this.ids = caseDefinitionIds;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    public CaseDefinitionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    public CaseDefinitionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        } else if (deploymentIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("ids is an empty collection");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    public CaseDefinitionQueryImpl caseDefinitionVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionVersionGreaterThan(Integer caseDefinitionVersion) {
        checkVersion(caseDefinitionVersion);
        this.versionGt = caseDefinitionVersion;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionVersionGreaterThanOrEquals(Integer caseDefinitionVersion) {
        checkVersion(caseDefinitionVersion);
        this.versionGte = caseDefinitionVersion;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionVersionLowerThan(Integer caseDefinitionVersion) {
        checkVersion(caseDefinitionVersion);
        this.versionLt = caseDefinitionVersion;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionVersionLowerThanOrEquals(Integer caseDefinitionVersion) {
        checkVersion(caseDefinitionVersion);
        this.versionLte = caseDefinitionVersion;
        return this;
    }

    protected void checkVersion(Integer version) {
        if (version == null) {
            throw new FlowableIllegalArgumentException("version is null");
        } else if (version <= 0) {
            throw new FlowableIllegalArgumentException("version must be positive");
        }
    }

    public CaseDefinitionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("caseDefinition tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("case definition tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    public CaseDefinitionQuery caseDefinitionWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    public CaseDefinitionQuery orderByDeploymentId() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_DEPLOYMENT_ID);
    }

    public CaseDefinitionQuery orderByCaseDefinitionKey() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_KEY);
    }

    public CaseDefinitionQuery orderByCaseDefinitionCategory() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_CATEGORY);
    }

    public CaseDefinitionQuery orderByCaseDefinitionId() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_ID);
    }

    public CaseDefinitionQuery orderByCaseDefinitionVersion() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_VERSION);
    }

    public CaseDefinitionQuery orderByCaseDefinitionName() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_NAME);
    }

    public CaseDefinitionQuery orderByTenantId() {
        return orderBy(CaseDefinitionQueryProperty.CASE_DEFINITION_TENANT_ID);
    }

    // results ////////////////////////////////////////////

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getCaseDefinitionEntityManager(commandContext).findCaseDefinitionCountByQueryCriteria(this);
    }

    public List<CaseDefinition> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getCaseDefinitionEntityManager(commandContext).findCaseDefinitionsByQueryCriteria(this);
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
