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
package org.flowable.engine.impl.cmmn;

import java.util.Map;

import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 */
public interface CaseInstanceService {
    
    /**
     * @return A new id that will be used when starting a case instance.
     *         This is for example needed to set the bidrectional relation
     *         when a process instance starts a case instance through a case task.
     */
    String generateNewCaseInstanceId();

    String startCaseInstanceByKey(String caseDefinitionKey, String predefinedCaseInstanceId, String caseInstanceName, String businessKey,
            String executionId, String tenantId, boolean fallbackToDefaultTenant, String parentDeploymentId, Map<String, Object> inParametersMap);
    
    void handleSignalEvent(EventSubscriptionEntity eventSubscription, Map<String, Object> variables);

    void deleteCaseInstance(String caseInstanceId);

    void deleteCaseInstancesForExecutionId(String executionId);
    
    void deleteCaseInstanceWithoutAgenda(String caseInstanceId);

}
