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

/**
 * An object structure representing an event definition
 * 
 * @author Tijs Rademakers
 * @author Joram Barez
 */
public interface EventDefinition {

    /** unique identifier */
    String getId();

    /**
     * category name of the event definition
     */
    String getCategory();

    /** label used for display purposes */
    String getName();

    /** unique name for all versions this event definition */
    String getKey();
    
    /** version of this event definition */
    int getVersion();

    /** description of this event definition **/
    String getDescription();

    /**
     * name of {@link EventRepositoryService#getResourceAsStream(String, String) the resource} of this event definition.
     */
    String getResourceName();

    /** The deployment in which this form is contained. */
    String getDeploymentId();

    /** The tenant identifier of this form */
    String getTenantId();

}
