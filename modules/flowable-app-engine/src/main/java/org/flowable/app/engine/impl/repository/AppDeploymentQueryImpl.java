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

import java.io.Serializable;
import java.util.List;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppDeploymentQuery;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class AppDeploymentQueryImpl extends AbstractQuery<AppDeploymentQuery, AppDeployment> implements AppDeploymentQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected String name;
    protected String nameLike;
    protected String category;
    protected String categoryNotEquals;
    protected String key;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean latest;

    public AppDeploymentQueryImpl() {
    }

    public AppDeploymentQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public AppDeploymentQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public AppDeploymentQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }
    
    @Override
    public AppDeploymentQueryImpl deploymentIds(List<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("Deployment ids is null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentName(String deploymentName) {
        if (deploymentName == null) {
            throw new FlowableIllegalArgumentException("deploymentName is null");
        }
        this.name = deploymentName;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("deploymentNameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentCategory(String deploymentCategory) {
        if (deploymentCategory == null) {
            throw new FlowableIllegalArgumentException("deploymentCategory is null");
        }
        this.category = deploymentCategory;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentCategoryNotEquals(String deploymentCategoryNotEquals) {
        if (deploymentCategoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("deploymentCategoryExclude is null");
        }
        this.categoryNotEquals = deploymentCategoryNotEquals;
        return this;
    }
    
    @Override
    public AppDeploymentQueryImpl deploymentKey(String deploymentKey) {
        if (deploymentKey == null) {
            throw new FlowableIllegalArgumentException("deploymentKey is null");
        }
        this.key = deploymentKey;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl deploymentWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public AppDeploymentQueryImpl latest() {
        if (key == null) {
            throw new FlowableIllegalArgumentException("latest can only be used together with a deployment key");
        }

        this.latest = true;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public AppDeploymentQuery orderByDeploymentId() {
        return orderBy(AppDeploymentQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public AppDeploymentQuery orderByDeploymenTime() {
        return orderBy(AppDeploymentQueryProperty.DEPLOY_TIME);
    }

    @Override
    public AppDeploymentQuery orderByDeploymentName() {
        return orderBy(AppDeploymentQueryProperty.DEPLOYMENT_NAME);
    }

    @Override
    public AppDeploymentQuery orderByTenantId() {
        return orderBy(AppDeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getAppDeploymentEntityManager(commandContext).findDeploymentCountByQueryCriteria(this);
    }

    @Override
    public List<AppDeployment> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getAppDeploymentEntityManager(commandContext).findDeploymentsByQueryCriteria(this);
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
    
    public String getKey() {
        return key;
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
