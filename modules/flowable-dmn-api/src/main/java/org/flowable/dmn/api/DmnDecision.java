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
package org.flowable.dmn.api;

/**
 * An object structure representing an executable DMN decision
 * 
 * @author Tijs Rademakers
 * @author Joram Barez
 * @author Yvo Swillens
 */
public interface DmnDecision {

    /** unique identifier */
    String getId();

    /**
     * category name of this definition
     */
    String getCategory();

    /** label used for display purposes */
    String getName();

    /** unique name for all versions this definition */
    String getKey();

    /** description of this definition **/
    String getDescription();

    /** version of this definition */
    int getVersion();

    /**
     * name of {@link DmnRepositoryService#getResourceAsStream(String, String) the resource} of this definition.
     */
    String getResourceName();

    /**
     * Does this decision have a graphical notation defined (such that a diagram can be generated)?
     */
    boolean hasGraphicalNotation();

    /** The resource name in the deployment of the diagram image (if any). */
    String getDiagramResourceName();

    /** The deployment in which this definition is contained. */
    String getDeploymentId();

    /** The tenant identifier of this definition */
    String getTenantId();

    /** The decision type of this definition */
    String getDecisionType();

}
