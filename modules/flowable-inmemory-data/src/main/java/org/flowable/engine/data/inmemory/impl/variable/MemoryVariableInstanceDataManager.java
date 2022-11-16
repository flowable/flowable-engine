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
package org.flowable.engine.data.inmemory.impl.variable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.exceptions.TooManyResultsException;
import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.InternalVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.VariableInstanceQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-Memory {@link VariableInstanceDataManager} implementation.
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryVariableInstanceDataManager extends AbstractMemoryDataManager<VariableInstanceEntity> implements VariableInstanceDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryVariableInstanceDataManager.class);

    public MemoryVariableInstanceDataManager(MapProvider mapProvider, VariableServiceConfiguration variableServiceConfiguration) {
        super(LOGGER, mapProvider, variableServiceConfiguration.getIdGenerator());
    }

    @Override
    public VariableInstanceEntity create() {
        VariableInstanceEntityImpl variableInstanceEntity = new VariableInstanceEntityImpl();
        variableInstanceEntity.setRevision(0);
        return variableInstanceEntity;
    }

    @Override
    public VariableInstanceEntity findById(String entityId) {
        return doFindById(entityId);
    }

    public List<VariableInstanceEntity> findByProcessId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findByProcessId {}", processInstanceId);
        }
        return getData().values().stream().filter(item -> processInstanceId.equals(item.getProcessInstanceId())).collect(Collectors.toList());
    }

    @Override
    public void insert(VariableInstanceEntity entity) {
        doInsert(entity);
    }

    @Override
    public VariableInstanceEntity update(VariableInstanceEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public void delete(String id) {
        doDelete(id);
    }

    @Override
    public void delete(VariableInstanceEntity entity) {
        doDelete(entity);
    }

    @Override
    public List<VariableInstanceEntity> findVariablesInstancesByQuery(InternalVariableInstanceQueryImpl internalVariableInstanceQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findVariablesInstancesByQuery {}", internalVariableInstanceQuery);
        }
        return getData().values().stream().filter(var -> {
            if (internalVariableInstanceQuery.getId() != null && !internalVariableInstanceQuery.getId().equals(var.getId())) {
                return false;
            }

            if (internalVariableInstanceQuery.getTaskId() != null && !internalVariableInstanceQuery.getTaskId().equals(var.getTaskId())) {
                return false;
            }

            if (internalVariableInstanceQuery.getTaskIds() != null
                            && !internalVariableInstanceQuery.getTaskIds().stream().anyMatch(tid -> tid.equals(var.getTaskId()))) {
                return false;
            }

            if (internalVariableInstanceQuery.getProcessInstanceId() != null
                            && !internalVariableInstanceQuery.getProcessInstanceId().equals(var.getProcessInstanceId())) {
                return false;
            }

            if (internalVariableInstanceQuery.getExecutionId() != null && !internalVariableInstanceQuery.getExecutionId().equals(var.getExecutionId())) {
                return false;
            }

            if (internalVariableInstanceQuery.getExecutionIds() != null
                            && !internalVariableInstanceQuery.getExecutionIds().stream().anyMatch(exid -> exid.equals(var.getExecutionId()))) {
                return false;
            }

            if (internalVariableInstanceQuery.isWithoutTaskId() && var.getTaskId() != null) {
                return false;
            }

            if (internalVariableInstanceQuery.getScopeId() != null && !internalVariableInstanceQuery.getScopeId().equals(var.getScopeId())) {
                return false;
            }

            if (internalVariableInstanceQuery.getScopeIds() != null && !internalVariableInstanceQuery.getScopeIds().isEmpty()
                            && !internalVariableInstanceQuery.getScopeIds().stream().anyMatch(scid -> scid.equals(var.getScopeId()))) {
                return false;
            }

            if (internalVariableInstanceQuery.getSubScopeId() != null && !internalVariableInstanceQuery.getSubScopeId().equals(var.getSubScopeId())) {
                return false;
            }

            if (internalVariableInstanceQuery.getSubScopeIds() != null && !internalVariableInstanceQuery.getSubScopeIds().isEmpty()
                            && !internalVariableInstanceQuery.getSubScopeIds().stream().anyMatch(sscid -> sscid.equals(var.getSubScopeId()))) {
                return false;
            }

            if (internalVariableInstanceQuery.isWithoutSubScopeId() && var.getSubScopeId() != null) {
                return false;
            }

            if (internalVariableInstanceQuery.getScopeType() != null && !internalVariableInstanceQuery.getScopeType().equals(var.getScopeType())) {
                return false;
            }

            if (internalVariableInstanceQuery.getScopeTypes() != null && !internalVariableInstanceQuery.getScopeTypes().isEmpty()
                            && !internalVariableInstanceQuery.getScopeTypes().stream().anyMatch(st -> st.equals(var.getScopeType()))) {
                return false;
            }

            if (internalVariableInstanceQuery.getName() != null && !internalVariableInstanceQuery.getName().equals(var.getName())) {
                return false;
            }

            if (internalVariableInstanceQuery.getNames() != null && !internalVariableInstanceQuery.getNames().isEmpty()
                            && !internalVariableInstanceQuery.getNames().stream().anyMatch(name -> name.equals(var.getName()))) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public VariableInstanceEntity findVariablesInstanceByQuery(InternalVariableInstanceQueryImpl internalVariableInstanceQuery) {
        List<VariableInstanceEntity> list = findVariablesInstancesByQuery(internalVariableInstanceQuery);

        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() > 1) {
            throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
        } else {
            return null;
        }
    }

    @Override
    public long findVariableInstanceCountByQueryCriteria(VariableInstanceQueryImpl variableInstanceQuery) {
        return findVariableInstancesByQueryCriteria(variableInstanceQuery).size();
    }

    @Override
    public List<VariableInstance> findVariableInstancesByQueryCriteria(VariableInstanceQueryImpl variableInstanceQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findVariableInstancesByQueryCriteria {}", variableInstanceQuery);
        }

        return getData().values().stream().filter(var -> {
            if (variableInstanceQuery.getId() != null && !variableInstanceQuery.getId().equals(var.getId())) {
                return false;
            }

            if (variableInstanceQuery.getTaskId() != null && !variableInstanceQuery.getTaskId().equals(var.getTaskId())) {
                return false;
            }

            if (variableInstanceQuery.getTaskIds() != null && !variableInstanceQuery.getTaskIds().stream().anyMatch(tid -> tid.equals(var.getTaskId()))) {
                return false;
            }

            if (variableInstanceQuery.getProcessInstanceId() != null && !variableInstanceQuery.getProcessInstanceId().equals(var.getProcessInstanceId())) {
                return false;
            }

            if (variableInstanceQuery.getExecutionId() != null && !variableInstanceQuery.getExecutionId().equals(var.getExecutionId())) {
                return false;
            }

            if (variableInstanceQuery.getExecutionIds() != null
                            && !variableInstanceQuery.getExecutionIds().stream().anyMatch(exid -> exid.equals(var.getExecutionId()))) {
                return false;
            }

            if (variableInstanceQuery.isExcludeTaskRelated() && var.getTaskId() != null) {
                return false;
            }

            if (variableInstanceQuery.isExcludeLocalVariables()) {
                if (var.getTaskId() != null) {
                    return false;
                }
                if (var.getSubScopeId() != null) {
                    return false;
                }

                if (var.getExecutionId() != null || var.getProcessInstanceId() != null) {
                    if (var.getExecutionId() == null || !var.getExecutionId().equals(var.getProcessInstanceId())) {
                        // execution id does not equal process id -> not local
                        return false;
                    }
                }
            }

            if (variableInstanceQuery.getScopeId() != null && !variableInstanceQuery.getScopeId().equals(var.getScopeId())) {
                return false;
            }

            if (variableInstanceQuery.getSubScopeId() != null && !variableInstanceQuery.getSubScopeId().equals(var.getSubScopeId())) {
                return false;
            }

            if (variableInstanceQuery.getScopeType() != null && !variableInstanceQuery.getScopeType().equals(var.getScopeType())) {
                return false;
            }

            if (variableInstanceQuery.getVariableName() != null && !variableInstanceQuery.getVariableName().equals(var.getName())) {
                return false;
            }

            if (variableInstanceQuery.getVariableNameLike() != null && !QueryUtil.queryLike(variableInstanceQuery.getVariableNameLike(), var.getName())) {
                return false;
            }

            if (variableInstanceQuery.getQueryVariableValue() != null && !QueryUtil.variableMatches(variableInstanceQuery.getQueryVariableValue(), var)) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteVariablesByTaskId(String taskId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteVariablesByTaskId {}", taskId);
        }

        if (taskId == null) {
            return;
        }
        getData().entrySet().removeIf(e -> taskId.equals(e.getValue().getTaskId()));
    }

    @Override
    public void deleteVariablesByExecutionId(String executionId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteVariablesByTaskId {}", executionId);
        }
        if (executionId == null) {
            return;
        }
        getData().entrySet().removeIf(e -> executionId.equals(e.getValue().getExecutionId()));
    }

    @Override
    public void deleteByScopeIdAndScopeType(String scopeId, String scopeType) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteByScopeIdAndScopeType {} {}", scopeId, scopeType);
        }
        if (scopeId == null || scopeType == null) {
            return;
        }
        getData().entrySet().removeIf(e -> scopeId.equals(e.getValue().getScopeId()) && scopeType.equals(e.getValue().getScopeType()));
    }

    @Override
    public void deleteByScopeIdAndScopeTypes(String scopeId, Collection<String> scopeTypes) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteByScopeIdAndScopeTypes {} {}", scopeId, scopeTypes);
        }
        if (scopeId == null || (scopeTypes == null || scopeTypes.isEmpty())) {
            return;
        }
        getData().entrySet().removeIf(e -> {
            VariableInstanceEntity v = e.getValue();
            if (!scopeId.equals(v.getScopeId())) {
                return false;
            }

            return scopeTypes.stream().anyMatch(st -> st.equals(v.getScopeType()));
        });
    }

    @Override
    public void deleteBySubScopeIdAndScopeTypes(String subScopeId, Collection<String> scopeTypes) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteBySubScopeIdAndScopeTypes {} {}", subScopeId, scopeTypes);
        }
        if (subScopeId == null || (scopeTypes == null || scopeTypes.isEmpty())) {
            return;
        }
        getData().entrySet().removeIf(e -> {
            VariableInstanceEntity v = e.getValue();
            if (!subScopeId.equals(v.getSubScopeId())) {
                return false;
            }

            return scopeTypes.stream().anyMatch(st -> st.equals(v.getScopeType()));
        });
    }

    @Override
    public List<VariableInstance> findVariableInstancesByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ExecutionDataManager implementation!");
    }

    @Override
    public long findVariableInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ExecutionDataManager implementation!");
    }
}
