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

import java.util.Date;

/**
 * An object structure representing the execution of a decision
 * 
 * @author Tijs Rademakers
 */
public interface DmnHistoricDecisionExecution {

    /** unique identifier */
    String getId();

    /** reference to the decision definition that was executed */
    String getDecisionDefinitionId();
    
    /** reference to the deployment of the decision definition that was executed */
    String getDeploymentId();

    /** start time of the decision execution */
    Date getStartTime();
    
    /** end time of the decision execution */
    Date getEndTime();

    /** reference to the (process) instance for which the decision was executed */
    String getInstanceId();
    
    /** reference to the execution for which the decision was executed */
    String getExecutionId();
    
    /** reference to the activity for which the decision was executed */
    String getActivityId();
    
    /** reference to the scope type for which the decision was executed */
    String getScopeType();
    
    /** identifier if the decision execution failed */
    boolean isFailed();

    /** tenant identifier of this decision execution */
    String getTenantId();

    /** detailed information of the decision execution */
    String getExecutionJson();

    /** reference to decision key */
    String getDecisionKey();

    /** reference to decision name */
    String getDecisionName();

    /** reference to decision version */
    String getDecisionVersion();
}
