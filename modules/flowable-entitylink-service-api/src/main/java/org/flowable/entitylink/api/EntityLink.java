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

import java.util.Date;

/**
 * An entity link is used to associate an entity with another entity.
 * 
 * @author Tijs Rademakers
 */
public interface EntityLink {

    /**
     * Returns the type of link. See for the native supported types.
     */
    String getLinkType();

    /**
     * Returns the scope id of the originating entity
     */
    String getScopeId();
    
    /**
     * The scope type associated with the originating entity
     */
    String getScopeType();
    
    /**
     * A scope definition id for the originating entity
     */
    String getScopeDefinitionId();
    
    /**
     * Returns the scope id of the referenced entity
     */
    String getReferenceScopeId();
    
    /**
     * The scope type associated with the referenced entity
     */
    String getReferenceScopeType();
    
    /**
     * A scope definition id for the referenced entity
     */
    String getReferenceScopeDefinitionId();

    /**
     * Returns the scope id of the root entity
     */
    String getRootScopeId();

    /**
     * The scope type associated with the root entity
     */
    String getRootScopeType();

    /**
     * A scope definition id for the root entity
     */
    String getRootScopeDefinitionId();
    
    /**
     * The create time for the entity link
     */
    Date getCreateTime();

}
