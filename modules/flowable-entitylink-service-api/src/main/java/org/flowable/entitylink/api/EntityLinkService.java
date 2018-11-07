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
package org.flowable.entitylink.api;

import java.util.List;

/**
 * Service which provides access to entity links.
 * 
 * @author Tijs Rademakers
 */
public interface EntityLinkService {
    
    EntityLink getEntityLink(String id);
    
    List<EntityLink> findEntityLinksByScopeIdAndType(String scopeId, String scopeType, String linkType);
    
    List<EntityLink> findEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String scopeType, String linkType);
    
    List<EntityLink> findEntityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType, String linkType);
    
    EntityLink createEntityLink();
    
    void insertEntityLink(EntityLink entityLink);
    
    void deleteEntityLink(EntityLink entityLink);
    
    List<EntityLink> deleteScopeEntityLink(String scopeId, String scopeType, String linkType);
    
    List<EntityLink> deleteScopeDefinitionEntityLink(String scopeDefinitionId, String scopeType, String linkType);
    
    void deleteEntityLinksByScopeIdAndType(String scopeId, String scopeType);
    
    void deleteEntityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType);
    
}
