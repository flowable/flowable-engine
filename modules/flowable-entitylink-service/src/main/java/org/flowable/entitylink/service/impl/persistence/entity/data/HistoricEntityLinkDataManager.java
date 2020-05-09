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
package org.flowable.entitylink.service.impl.persistence.entity.data;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntity;

/**
 * @author Tijs Rademakers
 */
public interface HistoricEntityLinkDataManager extends DataManager<HistoricEntityLinkEntity> {

    List<HistoricEntityLink> findHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType, String linkType);

    List<HistoricEntityLink> findHistoricEntityLinksWithSameRootScopeForScopeIdAndScopeType(String scopeId, String scopeType, String linkType);

    List<HistoricEntityLink> findHistoricEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String scopeType, String linkType);

    List<HistoricEntityLink> findHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType, String linkType);
    
    void deleteHistoricEntityLinksByScopeIdAndType(String scopeId, String scopeType);
    
    void deleteHistoricEntityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType);
    
    void deleteHistoricEntityLinksForNonExistingProcessInstances();
    
    void deleteHistoricEntityLinksForNonExistingCaseInstances();
}
