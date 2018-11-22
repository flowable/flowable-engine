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
package org.flowable.cmmn.engine.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;

/**
 * @author Tijs Rademakers
 */
public class EntityLinkUtil {

    public static void copyExistingEntityLinks(String scopeId, String referenceScopeId, String referenceScopeType) {
        EntityLinkService entityLinkService = CommandContextUtil.getEntityLinkService();
        List<EntityLink> entityLinks = entityLinkService.findEntityLinksByReferenceScopeIdAndType(scopeId, ScopeTypes.CMMN, EntityLinkType.CHILD);
        List<String> parentIds = new ArrayList<>();
        for (EntityLink entityLink : entityLinks) {
            if (!parentIds.contains(entityLink.getScopeId())) {
                EntityLinkEntity newEntityLink = (EntityLinkEntity) entityLinkService.createEntityLink();
                newEntityLink.setLinkType(EntityLinkType.CHILD);
                newEntityLink.setScopeId(entityLink.getScopeId());
                newEntityLink.setScopeType(entityLink.getScopeType());
                newEntityLink.setScopeDefinitionId(entityLink.getScopeDefinitionId());
                newEntityLink.setReferenceScopeId(referenceScopeId);
                newEntityLink.setReferenceScopeType(referenceScopeType);
                newEntityLink.setRootScopeId(entityLink.getRootScopeId());
                newEntityLink.setRootScopeType(entityLink.getRootScopeType());
                entityLinkService.insertEntityLink(newEntityLink);

                CommandContextUtil.getCmmnHistoryManager().recordEntityLinkCreated(newEntityLink);

                parentIds.add(entityLink.getScopeId());
            }
        }
    }

    public static void createNewEntityLink(String scopeId, String referenceScopeId, String referenceScopeType) {
        EntityLinkService entityLinkService = CommandContextUtil.getEntityLinkService();

        String rootScopeId = scopeId;
        String rootScopeType = ScopeTypes.CMMN;

        // Check if existing links already have root, if not, current is root
        Optional<EntityLink> entityLinkWithRoot = entityLinkService
            .findEntityLinksByReferenceScopeIdAndType(scopeId, ScopeTypes.CMMN, EntityLinkType.CHILD)
            .stream()
            .filter(e -> e.getRootScopeId() != null)
            .findFirst();

        if (entityLinkWithRoot.isPresent()) {
            rootScopeId = entityLinkWithRoot.get().getRootScopeId();
            rootScopeType = entityLinkWithRoot.get().getRootScopeType();
        }

        EntityLinkEntity newEntityLink = (EntityLinkEntity) entityLinkService.createEntityLink();
        newEntityLink.setLinkType(EntityLinkType.CHILD);
        newEntityLink.setScopeId(scopeId);
        newEntityLink.setScopeType(ScopeTypes.CMMN);
        newEntityLink.setReferenceScopeId(referenceScopeId);
        newEntityLink.setReferenceScopeType(referenceScopeType);
        newEntityLink.setRootScopeId(rootScopeId);
        newEntityLink.setRootScopeType(rootScopeType);
        entityLinkService.insertEntityLink(newEntityLink);

        CommandContextUtil.getCmmnHistoryManager().recordEntityLinkCreated(newEntityLink);
    }

}