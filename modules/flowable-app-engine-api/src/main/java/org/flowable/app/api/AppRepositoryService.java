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
package org.flowable.app.api;

import java.io.InputStream;
import java.util.List;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.api.repository.AppDeploymentQuery;
import org.flowable.app.api.repository.AppModel;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;

public interface AppRepositoryService {

    /** Starts creating a new deployment */
    AppDeploymentBuilder createDeployment();

    /**
     * Retrieves a list of deployment resources for the given deployment, ordered alphabetically.
     * 
     * @param deploymentId
     *            id of the deployment, cannot be null.
     */
    List<String> getDeploymentResourceNames(String deploymentId);

    /**
     * Gives access to a deployment resource through a stream of bytes.
     * 
     * @param deploymentId
     *            id of the deployment, cannot be null.
     * @param resourceName
     *            name of the resource, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the resource doesn't exist in the given deployment or when no deployment exists for the given deploymentId.
     */
    InputStream getResourceAsStream(String deploymentId, String resourceName);
    
    /**
     * Returns the {@link AppModel} including all App model info.
     */
    AppModel getAppModel(String appDefinitionId);
    
    /**
     * Returns the {@link AppModel} as a JSON string.
     */
    String convertAppModelToJson(String appDefinitionId);
    
    /**
     * Returns the {@link AppDefinition} including all App information like additional Properties (e.g. documentation).
     */
    AppDefinition getAppDefinition(String appDefinitionId);

    /**
     * Deletes the given deployment and cascade deletion to case instances, history case instances and jobs.
     * 
     * @param deploymentId
     *            id of the deployment, cannot be null.
     */
    void deleteDeployment(String deploymentId, boolean cascade);
    
    /** Query deployments */
    AppDeploymentQuery createDeploymentQuery();
    
    /** Query case definitions */
    AppDefinitionQuery createAppDefinitionQuery();
    
    /**
     * Sets the category of the case definition. App definitions can be queried by category: see {@link AppDefinitionQuery#appDefinitionCategory(String)}.
     * 
     * @throws FlowableObjectNotFoundException
     *             if no app definition with the provided id can be found.
     */
    void setAppDefinitionCategory(String appDefinitionId, String category);
}
