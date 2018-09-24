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
package org.flowable.cmmn.api.runtime;

import java.util.Date;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 */
public interface MilestoneInstanceQuery extends Query<MilestoneInstanceQuery, MilestoneInstance> {
    
    MilestoneInstanceQuery milestoneInstanceName(String name);
    MilestoneInstanceQuery milestoneInstanceCaseInstanceId(String caseInstanceId);
    MilestoneInstanceQuery milestoneInstanceCaseDefinitionId(String caseDefinitionId);
    MilestoneInstanceQuery milestoneInstanceReachedBefore(Date reachedBefore);
    MilestoneInstanceQuery milestoneInstanceReachedAfter(Date reachedAfter);
    MilestoneInstanceQuery milestoneInstanceTenantId(String tenantId);
    MilestoneInstanceQuery milestoneInstanceTenantIdLike(String tenantIdLike);
    MilestoneInstanceQuery milestoneInstanceWithoutTenantId();
    
    MilestoneInstanceQuery orderByMilestoneName();
    MilestoneInstanceQuery orderByTimeStamp();
    
}