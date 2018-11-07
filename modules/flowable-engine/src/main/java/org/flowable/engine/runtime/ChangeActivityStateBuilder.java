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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;

/**
 * Helper for changing the state of a process instance.
 * 
 * An instance can be obtained through {@link org.flowable.engine.RuntimeService#createChangeActivityStateBuilder()}.
 * 
 * @author Tijs Rademakers
 */
public interface ChangeActivityStateBuilder {

    /**
     * Set the id of the process instance
     **/
    ChangeActivityStateBuilder processInstanceId(String processInstanceId);
    
    /**
     * Set the id of the execution for which the activity should be changed
     **/
    ChangeActivityStateBuilder moveExecutionToActivityId(String executionId, String activityId);
    
    /**
     * Set the ids of the executions which should be changed to a single execution with the provided activity id.
     * This can be used for parallel execution like parallel/inclusive gateways, multiinstance, event sub processes etc.
     **/
    ChangeActivityStateBuilder moveExecutionsToSingleActivityId(List<String> executionIds, String activityId);
    
    /**
     * Set the id of an execution which should be changed to multiple executions with the provided activity ids.
     * This can be used for parallel execution like parallel/inclusive gateways, multiinstance, event sub processes etc.
     **/
    ChangeActivityStateBuilder moveSingleExecutionToActivityIds(String executionId, List<String> activityId);

    /**
     * Moves the execution with the current activity id to the provided new activity id
     */
    ChangeActivityStateBuilder moveActivityIdTo(String currentActivityId, String newActivityId);
    
    /**
     * Set the activity ids that should be changed to a single activity id.
     * This can be used for parallel execution like parallel/inclusive gateways, multiinstance, event sub processes etc.
     */
    ChangeActivityStateBuilder moveActivityIdsToSingleActivityId(List<String> currentActivityIds, String newActivityId);
    
    /**
     * Set the activity id that should be changed to multiple activity ids.
     * This can be used for parallel execution like parallel/inclusive gateways, multiinstance, event sub processes etc.
     */
    ChangeActivityStateBuilder moveSingleActivityIdToActivityIds(String currentActivityId, List<String> newActivityIds);
    
    /**
     * Moves the execution with the current activity id to an activity id in the parent process instance.
     * The sub process instance will be terminated, so all sub process instance executions need to be moved.
     */
    ChangeActivityStateBuilder moveActivityIdToParentActivityId(String currentActivityId, String newActivityId);
    
    /**
     * Moves the execution with the current activity id to an activity id in a new sub process instance for the provided call activity id.
     */
    ChangeActivityStateBuilder moveActivityIdToSubProcessInstanceActivityId(String currentActivityId, String newActivityId, String callActivityId);
    
    /**
     * Sets a process scope variable 
     */
    ChangeActivityStateBuilder processVariable(String processVariableName, Object processVariableValue);
    
    /**
     * Sets multiple process scope variables 
     */
    ChangeActivityStateBuilder processVariables(Map<String, Object> processVariables);
    
    /**
     * Sets a local scope variable for a start activity id
     */
    ChangeActivityStateBuilder localVariable(String startActivityId, String localVariableName, Object localVariableValue);
    
    /**
     * Sets multiple local scope variables for a start activity id
     */
    ChangeActivityStateBuilder localVariables(String startActivityId, Map<String, Object> localVariables);

    /**
     * Start the process instance
     * 
     * @throws FlowableObjectNotFoundException
     *             when no process instance is found
     * @throws FlowableException
     *             activity could not be canceled or started
     **/
    void changeState();

}
