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
package org.flowable.entitylink.service.impl.persistence.entity;

import java.util.Date;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.entitylink.api.EntityLink;

/**
 * @author Tijs Rademakers
 */
public interface EntityLinkEntity extends EntityLink, Entity {

    void setLinkType(String linkType);

    void setScopeId(String scopeId);
    
    void setSubScopeId(String subScopeId);
    
    void setScopeType(String scopeType);
    
    void setScopeDefinitionId(String scopeDefinitionId);
    
    void setParentElementId(String parentElementId);

    void setReferenceScopeId(String referenceScopeId);
    
    void setReferenceScopeType(String referenceScopeType);
    
    void setReferenceScopeDefinitionId(String referenceScopeDefinitionId);

    void setRootScopeId(String rootScopeId);

    void setRootScopeType(String rootScopeType);

    void setHierarchyType(String hierarchyType);

    void setCreateTime(Date createTime);
}
