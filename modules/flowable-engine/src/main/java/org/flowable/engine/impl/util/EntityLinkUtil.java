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
package org.flowable.engine.impl.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class EntityLinkUtil {

    public static void createEntityLinks(String scopeId, String subScopeId, String parentElementId,
            String referenceScopeId, String referenceScopeType) {
        
        // scopeId is the process instance in which this is being created
        // referenceScopeId is CallActivity, CaseTask, User

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        EntityLinkService entityLinkService = processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService();
        List<EntityLink> scopeParentEntityLinks = entityLinkService.findEntityLinksByReferenceScopeIdAndType(scopeId, ScopeTypes.BPMN, EntityLinkType.CHILD);

        Set<String> parentIds = new HashSet<>();

        HistoryManager historyManager = CommandContextUtil.getHistoryManager();
        EntityLink scopeRootEntityLink = null;
        // First copy existing links
        for (EntityLink parentEntityLink : scopeParentEntityLinks) {
            if (!parentIds.contains(parentEntityLink.getScopeId())) {
                String newHierarchyType = null;
                if (HierarchyType.ROOT.equals(parentEntityLink.getHierarchyType())) {
                    scopeRootEntityLink = parentEntityLink;
                    newHierarchyType = HierarchyType.ROOT;
                } else if (HierarchyType.PARENT.equals(parentEntityLink.getHierarchyType())) {
                    if (ScopeTypes.TASK.equals(referenceScopeType)) {
                        // For tasks we need to create a Grand Parent link as well
                        // The parent of the scope parent is the Task Grand Parent
                        newHierarchyType = HierarchyType.GRAND_PARENT;
                    }
                }

                copyAndCreateEntityLink(subScopeId, parentElementId, referenceScopeId, referenceScopeType, 
                        newHierarchyType, parentEntityLink, entityLinkService, historyManager);

                parentIds.add(parentEntityLink.getScopeId());
            }
        }

        // Create new entity link
        String hierarchyType;
        String rootScopeId;
        String rootScopeType;

        if (scopeRootEntityLink != null) {
            // If a root entity link exists then the entity link that we would create would be with parent hierarchy type
            hierarchyType = HierarchyType.PARENT;
            rootScopeId = scopeRootEntityLink.getRootScopeId();
            rootScopeType = scopeRootEntityLink.getRootScopeType();
        } else {
            hierarchyType = HierarchyType.ROOT;
            rootScopeId = scopeId;
            rootScopeType = ScopeTypes.BPMN;
        }

        createEntityLink(scopeId, subScopeId, parentElementId, referenceScopeId, referenceScopeType, hierarchyType, 
                rootScopeId, rootScopeType, entityLinkService, historyManager);
    }
    protected static EntityLinkEntity copyAndCreateEntityLink(String subScopeId, String parentElementId,
            String referenceScopeId, String referenceScopeType, String hierarchyType,
            EntityLink parentEntityLink, EntityLinkService entityLinkService, HistoryManager historyManager) {

        EntityLinkEntity newEntityLink = (EntityLinkEntity) entityLinkService.createEntityLink();
        newEntityLink.setLinkType(EntityLinkType.CHILD);
        newEntityLink.setScopeId(parentEntityLink.getScopeId());
        newEntityLink.setSubScopeId(subScopeId);
        newEntityLink.setScopeType(parentEntityLink.getScopeType());
        newEntityLink.setScopeDefinitionId(parentEntityLink.getScopeDefinitionId());
        newEntityLink.setParentElementId(parentElementId);
        newEntityLink.setReferenceScopeId(referenceScopeId);
        newEntityLink.setReferenceScopeType(referenceScopeType);
        newEntityLink.setHierarchyType(hierarchyType);
        newEntityLink.setRootScopeId(parentEntityLink.getRootScopeId());
        newEntityLink.setRootScopeType(parentEntityLink.getRootScopeType());
        entityLinkService.insertEntityLink(newEntityLink);

        historyManager.recordEntityLinkCreated(newEntityLink);

        return newEntityLink;
    }

    protected static EntityLinkEntity createEntityLink(String scopeId, String subScopeId, String parentElementId,
            String referenceScopeId, String referenceScopeType, String hierarchyType, String rootScopeId, String rootScopeType,
            EntityLinkService entityLinkService, HistoryManager historyManager) {

        EntityLinkEntity newEntityLink = (EntityLinkEntity) entityLinkService.createEntityLink();
        newEntityLink.setLinkType(EntityLinkType.CHILD);
        newEntityLink.setScopeId(scopeId);
        newEntityLink.setSubScopeId(subScopeId);
        newEntityLink.setScopeType(ScopeTypes.BPMN);
        newEntityLink.setParentElementId(parentElementId);
        newEntityLink.setReferenceScopeId(referenceScopeId);
        newEntityLink.setReferenceScopeType(referenceScopeType);
        newEntityLink.setHierarchyType(hierarchyType);
        newEntityLink.setRootScopeId(rootScopeId);
        newEntityLink.setRootScopeType(rootScopeType);
        entityLinkService.insertEntityLink(newEntityLink);

        historyManager.recordEntityLinkCreated(newEntityLink);

        return newEntityLink;
    }
    
}