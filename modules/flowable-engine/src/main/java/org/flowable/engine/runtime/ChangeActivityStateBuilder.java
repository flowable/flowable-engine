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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;

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
    ChangeActivityStateBuilder executionId(String executionId);

    /**
     * Set the activity that should be cancelled.
     */
    ChangeActivityStateBuilder cancelActivityId(String cancelActivityId);

    /**
     * Set the activity that should be started
     **/
    ChangeActivityStateBuilder startActivityId(String startActivityId);

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
