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

import java.util.Collection;
import java.util.List;

/**
 * @author Filip Hrisafov
 */
public interface InternalEntityLinkQuery<E extends EntityLinkInfo> {

    /**
     * Query entity links with the given scope id.
     */
    InternalEntityLinkQuery<E> scopeId(String scopeId);

    /**
     * Query entity links with the given scope ids.
     */
    InternalEntityLinkQuery<E> scopeIds(Collection<String> scopeIds);

    /**
     * Query entity links with the given scope definition id.
     */
    InternalEntityLinkQuery<E> scopeDefinitionId(String scopeDefinitionId);

    /**
     * Query entity links with the given scope type.
     */
    InternalEntityLinkQuery<E> scopeType(String scopeType);

    /**
     * Query entity links with the given reference scope id.
     */
    InternalEntityLinkQuery<E> referenceScopeId(String referenceScopeId);

    /**
     * Query entity links with the given reference scope definition id.
     */
    InternalEntityLinkQuery<E> referenceScopeDefinitionId(String referenceScopeDefinitionId);

    /**
     * Query entity links with the given reference scope type.
     */
    InternalEntityLinkQuery<E> referenceScopeType(String referenceScopeType);

    /**
     * Query entity links with the given root scope id.
     */
    InternalEntityLinkQuery<E> rootScopeId(String rootScopeId);

    /**
     * Query entity links with the given root scope type.
     */
    InternalEntityLinkQuery<E> rootScopeType(String rootScopeType);

    /**
     * Query entity links with the given link type.
     */
    InternalEntityLinkQuery<E> linkType(String linkType);

    /**
     * Query entity links with the given hierarchy type.
     */
    InternalEntityLinkQuery<E> hierarchyType(String hierarchyType);

    List<E> list();

    E singleResult();

}
