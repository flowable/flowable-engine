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

import java.io.Serializable;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DeploymentQueryImpl extends AbstractQuery<DeploymentQuery, Deployment> implements DeploymentQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected String name;
    protected String nameLike;
    protected String category;
    protected String categoryLike;
    protected String categoryNotEquals;
    protected String key;
    protected String keyLike;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String engineVersion;
    protected String derivedFrom;
    protected String processDefinitionKey;
    protected String processDefinitionKeyLike;
    protected boolean latest;

    public DeploymentQueryImpl() {
    }

    public DeploymentQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public DeploymentQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public DeploymentQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentIds(List<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("Deployment ids is null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentName(String deploymentName) {
        if (deploymentName == null) {
            throw new FlowableIllegalArgumentException("deploymentName is null");
        }
        this.name = deploymentName;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("deploymentNameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentCategory(String deploymentCategory) {
        if (deploymentCategory == null) {
            throw new FlowableIllegalArgumentException("deploymentCategory is null");
        }
        this.category = deploymentCategory;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentCategoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("deploymentCategoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentCategoryNotEquals(String deploymentCategoryNotEquals) {
        if (deploymentCategoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("deploymentCategoryExclude is null");
        }
        this.categoryNotEquals = deploymentCategoryNotEquals;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentKey(String deploymentKey) {
        if (deploymentKey == null) {
            throw new FlowableIllegalArgumentException("deploymentKey is null");
        }
        this.key = deploymentKey;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentKeyLike(String deploymentKeyLike) {
        if (deploymentKeyLike == null) {
            throw new FlowableIllegalArgumentException("deploymentKeyLike is null");
        }
        this.keyLike = deploymentKeyLike;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public DeploymentQueryImpl deploymentEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
        return this;
    }
    
    @Override
    public DeploymentQuery deploymentDerivedFrom(String deploymentId) {
        this.derivedFrom = deploymentId;
        return this;
    }

    @Override
    public DeploymentQueryImpl processDefinitionKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.processDefinitionKey = key;
        return this;
    }

    @Override
    public DeploymentQueryImpl processDefinitionKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.processDefinitionKeyLike = keyLike;
        return this;
    }

    @Override
    public DeploymentQueryImpl latest() {
        if (key == null) {
            throw new FlowableIllegalArgumentException("latest can only be used together with a deployment key");
        }

        this.latest = true;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public DeploymentQuery orderByDeploymentId() {
        return orderBy(DeploymentQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public DeploymentQuery orderByDeploymenTime() {
        return orderBy(DeploymentQueryProperty.DEPLOY_TIME);
    }

    @Override
    public DeploymentQuery orderByDeploymentName() {
        return orderBy(DeploymentQueryProperty.DEPLOYMENT_NAME);
    }

    @Override
    public DeploymentQuery orderByTenantId() {
        return orderBy(DeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getDeploymentEntityManager(commandContext).findDeploymentCountByQueryCriteria(this);
    }

    @Override
    public List<Deployment> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getDeploymentEntityManager(commandContext).findDeploymentsByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////////////////

    public String getDeploymentId() {
        return deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public String getCategory() {
        return category;
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
    
    public String getDerivedFrom() {
        return derivedFrom;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }
}
