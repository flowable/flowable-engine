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
package org.flowable.engine.data.inmemory.impl.identitylink;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory {@link IdentityLinkDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryIdentityLinkDataManager extends AbstractMemoryDataManager<IdentityLinkEntity> implements IdentityLinkDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryIdentityLinkDataManager.class);

    public MemoryIdentityLinkDataManager(MapProvider mapProvider, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdGenerator());
    }

    @Override
    public IdentityLinkEntity create() {
        return new IdentityLinkEntityImpl();
    }

    @Override
    public IdentityLinkEntity createIdentityLinkFromHistoricIdentityLink(HistoricIdentityLink historicIdentityLink) {
        return new IdentityLinkEntityImpl(historicIdentityLink);
    }

    @Override
    public void insert(IdentityLinkEntity entity) {
        doInsert(entity);
    }

    @Override
    public IdentityLinkEntity update(IdentityLinkEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public IdentityLinkEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinksByTaskId {}", taskId);
        }
        return getData().values().stream().filter(item -> item.getTaskId() != null && item.getTaskId().equals(taskId)).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinksByProcessInstanceId {}", processInstanceId);
        }
        return getData().values().stream().filter(item -> item.getProcessInstanceId() != null && item.getProcessInstanceId().equals(processInstanceId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinksByProcessDefinitionId {}", processDefinitionId);
        }
        return getData().values().stream().filter(item -> item.getProcessDefinitionId() != null && item.getProcessDefinitionId().equals(processDefinitionId))
                        .collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeIdAndType(String scopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinksByScopeIdAndType {} {}", scopeId, scopeType);
        }
        return getData().values().stream().filter(item -> item.getScopeId() != null && item.getScopeId().equals(scopeId) && item.getScopeType() != null
                        && item.getScopeType().equals(scopeType)).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksBySubScopeIdAndType(String subScopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinksBySubScopeIdAndType {} {}", subScopeId, scopeType);
        }

        return getData().values().stream().filter(item -> item.getSubScopeId() != null && item.getSubScopeId().equals(subScopeId) && item.getScopeType() != null
                        && item.getScopeType().equals(scopeType)).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinksByScopeDefinitionIdAndType {} {}", scopeDefinitionId, scopeType);
        }
        return getData().values().stream().filter(item -> item.getScopeDefinitionId() != null && item.getScopeDefinitionId().equals(scopeDefinitionId)
                        && item.getScopeType() != null && item.getScopeType().equals(scopeType)).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinkByTaskUserGroupAndType {} {} {} {}", taskId, userId, groupId, type);
        }
        return getData().values().stream().filter(item -> {
            if (item.getTaskId() == null) {
                return false;
            }
            if (!item.getTaskId().equals(taskId)) {
                return false;
            }
            if (userId != null && !userId.equals(item.getUserId())) {
                return false;
            }
            if (groupId != null && !groupId.equals(item.getGroupId())) {
                return false;
            }
            if (type != null && !type.equals(item.getType())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByProcessInstanceUserGroupAndType(String processInstanceId, String userId, String groupId, String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinkByProcessInstanceUserGroupAndType {} {} {} {}", processInstanceId, userId, groupId, type);
        }
        return getData().values().stream().filter(item -> {
            if (item.getProcessInstanceId() == null) {
                return false;
            }
            if (!item.getProcessInstanceId().equals(processInstanceId)) {
                return false;
            }
            if (userId != null && !userId.equals(item.getUserId())) {
                return false;
            }
            if (groupId != null && !groupId.equals(item.getGroupId())) {
                return false;
            }
            if (type != null && !type.equals(item.getType())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId, String userId, String groupId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinkByProcessDefinitionUserAndGroup {} {} {}", processDefinitionId, userId, groupId);
        }
        return getData().values().stream().filter(item -> {
            if (item.getProcessDefinitionId() == null) {
                return false;
            }
            if (!item.getProcessDefinitionId().equals(processDefinitionId)) {
                return false;
            }
            if (userId != null && !userId.equals(item.getUserId())) {
                return false;
            }
            if (groupId != null && !groupId.equals(item.getGroupId())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByScopeIdScopeTypeUserGroupAndType(String scopeId, String scopeType, String userId, String groupId,
                    String type) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinkByScopeIdScopeTypeUserGroupAndType {} {} {} {} {}", scopeId, scopeType, userId, groupId, type);
        }
        return getData().values().stream().filter(item -> {
            if (item.getScopeId() == null || item.getScopeType() == null) {
                return false;
            }
            if (!item.getScopeId().equals(scopeId)) {
                return false;
            }
            if (!item.getScopeType().equals(scopeType)) {
                return false;
            }
            if (userId != null && !userId.equals(item.getUserId())) {
                return false;
            }
            if (groupId != null && !groupId.equals(item.getGroupId())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(String scopeDefinitionId, String scopeType, String userId,
                    String groupId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup {} {} {} {}", scopeDefinitionId, scopeType, userId, groupId);
        }

        return getData().values().stream().filter(item -> {
            if (item.getScopeDefinitionId() == null || item.getScopeType() == null) {
                return false;
            }
            if (!item.getScopeDefinitionId().equals(scopeDefinitionId)) {
                return false;
            }
            if (!item.getScopeType().equals(scopeType)) {
                return false;
            }
            if (userId != null && !userId.equals(item.getUserId())) {
                return false;
            }
            if (groupId != null && !groupId.equals(item.getGroupId())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(IdentityLinkEntity entity) {
        doDelete(entity);
    }

    @Override
    public void deleteIdentityLinksByTaskId(String taskId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteIdentityLinksByTaskId {}", taskId);
        }

        getData().entrySet().removeIf(entry -> entry.getValue().getTaskId() != null && entry.getValue().getTaskId().equals(taskId));
    }

    @Override
    public void deleteIdentityLinksByProcDef(String processDefId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteIdentityLinksByProcDef {}", processDefId);
        }

        getData().entrySet()
                        .removeIf(entry -> entry.getValue().getProcessDefinitionId() != null && entry.getValue().getProcessDefinitionId().equals(processDefId));
    }

    @Override
    public void deleteIdentityLinksByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteIdentityLinksByProcessInstanceId {}", processInstanceId);
        }

        getData().entrySet().removeIf(
                        entry -> entry.getValue().getProcessInstanceId() != null && entry.getValue().getProcessInstanceId().equals(processInstanceId));
    }

    @Override
    public void deleteIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteIdentityLinksByScopeIdAndScopeType {} {}", scopeId, scopeType);
        }

        getData().entrySet().removeIf(entry -> entry.getValue().getScopeId() != null && entry.getValue().getScopeId().equals(scopeId)
                        && entry.getValue().getScopeType() != null && entry.getValue().getScopeType().equals(scopeType));
    }

    @Override
    public void deleteIdentityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteIdentityLinksByScopeDefinitionIdAndScopeType {} {}", scopeDefinitionId, scopeType);
        }

        getData().entrySet()
                        .removeIf(entry -> entry.getValue().getScopeDefinitionId() != null && entry.getValue().getScopeDefinitionId().equals(scopeDefinitionId)
                                        && entry.getValue().getScopeType() != null && entry.getValue().getScopeType().equals(scopeType));
    }

    @Override
    public void bulkDeleteIdentityLinksForScopeIdsAndScopeType(Collection<String> scopeIds, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("bulkDeleteIdentityLinksForScopeIdsAndScopeType {} {}", scopeIds, scopeType);
        }
        if (scopeIds == null) {
            return;
        }
        getData().entrySet().removeIf(entry -> {
            if (entry.getValue().getScopeType() == null) {
                return false;
            }
            if (entry.getValue().getScopeDefinitionId() == null) {
                return false;
            }
            if (!entry.getValue().getScopeType().equals(scopeType)) {
                return false;
            }
            if (!scopeIds.contains(entry.getValue().getScopeDefinitionId())) {
                return false;
            }

            return true;

        });
    }
}
