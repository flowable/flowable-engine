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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.AppRepositoryServiceImpl;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntity;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntity;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntityManager;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.IoUtil;

public class AppDeploymentBuilderImpl implements AppDeploymentBuilder {

    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected transient AppRepositoryServiceImpl repositoryService;
    protected transient AppResourceEntityManager resourceEntityManager;

    protected AppDeploymentEntity deployment;
    protected boolean isXsdValidationEnabled = true;
    protected boolean isDuplicateFilterEnabled;

    public AppDeploymentBuilderImpl() {
        AppEngineConfiguration appEngineConfiguration = CommandContextUtil.getAppEngineConfiguration();
        this.repositoryService = (AppRepositoryServiceImpl) appEngineConfiguration.getAppRepositoryService();
        this.deployment = appEngineConfiguration.getAppDeploymentEntityManager().create();
        this.resourceEntityManager = appEngineConfiguration.getAppResourceEntityManager();
    }

    public AppDeploymentBuilder addInputStream(String resourceName, InputStream inputStream) {
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

        AppResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(bytes);
        deployment.addResource(resource);
        return this;
    }

    public AppDeploymentBuilder addClasspathResource(String resource) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new FlowableException("resource '" + resource + "' not found");
        }
        return addInputStream(resource, inputStream);
    }

    public AppDeploymentBuilder addString(String resourceName, String text) {
        if (text == null) {
            throw new FlowableException("text is null");
        }

        AppResourceEntity resource = resourceEntityManager.create();
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
    public AppDeploymentBuilder addBytes(String resourceName, byte[] bytes) {
        if (bytes == null) {
            throw new FlowableException("bytes array is null");
        }

        AppResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(bytes);
        deployment.addResource(resource);
        return this;
    }

    public AppDeploymentBuilder name(String name) {
        deployment.setName(name);
        return this;
    }

    public AppDeploymentBuilder category(String category) {
        deployment.setCategory(category);
        return this;
    }
    
    public AppDeploymentBuilder key(String key) {
        deployment.setKey(key);
        return this;
    }

    public AppDeploymentBuilder disableSchemaValidation() {
        this.isXsdValidationEnabled = false;
        return this;
    }

    public AppDeploymentBuilder tenantId(String tenantId) {
        deployment.setTenantId(tenantId);
        return this;
    }

    @Override
    public AppDeploymentBuilder enableDuplicateFiltering() {
        this.isDuplicateFilterEnabled = true;
        return this;
    }

    public AppDeployment deploy() {
        return repositoryService.deploy(this);
    }

    public AppDeploymentEntity getDeployment() {
        return deployment;
    }

    public boolean isXsdValidationEnabled() {
        return isXsdValidationEnabled;
    }
    
    public boolean isDuplicateFilterEnabled() {
        return isDuplicateFilterEnabled;
    }

}
