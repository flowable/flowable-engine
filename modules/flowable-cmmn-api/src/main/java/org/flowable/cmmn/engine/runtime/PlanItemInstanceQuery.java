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
package org.flowable.cmmn.engine.runtime;

import java.util.Date;

import org.flowable.engine.common.api.query.Query;

/**
 * @author Joram Barrez
 */
public interface PlanItemInstanceQuery extends Query<PlanItemInstanceQuery, PlanItemInstance> {

    PlanItemInstanceQuery caseDefinitionId(String caseDefinitionId);
    PlanItemInstanceQuery caseInstanceId(String caseInstanceId);
    PlanItemInstanceQuery stageInstanceId(String stageInstanceId);
    PlanItemInstanceQuery planItemInstanceElementId(String elementId);
    PlanItemInstanceQuery planItemInstanceName(String name);
    PlanItemInstanceQuery planItemInstanceState(String state);
    PlanItemInstanceQuery planItemInstanceStartedBefore(Date startedBefore);
    PlanItemInstanceQuery planItemInstanceStarterAfter(Date startedAfer);
    PlanItemInstanceQuery planItemInstanceStartUserId(String startUserId);
    PlanItemInstanceQuery planItemInstanceReferenceId(String referenceId);
    PlanItemInstanceQuery planItemInstanceReferenceType(String referenceType);
    PlanItemInstanceQuery planItemInstanceTenantId(String tenantId);
    PlanItemInstanceQuery planItemInstanceWithoutTenantId();
    
    PlanItemInstanceQuery orderByStartTime();
    PlanItemInstanceQuery orderByName();
    
}
