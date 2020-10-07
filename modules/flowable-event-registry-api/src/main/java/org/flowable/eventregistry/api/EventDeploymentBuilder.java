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
package org.flowable.eventregistry.api;

import java.io.InputStream;

/**
 * Builder for creating new deployments.
 * 
 * A builder instance can be obtained through {@link org.flowable.eventregistry.api.EventRepositoryService#createDeployment()}.
 * 
 * Multiple resources can be added to one deployment before calling the {@link #deploy()} operation.
 * 
 * After deploying, no more changes can be made to the returned deployment and the builder instance can be disposed.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface EventDeploymentBuilder {

    EventDeploymentBuilder addInputStream(String resourceName, InputStream inputStream);

    EventDeploymentBuilder addClasspathResource(String resource);

    EventDeploymentBuilder addString(String resourceName, String text);

    EventDeploymentBuilder addEventDefinitionBytes(String resourceName, byte[] eventBytes);

    EventDeploymentBuilder addEventDefinition(String resourceName, String eventDefinition);
    
    EventDeploymentBuilder addChannelDefinitionBytes(String resourceName, byte[] channelBytes);

    EventDeploymentBuilder addChannelDefinition(String resourceName, String channelDefinition);

    /**
     * Gives the deployment the given name.
     */
    EventDeploymentBuilder name(String name);

    /**
     * Gives the deployment the given category.
     */
    EventDeploymentBuilder category(String category);

    /**
     * Gives the deployment the given tenant id.
     */
    EventDeploymentBuilder tenantId(String tenantId);

    /**
     * Gives the deployment the given parent deployment id.
     */
    EventDeploymentBuilder parentDeploymentId(String parentDeploymentId);

    /**
     * Allows to add a property to the deployment builder that influences the deployment.
     */
    EventDeploymentBuilder enableDuplicateFiltering();

    /**
     * Deploys all provided sources to the Flowable engine.
     */
    EventDeployment deploy();

}
