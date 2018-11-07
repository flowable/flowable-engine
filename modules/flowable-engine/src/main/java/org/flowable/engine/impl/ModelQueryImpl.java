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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ModelQueryImpl extends AbstractQuery<ModelQuery, Model> implements ModelQuery {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected String category;
    protected String categoryLike;
    protected String categoryNotEquals;
    protected String name;
    protected String nameLike;
    protected String key;
    protected Integer version;
    protected boolean latest;
    protected String deploymentId;
    protected boolean notDeployed;
    protected boolean deployed;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public ModelQueryImpl() {
    }

    public ModelQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ModelQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public ModelQueryImpl modelId(String modelId) {
        this.id = modelId;
        return this;
    }

    @Override
    public ModelQueryImpl modelCategory(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public ModelQueryImpl modelCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public ModelQueryImpl modelCategoryNotEquals(String categoryNotEquals) {
        if (categoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("categoryNotEquals is null");
        }
        this.categoryNotEquals = categoryNotEquals;
        return this;
    }

    @Override
    public ModelQueryImpl modelName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public ModelQueryImpl modelNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("nameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public ModelQuery modelKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.key = key;
        return this;
    }

    @Override
    public ModelQueryImpl modelVersion(Integer version) {
        if (version == null) {
            throw new FlowableIllegalArgumentException("version is null");
        } else if (version <= 0) {
            throw new FlowableIllegalArgumentException("version must be positive");
        }
        this.version = version;
        return this;
    }

    @Override
    public ModelQuery latestVersion() {
        this.latest = true;
        return this;
    }

    @Override
    public ModelQuery deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("DeploymentId is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public ModelQuery notDeployed() {
        if (deployed) {
            throw new FlowableIllegalArgumentException("Invalid usage: cannot use deployed() and notDeployed() in the same query");
        }
        this.notDeployed = true;
        return this;
    }

    @Override
    public ModelQuery deployed() {
        if (notDeployed) {
            throw new FlowableIllegalArgumentException("Invalid usage: cannot use deployed() and notDeployed() in the same query");
        }
        this.deployed = true;
        return this;
    }

    @Override
    public ModelQuery modelTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Model tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ModelQuery modelTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Model tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public ModelQuery modelWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////

    @Override
    public ModelQuery orderByModelCategory() {
        return orderBy(ModelQueryProperty.MODEL_CATEGORY);
    }

    @Override
    public ModelQuery orderByModelId() {
        return orderBy(ModelQueryProperty.MODEL_ID);
    }

    @Override
    public ModelQuery orderByModelKey() {
        return orderBy(ModelQueryProperty.MODEL_KEY);
    }

    @Override
    public ModelQuery orderByModelVersion() {
        return orderBy(ModelQueryProperty.MODEL_VERSION);
    }

    @Override
    public ModelQuery orderByModelName() {
        return orderBy(ModelQueryProperty.MODEL_NAME);
    }

    @Override
    public ModelQuery orderByCreateTime() {
        return orderBy(ModelQueryProperty.MODEL_CREATE_TIME);
    }

    @Override
    public ModelQuery orderByLastUpdateTime() {
        return orderBy(ModelQueryProperty.MODEL_LAST_UPDATE_TIME);
    }

    @Override
    public ModelQuery orderByTenantId() {
        return orderBy(ModelQueryProperty.MODEL_TENANT_ID);
    }

    // results ////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getModelEntityManager(commandContext).findModelCountByQueryCriteria(this);
    }

    @Override
    public List<Model> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getModelEntityManager(commandContext).findModelsByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public Integer getVersion() {
        return version;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryLike() {
        return categoryLike;
    }

    public String getCategoryNotEquals() {
        return categoryNotEquals;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getKey() {
        return key;
    }

    public boolean isLatest() {
        return latest;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public boolean isNotDeployed() {
        return notDeployed;
    }

    public boolean isDeployed() {
        return deployed;
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
