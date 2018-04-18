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
package org.flowable.dmn.engine.impl.repository;

import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DmnRepositoryServiceImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntityManager;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;

/**
 * @author Tijs Rademakers
 */
public class DmnDeploymentBuilderImpl implements DmnDeploymentBuilder, Serializable {

    private static final long serialVersionUID = 1L;
    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected transient DmnRepositoryServiceImpl repositoryService;
    protected transient DmnResourceEntityManager resourceEntityManager;

    protected DmnDeploymentEntity deployment;
    protected boolean isDmn20XsdValidationEnabled = true;
    protected boolean isDuplicateFilterEnabled;

    public DmnDeploymentBuilderImpl() {
        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        this.repositoryService = (DmnRepositoryServiceImpl) dmnEngineConfiguration.getDmnRepositoryService();
        this.deployment = dmnEngineConfiguration.getDeploymentEntityManager().create();
        this.resourceEntityManager = dmnEngineConfiguration.getResourceEntityManager();
    }

    @Override
    public DmnDeploymentBuilder addInputStream(String resourceName, InputStream inputStream) {
        if (inputStream == null) {
            throw new FlowableException("inputStream for resource '" + resourceName + "' is null");
        }

        byte[] bytes = null;
        try {
            bytes = IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            throw new FlowableException("could not get byte array from resource '" + resourceName + "'");
        }

        if (bytes == null) {
            throw new FlowableException("byte array for resource '" + resourceName + "' is null");
        }

        DmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(bytes);
        deployment.addResource(resource);
        return this;
    }

    @Override
    public DmnDeploymentBuilder addClasspathResource(String resource) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new FlowableException("resource '" + resource + "' not found");
        }
        return addInputStream(resource, inputStream);
    }

    @Override
    public DmnDeploymentBuilder addString(String resourceName, String text) {
        if (text == null) {
            throw new FlowableException("text is null");
        }

        DmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        try {
            resource.setBytes(text.getBytes(DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("Unable to get decision table bytes.", e);
        }
        deployment.addResource(resource);
        return this;
    }

    @Override
    public DmnDeploymentBuilder addDmnBytes(String resourceName, byte[] dmnBytes) {
        if (dmnBytes == null) {
            throw new FlowableException("dmn bytes is null");
        }

        DmnResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(dmnBytes);
        deployment.addResource(resource);
        return this;
    }

    @Override
    public DmnDeploymentBuilder addDmnModel(String resourceName, DmnDefinition dmnDefinition) {
        DmnXMLConverter dmnXMLConverter = new DmnXMLConverter();
        try {
            String dmn20Xml = new String(dmnXMLConverter.convertToXML(dmnDefinition), "UTF-8");
            addString(resourceName, dmn20Xml);
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("Error while transforming DMN model to xml: not UTF-8 encoded", e);
        }
        return this;
    }

    @Override
    public DmnDeploymentBuilder name(String name) {
        deployment.setName(name);
        return this;
    }

    @Override
    public DmnDeploymentBuilder category(String category) {
        deployment.setCategory(category);
        return this;
    }

    @Override
    public DmnDeploymentBuilder disableSchemaValidation() {
        this.isDmn20XsdValidationEnabled = false;
        return this;
    }

    @Override
    public DmnDeploymentBuilder tenantId(String tenantId) {
        deployment.setTenantId(tenantId);
        return this;
    }

    @Override
    public DmnDeploymentBuilder parentDeploymentId(String parentDeploymentId) {
        deployment.setParentDeploymentId(parentDeploymentId);
        return this;
    }

    @Override
    public DmnDeploymentBuilder enableDuplicateFiltering() {
        isDuplicateFilterEnabled = true;
        return this;
    }

    @Override
    public DmnDeployment deploy() {
        return repositoryService.deploy(this);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public DmnDeploymentEntity getDeployment() {
        return deployment;
    }

    public boolean isDmnXsdValidationEnabled() {
        return isDmn20XsdValidationEnabled;
    }

    public boolean isDuplicateFilterEnabled() {
        return isDuplicateFilterEnabled;
    }
}
