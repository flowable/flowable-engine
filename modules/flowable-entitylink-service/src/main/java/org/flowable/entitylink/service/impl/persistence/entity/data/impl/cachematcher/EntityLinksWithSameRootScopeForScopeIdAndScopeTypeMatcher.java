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
package org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.entitylink.api.EntityLinkInfo;

/**
 * @author Filip Hrisafov
 */
public class EntityLinksWithSameRootScopeForScopeIdAndScopeTypeMatcher<EntityImpl extends Entity & EntityLinkInfo> implements CachedEntityMatcher<EntityImpl> {

    @Override
    public boolean isRetained(Collection<EntityImpl> databaseEntities, Collection<CachedEntity> cachedEntities, EntityImpl entity, Object param) {
        Map<String, Object> params = (Map<String, Object>) param;
        String scopeId = (String) params.get("scopeId");
        String scopeType = (String) params.get("scopeType");
        String linkType = (String) params.get("linkType");

        if (entity.getLinkType() != null && entity.getLinkType().equals(linkType) && entity.getRootScopeId() != null) {
            EntityImpl matchingEntityLink = getMatchingEntityLink(databaseEntities, cachedEntities, scopeId, scopeType);
            if (matchingEntityLink != null) {
                return Objects.equals(matchingEntityLink.getRootScopeId(), entity.getRootScopeId())
                        && Objects.equals(matchingEntityLink.getRootScopeType(), entity.getRootScopeType());
            }
        }

        return false;
    }

    public EntityImpl getMatchingEntityLink(Collection<EntityImpl> databaseEntities, Collection<CachedEntity> cachedEntities, String scopeId,
            String scopeType) {

        // Doing some preprocessing here: we need to find the entity link that matches the provided scope id and scope type,
        // as we need to match the root scope id later on.

        if (cachedEntities != null) {
            for (CachedEntity cachedEntity : cachedEntities) {
                EntityImpl entityLink = (EntityImpl) cachedEntity.getEntity();
                if (scopeId.equals(entityLink.getScopeId()) && scopeType.equals(entityLink.getScopeType())) {
                    return entityLink;
                }
            }
        }

        if (databaseEntities != null) {
            for (EntityImpl databaseEntityLink : databaseEntities) {
                if (scopeId.equals(databaseEntityLink.getScopeId()) && scopeType.equals(databaseEntityLink.getScopeType())) {
                    return databaseEntityLink;
                }
            }
        }

        return null;
    }

}