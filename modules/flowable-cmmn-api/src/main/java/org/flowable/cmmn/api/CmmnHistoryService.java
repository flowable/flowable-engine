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
package org.flowable.cmmn.api;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;

/**
 * @author Joram Barrez
 */
public interface CmmnHistoryService {
    
    HistoricCaseInstanceQuery createHistoricCaseInstanceQuery();

    HistoricMilestoneInstanceQuery createHistoricMilestoneInstanceQuery();
    
    HistoricVariableInstanceQuery createHistoricVariableInstanceQuery();
    
    HistoricTaskInstanceQuery createHistoricTaskInstanceQuery();

    HistoricPlanItemInstanceQuery createHistoricPlanItemInstanceQuery();

    void deleteHistoricCaseInstance(String caseInstanceId);
    
    /**
     * Deletes historic task instance. This might be useful for tasks that are {@link CmmnTaskService#newTask() dynamically created} and then {@link CmmnTaskService#complete(String) completed}. If the
     * historic task instance doesn't exist, no exception is thrown and the method returns normal.
     */
    void deleteHistoricTaskInstance(String taskId);
    
    /**
     * Retrieves the {@link HistoricIdentityLink}s associated with the given task. Such an {@link IdentityLink} informs how a certain identity (eg. group or user) is associated with a certain task
     * (eg. as candidate, assignee, etc.), even if the task is completed as opposed to {@link IdentityLink}s which only exist for active tasks.
     */
    List<HistoricIdentityLink> getHistoricIdentityLinksForTask(String taskId);

    /**
     * Retrieves the {@link HistoricIdentityLink}s associated with the given case instance. Such an {@link IdentityLink} informs how a certain identity (eg. group or user) is associated with a
     * certain case instance, even if the instance is completed as opposed to {@link IdentityLink}s which only exist for active instances.
     */
    List<HistoricIdentityLink> getHistoricIdentityLinksForCaseInstance(String caseInstanceId);
}
