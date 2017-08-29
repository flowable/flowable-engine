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
package org.flowable.cmmn.engine.history;

import java.util.Date;
import java.util.Set;

import org.flowable.engine.common.api.query.Query;

/**
 * @author Joram Barrez
 */
public interface HistoricCaseInstanceQuery extends Query<HistoricCaseInstanceQuery, HistoricCaseInstance> {

    HistoricCaseInstanceQuery caseInstanceId(String caseInstanceId);
    HistoricCaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    HistoricCaseInstanceQuery caseInstanceParentId(String parentId);
    HistoricCaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    HistoricCaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    HistoricCaseInstanceQuery caseDefinitionId(String caseDefinitionId);
    HistoricCaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    HistoricCaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    HistoricCaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);
    HistoricCaseInstanceQuery finished();
    HistoricCaseInstanceQuery unfinished();
    HistoricCaseInstanceQuery startedBefore(Date beforeTime);
    HistoricCaseInstanceQuery startedAfter(Date afterTime);
    HistoricCaseInstanceQuery finishedBefore(Date beforeTime);
    HistoricCaseInstanceQuery finishedAfter(Date afterTime);
    HistoricCaseInstanceQuery startedBy(String userId);
    HistoricCaseInstanceQuery caseInstanceCallbackId(String callbackId);
    HistoricCaseInstanceQuery caseInstanceCallbackType(String callbackType);
    HistoricCaseInstanceQuery caseInstanceTenantId(String tenantId);
    HistoricCaseInstanceQuery caseInstanceWithoutTenantId();
    
    HistoricCaseInstanceQuery orderByCaseInstanceId();
    HistoricCaseInstanceQuery orderByCaseDefinitionKey();
    HistoricCaseInstanceQuery orderByCaseDefinitionId();
    HistoricCaseInstanceQuery orderByStartTime();
    HistoricCaseInstanceQuery orderByEndTime();
    HistoricCaseInstanceQuery orderByTenantId();
    
}
