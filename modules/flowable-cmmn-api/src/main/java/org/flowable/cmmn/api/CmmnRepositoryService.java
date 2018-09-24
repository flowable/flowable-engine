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
package org.flowable.cmmn.api;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.form.api.FormDefinition;

public interface CmmnRepositoryService {

    /** Starts creating a new deployment */
    CmmnDeploymentBuilder createDeployment();

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
     * Returns the {@link CaseDefinition} including all CMMN information like additional Properties (e.g. documentation).
     */
    CaseDefinition getCaseDefinition(String caseDefinitionId);

    /**
     * Gives access to a deployed case model, e.g., a CMMN 1.1 XML file, through a stream of bytes.
     * 
     * @param caseDefinitionId
     *            id of a {@link CaseDefinition}, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the case model doesn't exist.
     */
    CmmnModel getCmmnModel(String caseDefinitionId);
    
    /**
     * Gives access to a deployed case diagram, e.g., a PNG image, through a stream of bytes.
     * 
     * @param caseDefinitionId
     *            id of a {@link CaseDefinition}, cannot be null.
     * @return null when the diagram resource name of a {@link CaseDefinition} is null.
     * @throws FlowableObjectNotFoundException
     *             when the case diagram doesn't exist.
     */
    InputStream getCaseDiagram(String caseDefinitionId);
    
    /**
     * Deletes the given deployment and cascade deletion to case instances, history case instances and jobs.
     * 
     * @param deploymentId
     *            id of the deployment, cannot be null.
     */
    void deleteDeployment(String deploymentId, boolean cascade);
    
    /** Query deployments */
    CmmnDeploymentQuery createDeploymentQuery();
    
    /** Query case definitions */
    CaseDefinitionQuery createCaseDefinitionQuery();
    
    /**
     * Sets the category of the case definition. Case definitions can be queried by category: see {@link CaseDefinitionQuery#caseDefinitionCategory(String)}.
     * 
     * @throws FlowableObjectNotFoundException
     *             if no case definition with the provided id can be found.
     */
    void setCaseDefinitionCategory(String caseDefinitionId, String category);
    
    /**
     * Changes the parent deployment id of a deployment. This is used to move deployments to a different app deployment parent.
     * 
     * @param deploymentId
     *              The id of the deployment of which the parent deployment identifier will be changed.
     * @param newParentDeploymentId
     *              The new parent deployment identifier.
     */
    void changeDeploymentParentDeploymentId(String deploymentId, String newParentDeploymentId);
    
    /**
     * Retrieves the {@link DmnDecisionTable}s associated with the given case definition.
     *
     * @param caseDefinitionId
     *            id of the case definition, cannot be null.
     *
     */
    List<DmnDecisionTable> getDecisionTablesForCaseDefinition(String caseDefinitionId);

    /**
     * Retrieves the {@link FormDefinition}s associated with the given case definition.
     *
     * @param caseDefinitionId
     *            id of the case definition, cannot be null.
     *
     */
    List<FormDefinition> getFormDefinitionsForCaseDefinition(String caseDefinitionId);
}
