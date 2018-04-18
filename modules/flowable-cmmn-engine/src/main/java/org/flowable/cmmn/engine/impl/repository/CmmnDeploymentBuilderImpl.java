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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.CmmnRepositoryServiceImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.IoUtil;

public class CmmnDeploymentBuilderImpl implements CmmnDeploymentBuilder {

    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected transient CmmnRepositoryServiceImpl repositoryService;
    protected transient CmmnResourceEntityManager resourceEntityManager;

    protected CmmnDeploymentEntity deployment;
    protected boolean isCmmn20XsdValidationEnabled = true;
    protected boolean isDuplicateFilterEnabled;

    public CmmnDeploymentBuilderImpl() {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        this.repositoryService = (CmmnRepositoryServiceImpl) cmmnEngineConfiguration.getCmmnRepositoryService();
        this.deployment = cmmnEngineConfiguration.getCmmnDeploymentEntityManager().create();
        this.resourceEntityManager = cmmnEngineConfiguration.getCmmnResourceEntityManager();
    }

    public CmmnDeploymentBuilder addInputStream(String resourceName, InputStream inputStream) {
        if (inputStream == null) {
            throw new FlowableException("inputStream for resource '" + resourceName + "' is null");
        }

        byte[] bytes = null;
        try {
            bytes = IoUtil.readInputStream(inputStream, resourceName);
        } catch (Exception e) {
            throw new FlowableException("could not get byte array from resource '" + resourceName + "'");
        }

        if (bytes == null) {
            throw new FlowableException("byte array for resource '" + resourceName + "' is null");
        }

        CmmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(bytes);
        deployment.addResource(resource);
        return this;
    }

    public CmmnDeploymentBuilder addClasspathResource(String resource) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new FlowableException("resource '" + resource + "' not found");
        }
        return addInputStream(resource, inputStream);
    }

    public CmmnDeploymentBuilder addString(String resourceName, String text) {
        if (text == null) {
            throw new FlowableException("text is null");
        }

        CmmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        try {
            resource.setBytes(text.getBytes(DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("Unable to get bytes.", e);
        }
        deployment.addResource(resource);
        return this;
    }
    
    @Override
    public CmmnDeploymentBuilder addBytes(String resourceName, byte[] bytes) {
        if (bytes == null) {
            throw new FlowableException("bytes array is null");
        }

        CmmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(bytes);
        deployment.addResource(resource);
        return this;
    }

    public CmmnDeploymentBuilder addCmmnBytes(String resourceName, byte[] cmmnBytes) {
        if (cmmnBytes == null) {
            throw new FlowableException("cmmn bytes is null");
        }

        CmmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(cmmnBytes);
        deployment.addResource(resource);
        return this;
    }

    public CmmnDeploymentBuilder addCmmnModel(String resourceName, CmmnModel cmmnModel) {
        // TODO
        return null;
    }

    public CmmnDeploymentBuilder name(String name) {
        deployment.setName(name);
        return this;
    }

    public CmmnDeploymentBuilder category(String category) {
        deployment.setCategory(category);
        return this;
    }
    
    public CmmnDeploymentBuilder key(String key) {
        deployment.setKey(key);
        return this;
    }

    public CmmnDeploymentBuilder disableSchemaValidation() {
        this.isCmmn20XsdValidationEnabled = false;
        return this;
    }

    public CmmnDeploymentBuilder tenantId(String tenantId) {
        deployment.setTenantId(tenantId);
        return this;
    }

    public CmmnDeploymentBuilder parentDeploymentId(String parentDeploymentId) {
        deployment.setParentDeploymentId(parentDeploymentId);
        return this;
    }
    
    @Override
    public CmmnDeploymentBuilder enableDuplicateFiltering() {
        this.isDuplicateFilterEnabled = true;
        return this;
    }

    public CmmnDeployment deploy() {
        return repositoryService.deploy(this);
    }

    public CmmnDeploymentEntity getDeployment() {
        return deployment;
    }

    public boolean isCmmnXsdValidationEnabled() {
        return isCmmn20XsdValidationEnabled;
    }
    
    public boolean isDuplicateFilterEnabled() {
        return isDuplicateFilterEnabled;
    }

}
