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

package org.flowable.identitylink.service.impl.persistence.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.IdentityLinkEventHandler;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;

/**
 * @author Tom Baeyens
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */
public class IdentityLinkEntityManagerImpl
        extends AbstractServiceEngineEntityManager<IdentityLinkServiceConfiguration, IdentityLinkEntity, IdentityLinkDataManager>
        implements IdentityLinkEntityManager {

    public IdentityLinkEntityManagerImpl(IdentityLinkServiceConfiguration identityLinkServiceConfiguration, IdentityLinkDataManager identityLinkDataManager) {
        super(identityLinkServiceConfiguration, identityLinkServiceConfiguration.getEngineName(), identityLinkDataManager);
    }

    @Override
    public IdentityLinkEntity createIdentityLinkFromHistoricIdentityLink(HistoricIdentityLink historicIdentityLink) {
        return dataManager.createIdentityLinkFromHistoricIdentityLink(historicIdentityLink);
    }

    @Override
    public void insert(IdentityLinkEntity entity, boolean fireCreateEvent) {
        super.insert(entity, fireCreateEvent);

        IdentityLinkEventHandler identityLinkEventHandler = getIdentityLinkEventHandler();
        if (identityLinkEventHandler != null) {
            identityLinkEventHandler.handleIdentityLinkAddition(entity);
        }
    }

    @Override
    public void delete(IdentityLinkEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, fireDeleteEvent);

        IdentityLinkEventHandler identityLinkEventHandler = getIdentityLinkEventHandler();
        if (identityLinkEventHandler != null) {
            getIdentityLinkEventHandler().handleIdentityLinkDeletion(entity);
        }
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId) {
        return dataManager.findIdentityLinksByTaskId(taskId);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId) {
        return dataManager.findIdentityLinksByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeIdAndType(String scopeId, String scopeType) {
        return dataManager.findIdentityLinksByScopeIdAndType(scopeId, scopeType);
    }
    
    @Override
    public List<IdentityLinkEntity> findIdentityLinksBySubScopeIdAndType(String subScopeId, String scopeType) {
        return dataManager.findIdentityLinksBySubScopeIdAndType(subScopeId, scopeType);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId) {
        return dataManager.findIdentityLinksByProcessDefinitionId(processDefinitionId);
    }
    
    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        return dataManager.findIdentityLinksByScopeDefinitionIdAndType(scopeDefinitionId, scopeType);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type) {
        return dataManager.findIdentityLinkByTaskUserGroupAndType(taskId, userId, groupId, type);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByProcessInstanceUserGroupAndType(String processInstanceId, String userId, String groupId, String type) {
        return dataManager.findIdentityLinkByProcessInstanceUserGroupAndType(processInstanceId, userId, groupId, type);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId, String userId, String groupId) {
        return dataManager.findIdentityLinkByProcessDefinitionUserAndGroup(processDefinitionId, userId, groupId);
    }
    
    @Override
    public List<IdentityLinkEntity> findIdentityLinkByScopeIdScopeTypeUserGroupAndType(String scopeId, String scopeType, String userId, String groupId, String type) {
        return dataManager.findIdentityLinkByScopeIdScopeTypeUserGroupAndType(scopeId, scopeType, userId, groupId, type);
    }
    
    @Override
    public List<IdentityLinkEntity> findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(String scopeDefinitionId, String scopeType, String userId, String groupId) {
        return dataManager.findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(scopeDefinitionId, scopeType, userId, groupId);
    }
    
    @Override
    public IdentityLinkEntity addProcessInstanceIdentityLink(String processInstanceId, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = dataManager.create();
        identityLinkEntity.setProcessInstanceId(processInstanceId);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(type);
        insert(identityLinkEntity);
        return identityLinkEntity;
    }
    
    @Override
    public IdentityLinkEntity addScopeIdentityLink(String scopeDefinitionId, String scopeId, String scopeType, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = dataManager.create();
        identityLinkEntity.setScopeDefinitionId(scopeDefinitionId);
        identityLinkEntity.setScopeId(scopeId);
        identityLinkEntity.setScopeType(scopeType);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(type);
        insert(identityLinkEntity);
        return identityLinkEntity;
    }
    
    @Override
    public IdentityLinkEntity addSubScopeIdentityLink(String scopeDefinitionId, String scopeId, String subScopeId, String scopeType, 
                    String userId, String groupId, String type) {
        
        IdentityLinkEntity identityLinkEntity = dataManager.create();
        identityLinkEntity.setScopeDefinitionId(scopeDefinitionId);
        identityLinkEntity.setScopeId(scopeId);
        identityLinkEntity.setSubScopeId(subScopeId);
        identityLinkEntity.setScopeType(scopeType);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(type);
        insert(identityLinkEntity);
        return identityLinkEntity;
    }

    @Override
    public IdentityLinkEntity addTaskIdentityLink(String taskId, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = dataManager.create();
        identityLinkEntity.setTaskId(taskId);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(type);
        insert(identityLinkEntity);
        
        return identityLinkEntity;
    }

    @Override
    public IdentityLinkEntity addProcessDefinitionIdentityLink(String processDefinitionId, String userId, String groupId) {
        IdentityLinkEntity identityLinkEntity = dataManager.create();
        identityLinkEntity.setProcessDefId(processDefinitionId);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(IdentityLinkType.CANDIDATE);
        insert(identityLinkEntity);
        return identityLinkEntity;
    }
    
    @Override
    public IdentityLinkEntity addScopeDefinitionIdentityLink(String scopeDefinitionId, String scopeType, String userId, String groupId) {
        IdentityLinkEntity identityLinkEntity = dataManager.create();
        identityLinkEntity.setScopeDefinitionId(scopeDefinitionId);
        identityLinkEntity.setScopeType(scopeType);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(IdentityLinkType.CANDIDATE);
        insert(identityLinkEntity);
        return identityLinkEntity;
    }

    @Override
    public IdentityLinkEntity addCandidateUser(String taskId, String userId) {
        return addTaskIdentityLink(taskId, userId, null, IdentityLinkType.CANDIDATE);
    }

    @Override
    public List<IdentityLinkEntity> addCandidateUsers(String taskId, Collection<String> candidateUsers) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (String candidateUser : candidateUsers) {
            identityLinks.add(addCandidateUser(taskId, candidateUser));
        }
        
        return identityLinks;
    }

    @Override
    public IdentityLinkEntity addCandidateGroup(String taskId, String groupId) {
        return addTaskIdentityLink(taskId, null, groupId, IdentityLinkType.CANDIDATE);
    }

    @Override
    public List<IdentityLinkEntity> addCandidateGroups(String taskId, Collection<String> candidateGroups) {
        List<IdentityLinkEntity> identityLinks = new ArrayList<>();
        for (String candidateGroup : candidateGroups) {
            identityLinks.add(addCandidateGroup(taskId, candidateGroup));
        }
        return identityLinks;
    }
    
    @Override
    public List<IdentityLinkEntity> deleteProcessInstanceIdentityLink(String processInstanceId, String userId, String groupId, String type) {
        List<IdentityLinkEntity> identityLinks = findIdentityLinkByProcessInstanceUserGroupAndType(processInstanceId, userId, groupId, type);

        for (IdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
        }

        return identityLinks;
    }
    
    @Override
    public List<IdentityLinkEntity> deleteScopeIdentityLink(String scopeId, String scopeType, String userId, String groupId, String type) {
        List<IdentityLinkEntity> identityLinks = findIdentityLinkByScopeIdScopeTypeUserGroupAndType(scopeId, scopeType, userId, groupId, type);

        for (IdentityLinkEntity identityLink : identityLinks) {
            deleteIdentityLink(identityLink);
        }

        return identityLinks;
    }

    @Override
    public List<IdentityLinkEntity> deleteTaskIdentityLink(String taskId, List<IdentityLinkEntity> currentIdentityLinks, String userId, String groupId, String type) {
        List<IdentityLinkEntity> identityLinks = findIdentityLinkByTaskUserGroupAndType(taskId, userId, groupId, type);

        List<IdentityLinkEntity> removedIdentityLinkEntities = new ArrayList<>();
        for (IdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
            removedIdentityLinkEntities.add(identityLink);
        }

        if (currentIdentityLinks != null) { // The currentIdentityLinks might contain identity links that are in the cache, but not yet in the db
            for (IdentityLinkEntity identityLinkEntity : currentIdentityLinks) {
                if (type.equals(identityLinkEntity.getType()) && !contains(removedIdentityLinkEntities, identityLinkEntity.getId())) {

                    if ((userId != null && userId.equals(identityLinkEntity.getUserId()))
                            || (groupId != null && groupId.equals(identityLinkEntity.getGroupId()))) {

                        delete(identityLinkEntity);
                        removedIdentityLinkEntities.add(identityLinkEntity);

                    }
                }
            }
        }
        
        return removedIdentityLinkEntities;
    }

    protected boolean contains(List<IdentityLinkEntity> identityLinkEntities, String identityLinkId) {
        return identityLinkEntities.stream().anyMatch(identityLinkEntity -> Objects.equals(identityLinkId, identityLinkEntity.getId()));
    }

    @Override
    public List<IdentityLinkEntity> deleteProcessDefinitionIdentityLink(String processDefinitionId, String userId, String groupId) {
        List<IdentityLinkEntity> identityLinks = findIdentityLinkByProcessDefinitionUserAndGroup(processDefinitionId, userId, groupId);
        for (IdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
        }
        
        return identityLinks;
    }
    
    @Override
    public List<IdentityLinkEntity> deleteScopeDefinitionIdentityLink(String scopeDefinitionId, String scopeType, String userId, String groupId) {
        List<IdentityLinkEntity> identityLinks = findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(scopeDefinitionId, scopeType, userId, groupId);
        for (IdentityLinkEntity identityLink : identityLinks) {
            deleteIdentityLink(identityLink);
        }
        
        return identityLinks;
    }
    
    public void deleteIdentityLink(IdentityLinkEntity identityLink) {
        delete(identityLink);
    }

    @Override
    public void deleteIdentityLinksByTaskId(String taskId) {
        dataManager.deleteIdentityLinksByTaskId(taskId);
    }

    @Override
    public void deleteIdentityLinksByProcDef(String processDefId) {
        dataManager.deleteIdentityLinksByProcDef(processDefId);
    }
    
    @Override
    public void deleteIdentityLinksByProcessInstanceId(String processInstanceId) {
        dataManager.deleteIdentityLinksByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        dataManager.deleteIdentityLinksByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public void deleteIdentityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType) {
        dataManager.deleteIdentityLinksByScopeDefinitionIdAndScopeType(scopeDefinitionId, scopeType);
    }

    protected IdentityLinkEventHandler getIdentityLinkEventHandler() {
        return serviceConfiguration.getIdentityLinkEventHandler();
    }

}
