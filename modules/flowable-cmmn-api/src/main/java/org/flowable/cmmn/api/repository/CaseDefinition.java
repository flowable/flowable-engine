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

import org.flowable.cmmn.api.CmmnRepositoryService;

public interface CaseDefinition {

    /** unique identifier */
    String getId();

    /**
     * category name which is derived from the targetNamespace attribute in the definitions element
     */
    String getCategory();

    /** label used for display purposes */
    String getName();

    /** unique name for all versions of this case definition */
    String getKey();

    /** description of this case definition **/
    String getDescription();

    /** version of this case definition */
    int getVersion();

    /**
     * name of {@link CmmnRepositoryService#getResourceAsStream(String, String) the resource} of this case definition.
     */
    String getResourceName();

    /** The deployment in which this case definition is contained. */
    String getDeploymentId();

    /** The resource name in the deployment of the diagram image (if any). */
    String getDiagramResourceName();

    /**
     * Does this case definition have a start form key}.
     */
    boolean hasStartFormKey();

    /**
     * Does this case definition have a graphical notation defined (such that a diagram can be generated)?
     */
    boolean hasGraphicalNotation();

    /** The tenant identifier of this case definition */
    String getTenantId();

}
