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
package org.flowable.cmmn.api.history;

import java.util.Date;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 */
public interface HistoricMilestoneInstanceQuery extends Query<HistoricMilestoneInstanceQuery, HistoricMilestoneInstance> {
    
    HistoricMilestoneInstanceQuery milestoneInstanceName(String name);
    HistoricMilestoneInstanceQuery milestoneInstanceCaseInstanceId(String caseInstanceId);
    HistoricMilestoneInstanceQuery milestoneInstanceCaseDefinitionId(String caseDefinitionId);
    HistoricMilestoneInstanceQuery milestoneInstanceReachedBefore(Date reachedBefore);
    HistoricMilestoneInstanceQuery milestoneInstanceReachedAfter(Date reachedAfter);
    
    HistoricMilestoneInstanceQuery orderByMilestoneName();
    HistoricMilestoneInstanceQuery orderByTimeStamp();
    
}