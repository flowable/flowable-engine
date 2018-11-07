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

import java.io.Serializable;
import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CmmnDeploymentQueryImpl extends AbstractQuery<CmmnDeploymentQuery, CmmnDeployment> implements CmmnDeploymentQuery, Serializable {

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
    protected String parentDeploymentId;
    protected String parentDeploymentIdLike;
    protected List<String> parentDeploymentIds;
    protected boolean latest;

    public CmmnDeploymentQueryImpl() {
    }

    public CmmnDeploymentQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public CmmnDeploymentQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }
    
    @Override
    public CmmnDeploymentQueryImpl deploymentIds(List<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("Deployment ids is null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentName(String deploymentName) {
        if (deploymentName == null) {
            throw new FlowableIllegalArgumentException("deploymentName is null");
        }
        this.name = deploymentName;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("deploymentNameLike is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentCategory(String deploymentCategory) {
        if (deploymentCategory == null) {
            throw new FlowableIllegalArgumentException("deploymentCategory is null");
        }
        this.category = deploymentCategory;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentCategoryNotEquals(String deploymentCategoryNotEquals) {
        if (deploymentCategoryNotEquals == null) {
            throw new FlowableIllegalArgumentException("deploymentCategoryExclude is null");
        }
        this.categoryNotEquals = deploymentCategoryNotEquals;
        return this;
    }
    
    @Override
    public CmmnDeploymentQueryImpl deploymentKey(String deploymentKey) {
        if (deploymentKey == null) {
            throw new FlowableIllegalArgumentException("deploymentKey is null");
        }
        this.key = deploymentKey;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl deploymentWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl parentDeploymentId(String parentDeploymentId) {
        if (parentDeploymentId == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentId is null");
        }
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public CmmnDeploymentQueryImpl parentDeploymentIdLike(String parentDeploymentIdLike) {
        if (parentDeploymentIdLike == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentIdLike is null");
        }
        this.parentDeploymentIdLike = parentDeploymentIdLike;
        return this;
    }
    
    @Override
    public CmmnDeploymentQueryImpl parentDeploymentIds(List<String> parentDeploymentIds) {
        if (parentDeploymentIds == null) {
            throw new FlowableIllegalArgumentException("parentDeploymentIds is null");
        }
        this.parentDeploymentIds = parentDeploymentIds;
        return this;
    }
    
    @Override
    public CmmnDeploymentQueryImpl latest() {
        if (key == null) {
            throw new FlowableIllegalArgumentException("latest can only be used together with a deployment key");
        }

        this.latest = true;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public CmmnDeploymentQuery orderByDeploymentId() {
        return orderBy(CmmnDeploymentQueryProperty.DEPLOYMENT_ID);
    }

    @Override
    public CmmnDeploymentQuery orderByDeploymenTime() {
        return orderBy(CmmnDeploymentQueryProperty.DEPLOY_TIME);
    }

    @Override
    public CmmnDeploymentQuery orderByDeploymentName() {
        return orderBy(CmmnDeploymentQueryProperty.DEPLOYMENT_NAME);
    }

    @Override
    public CmmnDeploymentQuery orderByTenantId() {
        return orderBy(CmmnDeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getCmmnDeploymentEntityManager(commandContext).findDeploymentCountByQueryCriteria(this);
    }

    @Override
    public List<CmmnDeployment> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getCmmnDeploymentEntityManager(commandContext).findDeploymentsByQueryCriteria(this);
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
