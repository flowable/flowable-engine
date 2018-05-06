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

package org.flowable.form.engine.impl;

import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormDefinitionQueryImpl extends AbstractQuery<FormDefinitionQuery, FormDefinition> implements FormDefinitionQuery {

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
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public FormDefinitionQueryImpl() {
    }

    public FormDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public FormDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public FormDefinitionQueryImpl formId(String formId) {
        this.id = formId;
        return this;
    }

    @Override
    public FormDefinitionQuery formIds(Set<String> formIds) {
        this.ids = formIds;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl deploymentIds(Set<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("ids are null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.keyLike = keyLike;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formResourceName(String resourceName) {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }
        this.resourceName = resourceName;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formResourceNameLike(String resourceNameLike) {
        if (resourceNameLike == null) {
            throw new FlowableIllegalArgumentException("resourceNameLike is null");
        }
        this.resourceNameLike = resourceNameLike;
        return this;
    }

    @Override
    public FormDefinitionQueryImpl formVersion(Integer version) {
        checkVersion(version);
        this.version = version;
        return this;
    }

    @Override
    public FormDefinitionQuery formVersionGreaterThan(Integer formVersion) {
        checkVersion(formVersion);
        this.versionGt = formVersion;
        return this;
    }

    @Override
    public FormDefinitionQuery formVersionGreaterThanOrEquals(Integer formVersion) {
        checkVersion(formVersion);
        this.versionGte = formVersion;
        return this;
    }

    @Override
    public FormDefinitionQuery formVersionLowerThan(Integer formVersion) {
        checkVersion(formVersion);
        this.versionLt = formVersion;
        return this;
    }

    @Override
    public FormDefinitionQuery formVersionLowerThanOrEquals(Integer formVersion) {
        checkVersion(formVersion);
        this.versionLte = formVersion;
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
    public FormDefinitionQueryImpl latestVersion() {
        this.latest = true;
        return this;
    }

    @Override
    public FormDefinitionQuery formTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("form tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public FormDefinitionQuery formTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("form tenantId is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public FormDefinitionQuery formWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    @Override
    public FormDefinitionQuery orderByDeploymentId() {
        return orderBy(FormQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public FormDefinitionQuery orderByFormDefinitionKey() {
        return orderBy(FormQueryProperty.FORM_DEFINITION_KEY);
    }

    @Override
    public FormDefinitionQuery orderByFormCategory() {
        return orderBy(FormQueryProperty.FORM_CATEGORY);
    }

    @Override
    public FormDefinitionQuery orderByFormId() {
        return orderBy(FormQueryProperty.FORM_ID);
    }

    @Override
    public FormDefinitionQuery orderByFormVersion() {
        return orderBy(FormQueryProperty.FORM_VERSION);
    }

    @Override
    public FormDefinitionQuery orderByFormName() {
        return orderBy(FormQueryProperty.FORM_NAME);
    }

    @Override
    public FormDefinitionQuery orderByTenantId() {
        return orderBy(FormQueryProperty.FORM_TENANT_ID);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getFormDefinitionEntityManager(commandContext).findFormDefinitionCountByQueryCriteria(this);
    }

    @Override
    public List<FormDefinition> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getFormDefinitionEntityManager(commandContext).findFormDefinitionsByQueryCriteria(this);
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
