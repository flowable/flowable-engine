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
package org.flowable.cmmn.engine.interceptor;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public interface CmmnIdentityLinkInterceptor {

    void handleCompleteTask(TaskEntity task);
    
    void handleAddIdentityLinkToTask(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity);
    
    void handleAddAssigneeIdentityLinkToTask(TaskEntity taskEntity, String assignee);
    
    void handleAddOwnerIdentityLinkToTask(TaskEntity taskEntity, String owner);
    
    void handleCreateCaseInstance(CaseInstanceEntity caseInstance);

    void handleReactivateCaseInstance(CaseInstanceEntity caseInstance);
}
