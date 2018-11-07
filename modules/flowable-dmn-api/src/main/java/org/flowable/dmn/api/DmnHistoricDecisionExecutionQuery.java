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

import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link DmnHistoricDecisionExecution}s.
 * 
 * @author Tijs Rademakers
 */
public interface DmnHistoricDecisionExecutionQuery extends Query<DmnHistoricDecisionExecutionQuery, DmnHistoricDecisionExecution> {

    /** Only select decision execution with the given id. */
    DmnHistoricDecisionExecutionQuery id(String id);

    /** Only select decision executions with the given ids. */
    DmnHistoricDecisionExecutionQuery ids(Set<String> decisionExecutionIds);

    /** Only select decision executions with the given definition id. */
    DmnHistoricDecisionExecutionQuery decisionDefinitionId(String decisionDefinitionId);
    
    /** Only select decision executions with the given deployment id. */
    DmnHistoricDecisionExecutionQuery deploymentId(String deploymentId);
    
    /** Only select decision executions with the given definition key. */
    DmnHistoricDecisionExecutionQuery decisionKey(String decisionKey);

    /** Only select decision executions with the given instance id. */
    DmnHistoricDecisionExecutionQuery instanceId(String instanceId);

    /** Only select decision executions with the given execution id. */
    DmnHistoricDecisionExecutionQuery executionId(String executionId);
    
    /** Only select decision executions with the given activity id. */
    DmnHistoricDecisionExecutionQuery activityId(String activityId);
    
    /** Only select decision executions with the given scope type. */
    DmnHistoricDecisionExecutionQuery scopeType(String scopeType);
    
    /** Only select decision executions with the given failed state. */
    DmnHistoricDecisionExecutionQuery failed(Boolean failed);

    /**
     * Only select decision executions that have the given tenant id.
     */
    DmnHistoricDecisionExecutionQuery tenantId(String tenantId);

    /**
     * Only select decision executions with a tenant id like the given one.
     */
    DmnHistoricDecisionExecutionQuery tenantIdLike(String tenantIdLike);

    /**
     * Only select decision executions that do not have a tenant id.
     */
    DmnHistoricDecisionExecutionQuery withoutTenantId();

    // ordering ////////////////////////////////////////////////////////////

    /**
     * Order by the start time of the decision executions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnHistoricDecisionExecutionQuery orderByStartTime();
    
    /**
     * Order by the end time of the decision executions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnHistoricDecisionExecutionQuery orderByEndTime();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnHistoricDecisionExecutionQuery orderByTenantId();

}
