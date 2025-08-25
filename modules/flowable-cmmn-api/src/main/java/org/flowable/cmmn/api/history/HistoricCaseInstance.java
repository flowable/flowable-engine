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
import java.util.Map;

/**
 * @author Joram Barrez
 */
public interface HistoricCaseInstance {
    
    String getId();
    String getName();
    String getParentId();
    String getBusinessKey();
    String getBusinessStatus();
    String getCaseDefinitionId();
    String getCaseDefinitionKey();
    String getCaseDefinitionName();
    Integer getCaseDefinitionVersion();
    String getCaseDefinitionDeploymentId();
    String getState();
    Date getStartTime();
    Date getEndTime();
    String getStartUserId();
    Date getLastReactivationTime();
    String getLastReactivationUserId();
    String getCallbackId();
    String getCallbackType();
    String getReferenceId();
    String getReferenceType();
    String getTenantId();
    String getEndUserId();

    Map<String, Object> getCaseVariables();

    /** Sets an optional localized name for the case. */
    void setLocalizedName(String localizedName);
}
