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
package org.flowable.app.api.repository;

import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.flowable.app.api.AppRepositoryService;

/**
 * Builder for creating new deployments, similar to the bpmn deployment builder.
 * 
 * A builder instance can be obtained through {@link AppRepositoryService#createDeployment()}.
 * Multiple resources can be added to one deployment before calling the {@link #deploy()} operation.
 * After deploying, no more changes can be made to the returned deployment and the builder instance can be disposed.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface AppDeploymentBuilder {

    AppDeploymentBuilder addInputStream(String resourceName, InputStream inputStream);

    AppDeploymentBuilder addClasspathResource(String resource);

    AppDeploymentBuilder addString(String resourceName, String text);
    
    AppDeploymentBuilder addBytes(String resourceName, byte[] bytes);
    
    AppDeploymentBuilder addZipInputStream(ZipInputStream zipInputStream);

    /**
     * If called, no XML schema validation against the XSD.
     * 
     * Not recommended in general.
     */
    AppDeploymentBuilder disableSchemaValidation();

    /**
     * Gives the deployment the given name.
     */
    AppDeploymentBuilder name(String name);

    /**
     * Gives the deployment the given category.
     */
    AppDeploymentBuilder category(String category);
    
    /**
     * Gives the deployment the given key.
     */
    AppDeploymentBuilder key(String key);

    /**
     * Gives the deployment the given tenant id.
     */
    AppDeploymentBuilder tenantId(String tenantId);
    
    /**
     * If set, this deployment will be compared to any previous deployment. This means that every (non-generated) resource will be compared with the provided resources of this deployment.
     */
    AppDeploymentBuilder enableDuplicateFiltering();

    /**
     * Deploys all provided sources to the CMMN engine.
     */
    AppDeployment deploy();

}
