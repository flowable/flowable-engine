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
import java.util.Set;

import org.flowable.engine.common.api.query.Query;

/**
 * @author Joram Barrez
 */
public interface CaseInstanceQuery extends Query<CaseInstanceQuery, CaseInstance> {

    CaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    CaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    CaseInstanceQuery caseDefinitionId(String caseDefinitionId);
    CaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    CaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    CaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);
    CaseInstanceQuery caseInstanceId(String caseInstanceId);
    CaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    CaseInstanceQuery caseInstanceParentId(String parentId);
    CaseInstanceQuery caseInstanceStartedBefore(Date beforeTime);
    CaseInstanceQuery caseInstanceStartedAfter(Date afterTime);
    CaseInstanceQuery caseInstanceStartedBy(String userId);
    CaseInstanceQuery caseInstanceCallbackId(String callbackId);
    CaseInstanceQuery caseInstanceCallbackType(String callbackType);
    CaseInstanceQuery caseInstanceTenantId(String tenantId);
    CaseInstanceQuery caseInstanceWithoutTenantId();
    
    CaseInstanceQuery orderByCaseInstanceId();
    CaseInstanceQuery orderByCaseDefinitionKey();
    CaseInstanceQuery orderByCaseDefinitionId();
    CaseInstanceQuery orderByStartTime();
    CaseInstanceQuery orderByTenantId();
    
}
