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
package org.flowable.cmmn.api.repository;

import java.io.InputStream;

import org.flowable.cmmn.api.CmmnRepositoryService;

/**
 * Builder for creating new deployments, similar to the bpmn deployment builder.
 * 
 * A builder instance can be obtained through {@link CmmnRepositoryService#createDeployment()}.
 * Multiple resources can be added to one deployment before calling the {@link #deploy()} operation.
 * After deploying, no more changes can be made to the returned deployment and the builder instance can be disposed.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface CmmnDeploymentBuilder {

    CmmnDeploymentBuilder addInputStream(String resourceName, InputStream inputStream);

    CmmnDeploymentBuilder addClasspathResource(String resource);

    CmmnDeploymentBuilder addString(String resourceName, String text);
    
    CmmnDeploymentBuilder addBytes(String resourceName, byte[] bytes);

    /**
     * If called, no XML schema validation against the BPMN 2.0 XSD.
     * 
     * Not recommended in general.
     */
    CmmnDeploymentBuilder disableSchemaValidation();

    /**
     * Gives the deployment the given name.
     */
    CmmnDeploymentBuilder name(String name);

    /**
     * Gives the deployment the given category.
     */
    CmmnDeploymentBuilder category(String category);
    
    /**
     * Gives the deployment the given key.
     */
    CmmnDeploymentBuilder key(String key);

    /**
     * Gives the deployment the given tenant id.
     */
    CmmnDeploymentBuilder tenantId(String tenantId);

    /**
     * Gives the deployment the given parent deployment id.
     */
    CmmnDeploymentBuilder parentDeploymentId(String parentDeploymentId);
    
    /**
     * If set, this deployment will be compared to any previous deployment. This means that every (non-generated) resource will be compared with the provided resources of this deployment.
     */
    CmmnDeploymentBuilder enableDuplicateFiltering();

    /**
     * Deploys all provided sources to the CMMN engine.
     */
    CmmnDeployment deploy();

}
