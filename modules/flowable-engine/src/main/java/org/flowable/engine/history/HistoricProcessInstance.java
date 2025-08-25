/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.history;

import java.util.Date;
import java.util.Map;

import org.flowable.engine.IdentityService;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * A single execution of a whole process definition that is stored permanently.
 * 
 * @author Christian Stettler
 */
public interface HistoricProcessInstance {

    /**
     * The process instance id (== as the id for the runtime {@link ProcessInstance process instance}).
     */
    String getId();

    /** The user provided unique reference to this process instance. */
    String getBusinessKey();
    
    /** The user provided business status for this process instance. */
    String getBusinessStatus();

    /** The process definition reference. */
    String getProcessDefinitionId();

    /** The name of the process definition of the process instance. */
    String getProcessDefinitionName();

    /** The key of the process definition of the process instance. */
    String getProcessDefinitionKey();

    /** The version of the process definition of the process instance. */
    Integer getProcessDefinitionVersion();

    /**
     * The category of the process definition of the process instance.
     */
    String getProcessDefinitionCategory();

    /**
     * The deployment id of the process definition of the process instance.
     */
    String getDeploymentId();

    /** The time the process was started. */
    Date getStartTime();

    /** The time the process was ended. */
    Date getEndTime();

    /**
     * The difference between {@link #getEndTime()} and {@link #getStartTime()} .
     */
    Long getDurationInMillis();

    /**
     * Reference to the activity in which this process instance ended. Note that a process instance can have multiple end events, in this case it might not be deterministic which activity id will be
     * referenced here. Use a {@link HistoricActivityInstanceQuery} instead to query for end events of the process instance (use the activityTYpe attribute)
     */
    String getEndActivityId();

    /**
     * The authenticated user that started this process instance.
     * 
     * @see IdentityService#setAuthenticatedUserId(String)
     */
    String getStartUserId();

    /**
     * The state of this process instance.
     *
     * @see IdentityService#setAuthenticatedUserId(String)
     */
    String getState();

    /**
     * The authenticated user that ended this process instance.
     *
     */
    String getEndUserId();

    /** The start activity. */
    String getStartActivityId();

    /** Obtains the reason for the process instance's deletion. */
    String getDeleteReason();

    /**
     * The process instance id of a potential super process instance or null if no super process instance exists
     */
    String getSuperProcessInstanceId();

    /**
     * The tenant identifier for the process instance.
     */
    String getTenantId();

    /**
     * The name for the process instance.
     */
    String getName();

    /**
     * The description for the process instance.
     */
    String getDescription();
    
    /**
     * The callback id for the process instance. 
     */
    String getCallbackId();
    
    /**
     * The callback type for the process instance.
     */
    String getCallbackType();

    /**
     * The reference id for the process instance.
     */
    String getReferenceId();

    /**
     * The reference type for the process instance.
     */
    String getReferenceType();

    /**
     * If this process instance runs in the context of a case and stage, this method returns it's closest parent stage instance id
     * (the stage plan item instance id to be precise).
     *
     * @return the stage instance id this process instance belongs to or null, if it is not part of a case at all or is not a child element of a stage
     */
    String getPropagatedStageInstanceId();

    /** Returns the process variables if requested in the process instance query */
    Map<String, Object> getProcessVariables();
}
