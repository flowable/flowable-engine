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
import java.util.List;
import java.util.Set;

import org.flowable.engine.common.api.query.Query;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface HistoricCaseInstanceQuery extends Query<HistoricCaseInstanceQuery, HistoricCaseInstance> {

    /**
     * Only select historic case instances with the given identifier.
     */
    HistoricCaseInstanceQuery caseInstanceId(String caseInstanceId);
    
    /**
     * Only select historic case instances with one the given identifiers.
     */
    HistoricCaseInstanceQuery caseInstanceIds(Set<String> caseInstanceIds);
    
    /**
     * Only select historic case instances with the given business key.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    
    /**
     * Only select historic case instances with the parent identifier.
     */
    HistoricCaseInstanceQuery caseInstanceParentId(String parentId);
    
    /**
     * Only select historic case instances with the given key.
     */
    HistoricCaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    
    /**
     * Only select historic case instances with the given keys.
     */
    HistoricCaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    
    /**
     * Only select historic case instances with the given case definition identifier.
     */
    HistoricCaseInstanceQuery caseDefinitionId(String caseDefinitionId);
    
    /**
     * Only select historic case instances with the given case definition category.
     */
    HistoricCaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    
    /**
     * Only select historic case instances with the given case definition name.
     */
    HistoricCaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    
    /**
     * Only select historic case instances with the given case definition version.
     */
    HistoricCaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);
    
    /**
     * Only select historic case instances that are defined by a case definition with the given deployment identifier.
     */
    HistoricCaseInstanceQuery deploymentId(String deploymentId);

    /**
     * Only select historic case instances that are defined by a case definition with one of the given deployment identifiers.
     */
    HistoricCaseInstanceQuery deploymentIds(List<String> deploymentIds);
    
    /**
     * Only select historic case instances that are finished.
     */
    HistoricCaseInstanceQuery finished();
    
    /**
     * Only select historic case instances that are not finished.
     */
    HistoricCaseInstanceQuery unfinished();
    
    /**
     * Only select historic case instances that are started before the provided date time.
     */
    HistoricCaseInstanceQuery startedBefore(Date beforeTime);
    
    /**
     * Only select historic case instances that are started after the provided date time.
     */
    HistoricCaseInstanceQuery startedAfter(Date afterTime);
    
    /**
     * Only select historic case instances that are finished before the provided date time.
     */
    HistoricCaseInstanceQuery finishedBefore(Date beforeTime);
    
    /**
     * Only select historic case instances that are finished after the provided date time.
     */
    HistoricCaseInstanceQuery finishedAfter(Date afterTime);
    
    /**
     * Only select historic case instances that are started by the provided user identifier.
     */
    HistoricCaseInstanceQuery startedBy(String userId);
    
    /**
     * Only select historic case instances that have the provided callback identifier.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackId(String callbackId);
    
    /**
     * Only select historic case instances that have the provided callback type.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackType(String callbackType);
    
    /**
     * Only select historic case instances that have the tenant identifier.
     */
    HistoricCaseInstanceQuery caseInstanceTenantId(String tenantId);
    
    /**
     * Only select historic case instances that have no tenant identifier.
     */
    HistoricCaseInstanceQuery caseInstanceWithoutTenantId();
    
    HistoricCaseInstanceQuery orderByCaseInstanceId();
    HistoricCaseInstanceQuery orderByCaseDefinitionKey();
    HistoricCaseInstanceQuery orderByCaseDefinitionId();
    HistoricCaseInstanceQuery orderByStartTime();
    HistoricCaseInstanceQuery orderByEndTime();
    HistoricCaseInstanceQuery orderByTenantId();
    
}
