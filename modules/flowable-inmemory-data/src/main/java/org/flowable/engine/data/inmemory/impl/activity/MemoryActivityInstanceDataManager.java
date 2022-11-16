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
package org.flowable.engine.data.inmemory.impl.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.engine.data.inmemory.AbstractMemoryDataManager;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.data.inmemory.util.QueryUtil;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.runtime.ActivityInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In memory implementation of {@link ActivityInstanceDataManager}
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryActivityInstanceDataManager extends AbstractMemoryDataManager<ActivityInstanceEntity> implements ActivityInstanceDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryActivityInstanceDataManager.class);

    // Maintain a map of activities by execution for faster access
    private Map<String, List<ActivityInstanceEntity>> byExecutionId;

    public MemoryActivityInstanceDataManager(MapProvider mapProvider, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(LOGGER, mapProvider, processEngineConfiguration.getIdGenerator());
        byExecutionId = mapProvider.create(1023, 0.6f);
    }

    @Override
    public ActivityInstanceEntity create() {
        return new ActivityInstanceEntityImpl();
    }

    @Override
    public void insert(ActivityInstanceEntity entity) {
        doInsert(entity);
        if (entity.getExecutionId() != null) {
            byExecutionId.computeIfAbsent(entity.getExecutionId(), (key) -> new ArrayList<>()).add(entity);
        }
    }

    @Override
    public ActivityInstanceEntity update(ActivityInstanceEntity entity) {
        return doUpdate(entity);
    }

    @Override
    public ActivityInstanceEntity findById(String entityId) {
        return doFindById(entityId);
    }

    @Override
    public void delete(String id) {
        doDelete(id, (d) -> {
            handleDeleted(d);
        });
    }

    @Override
    public void delete(ActivityInstanceEntity entity) {
        doDelete(entity);
        if (entity.getExecutionId() != null) {
            handleDeleted(entity);
        }
    }

    @Override
    public List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findUnfinishedActivityInstancesByExecutionAndActivityId {} {}", executionId, activityId);
        }

        return findActivityInstancesByExecutionIdAndActivityId(executionId, activityId).stream().filter(item -> item.getEndTime() == null)
                        .collect(Collectors.toList());
    }

    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByExecutionIdAndActivityId(String executionId, String activityId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findActivityInstancesByExecutionIdAndActivityId {} {}", executionId, activityId);
        }
        List<ActivityInstanceEntity> items = byExecutionId.get(executionId);
        return items == null ? Collections.emptyList()
                        : items.stream().filter(item -> item.getActivityId() != null && item.getActivityId().equals(activityId)).collect(Collectors.toList());
    }

    @Override
    public ActivityInstanceEntity findActivityInstanceByTaskId(String taskId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findActivityInstanceByTaskId {}", taskId);
        }
        return getData().values().stream().filter(item -> item.getTaskId() != null && item.getTaskId().equals(taskId)).findFirst().orElse(null);
    }

    @Override
    public List<ActivityInstanceEntity> findActivityInstancesByProcessInstanceId(String processInstanceId, boolean includeDeleted) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findActivityInstancesByProcessInstanceId {} {}", processInstanceId, includeDeleted);
        }
        List<ActivityInstanceEntity> r = getData().values().stream().filter(item -> {
            if (item.getProcessInstanceId() == null || !item.getProcessInstanceId().equals(processInstanceId)) {
                return false;
            }
            if (!includeDeleted && item.isDeleted()) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        r.sort(Comparator.comparing(ActivityInstanceEntity::getStartTime).thenComparing(ActivityInstanceEntity::getTransactionOrder,
                        Comparator.nullsFirst(Comparator.naturalOrder())));
        return r;
    }

    @Override
    public long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findActivityInstanceCountByQueryCriteria {}", activityInstanceQuery);
        }
        return findActivityInstancesByQueryCriteria(activityInstanceQuery).size();
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("findActivityInstancesByQueryCriteria {}", activityInstanceQuery);
        }

        if (activityInstanceQuery == null) {
            return Collections.emptyList();
        }

        return sortAndLimit(getData().values().stream().filter(item -> {
            return filterActivityInstance(item, activityInstanceQuery);
        }).collect(Collectors.toList()), activityInstanceQuery);
    }

    @Override
    public List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ActivityInstanceDataManager implementation!");
    }

    @Override
    public long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new IllegalStateException("Native query not supported by this ActivityInstanceDataManager implementation!");
    }

    @Override
    public void deleteActivityInstancesByProcessInstanceId(String processInstanceId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("deleteActivityInstancesByProcessInstanceId {}", processInstanceId);
        }

        getData().entrySet().removeIf(item -> {
            ActivityInstanceEntity v = item.getValue();
            if (v.getProcessInstanceId() == null) {
                return false;
            }
            if (!v.getProcessInstanceId().equals(processInstanceId)) {
                return false;
            }
            handleDeleted(v);
            return true;
        });
    }

    private void handleDeleted(ActivityInstanceEntity deleted) {
        List<ActivityInstanceEntity> items = byExecutionId.get(deleted.getExecutionId());
        if (items != null) {
            items.removeIf(item -> item.getId().equals(deleted.getId()));
            if (items.isEmpty()) {
                byExecutionId.remove(deleted.getExecutionId());
            }
        }
    }

    private List<ActivityInstance> sortAndLimit(List<ActivityInstance> collect, ActivityInstanceQueryImpl query) {

        if (collect == null || collect.isEmpty()) {
            return collect;
        }

        return sortAndPaginate(collect, ActivityInstanceComparator.resolve(query.getOrderBy()), query.getFirstResult(), query.getMaxResults());
    }

    private boolean filterActivityInstance(ActivityInstanceEntity item, ActivityInstanceQueryImpl query) {
        Boolean retVal = null; // Used to keep track of true/false return for
                               // queries
        if (query.getProcessInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessInstanceId().equals(item.getProcessInstanceId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getActivityInstanceId() != null) {
            retVal = QueryUtil.matchReturn(query.getActivityInstanceId().equals(item.getId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getExecutionId() != null) {
            retVal = QueryUtil.matchReturn(query.getExecutionId().equals(item.getExecutionId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getProcessDefinitionId() != null) {
            retVal = QueryUtil.matchReturn(query.getProcessDefinitionId().equals(item.getProcessDefinitionId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getActivityId() != null) {
            retVal = QueryUtil.matchReturn(query.getActivityId().equals(item.getActivityId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getActivityName() != null) {
            retVal = QueryUtil.matchReturn(query.getActivityName().equals(item.getActivityName()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getActivityType() != null) {
            retVal = QueryUtil.matchReturn(query.getActivityType().equals(item.getActivityType()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getAssignee() != null) {
            retVal = QueryUtil.matchReturn(query.getAssignee().equals(item.getAssignee()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.getTenantId() != null) {
            retVal = QueryUtil.matchReturn(query.getTenantId().equals(item.getTenantId()), false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.getTenantIdLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getTenantIdLike(), item.getTenantId()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        if (query.isWithoutTenantId()) {
            retVal = QueryUtil.matchReturn(item.getTenantId() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.isUnfinished()) {
            retVal = QueryUtil.matchReturn(item.getEndTime() == null, false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.isFinished()) {
            retVal = QueryUtil.matchReturn(item.getEndTime() != null, false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.getDeleteReason() != null) {
            retVal = QueryUtil.matchReturn(query.getDeleteReason().equals(item.getDeleteReason()), false);
            if (retVal != null) {
                return retVal;
            }
        }
        if (query.getDeleteReasonLike() != null) {
            retVal = QueryUtil.matchReturn(QueryUtil.queryLike(query.getDeleteReasonLike(), item.getDeleteReason()), false);
            if (retVal != null) {
                return retVal;
            }
        }

        return true;
    }
}
