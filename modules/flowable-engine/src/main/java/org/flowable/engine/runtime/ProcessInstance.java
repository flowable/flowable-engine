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
package org.flowable.engine.runtime;

import java.util.Date;
import java.util.Map;

import org.flowable.engine.repository.ProcessDefinition;

/**
 * Represents one execution of a {@link ProcessDefinition}.
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Daniel Meyer
 * @author Tijs Rademakers
 */
public interface ProcessInstance extends Execution {

    /**
     * The id of the process definition of the process instance.
     */
    String getProcessDefinitionId();

    /**
     * The name of the process definition of the process instance.
     */
    String getProcessDefinitionName();

    /**
     * The key of the process definition of the process instance.
     */
    String getProcessDefinitionKey();

    /**
     * The version of the process definition of the process instance.
     */
    Integer getProcessDefinitionVersion();

    /**
     * The category of the process definition of the process instance.
     */
    String getProcessDefinitionCategory();

    /**
     * The deployment id of the process definition of the process instance.
     */
    String getDeploymentId();

    /**
     * The business key of this process instance.
     */
    String getBusinessKey();
    
    /**
     * The business status of this process instance.
     */
    String getBusinessStatus();

    /**
     * returns true if the process instance is suspended
     */
    @Override
    boolean isSuspended();

    /**
     * Returns the process variables if requested in the process instance query
     */
    Map<String, Object> getProcessVariables();

    /**
     * The tenant identifier of this process instance
     */
    @Override
    String getTenantId();

    /**
     * Returns the name of this process instance.
     */
    @Override
    String getName();

    /**
     * Returns the description of this process instance.
     */
    @Override
    String getDescription();

    /**
     * Returns the localized name of this process instance.
     */
    String getLocalizedName();

    /**
     * Returns the localized description of this process instance.
     */
    String getLocalizedDescription();

    /**
     * Returns the start time of this process instance.
     */
    Date getStartTime();

    /**
     * Returns the user id of this process instance.
     */
    String getStartUserId();
    
    /**
     * Returns the callback id of this process instance.
     */
    String getCallbackId();
    
    /**
     * Returns the callback type of this process instance. 
     */
    String getCallbackType();
}
