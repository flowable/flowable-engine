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

import java.io.Serializable;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentQuery;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DmnDeploymentQueryImpl extends AbstractQuery<DmnDeploymentQuery, DmnDeployment> implements DmnDeploymentQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String deploymentId;
    protected String name;
    protected String nameLike;
    protected String category;
    protected String categoryNotEquals;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String parentDeploymentId;
    protected String parentDeploymentIdLike;
    protected String decisionTableKey;
    protected String decisionTableKeyLike;

    public DmnDeploymentQueryImpl() {
    }

    public DmnDeploymentQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public DmnDeploymentQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public DmnDeploymentQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentName(String deploymentName) {
        if (deploymentName == null) {
            throw new FlowableIllegalArgumentException("deploymentName is null");
        }
        this.name = deploymentName;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("deploymentNameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentCategory(String deploymentCategory) {
        if (deploymentCategory == null) {
            throw new FlowableIllegalArgumentException("deploymentCategory is null");
        }
        this.category = deploymentCategory;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentCategoryNotEquals(String deploymentCategoryNotEquals) {
        if (deploymentCategoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("deploymentCategoryExclude is null");
        }
        this.categoryNotEquals = deploymentCategoryNotEquals;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl deploymentWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl parentDeploymentId(String parentDeploymentId) {
        if (parentDeploymentId == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentId is null");
        }
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl parentDeploymentIdLike(String parentDeploymentIdLike) {
        if (parentDeploymentIdLike == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentIdLike is null");
        }
        this.parentDeploymentIdLike = parentDeploymentIdLike;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl decisionTableKey(String key) {
        if (key == null) {
            throw new FlowableIllegalArgumentException("key is null");
        }
        this.decisionTableKey = key;
        return this;
    }

    @Override
    public DmnDeploymentQueryImpl decisionTableKeyLike(String keyLike) {
        if (keyLike == null) {
            throw new FlowableIllegalArgumentException("keyLike is null");
        }
        this.decisionTableKeyLike = keyLike;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public DmnDeploymentQuery orderByDeploymentId() {
        return orderBy(DeploymentQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public DmnDeploymentQuery orderByDeploymenTime() {
        return orderBy(DeploymentQueryProperty.DEPLOY_TIME);
    }

    @Override
    public DmnDeploymentQuery orderByDeploymentName() {
        return orderBy(DeploymentQueryProperty.DEPLOYMENT_NAME);
    }

    @Override
    public DmnDeploymentQuery orderByTenantId() {
        return orderBy(DeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getDeploymentEntityManager(commandContext).findDeploymentCountByQueryCriteria(this);
    }

    @Override
    public List<DmnDeployment> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getDeploymentEntityManager(commandContext).findDeploymentsByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////////////////

    public String getDeploymentId() {
        return deploymentId;
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

    public String getDecisionTableKey() {
        return decisionTableKey;
    }

    public String getDecisionTableKeyLike() {
        return decisionTableKeyLike;
    }
}
