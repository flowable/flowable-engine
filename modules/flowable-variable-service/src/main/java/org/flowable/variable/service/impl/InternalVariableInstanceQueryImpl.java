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
package org.flowable.variable.service.impl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.db.SingleCachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.variable.service.InternalVariableInstanceQuery;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;

/**
 * @author Filip Hrisafov
 */
public class InternalVariableInstanceQueryImpl
        implements InternalVariableInstanceQuery, CachedEntityMatcher<VariableInstanceEntity>, SingleCachedEntityMatcher<VariableInstanceEntity> {

    protected final VariableInstanceDataManager dataManager;

    public InternalVariableInstanceQueryImpl(VariableInstanceDataManager dataManager) {
        this.dataManager = dataManager;
    }

    protected String id;
    protected String taskId;
    protected Collection<String> taskIds;
    protected String processInstanceId;
    protected String executionId;
    protected Collection<String> executionIds;
    protected boolean withoutTaskId;
    protected String scopeId;
    protected Collection<String> scopeIds;
    protected String subScopeId;
    protected Collection<String> subScopeIds;
    protected boolean withoutSubScopeId;
    protected String scopeType;
    protected Collection<String> scopeTypes;
    protected String name;
    protected Collection<String> names;

    @Override
    public InternalVariableInstanceQuery id(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new FlowableIllegalArgumentException("id is empty");
        }
        this.id = id;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery taskId(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            throw new FlowableIllegalArgumentException("taskId is empty");
        }

        if (withoutTaskId) {
            throw new FlowableIllegalArgumentException("Cannot combine taskId(String) with withoutTaskId() in the same query");
        }

        this.taskId = taskId;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery taskIds(Collection<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("taskIds is null or empty");
        }

        if (withoutTaskId) {
            throw new FlowableIllegalArgumentException("Cannot combine taskIds(Collection) with withoutTaskId() in the same query");
        }
        this.taskIds = taskIds;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery processInstanceId(String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableIllegalArgumentException("processInstanceId is empty");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery executionId(String executionId) {
        if (StringUtils.isEmpty(executionId)) {
            throw new FlowableIllegalArgumentException("executionId is empty");
        }
        this.executionId = executionId;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery executionIds(Collection<String> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("executionIds is null or empty");
        }
        this.executionIds = executionIds;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery withoutTaskId() {
        if (taskId != null || taskIds != null) {
            throw new FlowableIllegalArgumentException("Cannot combine withoutTaskId() with task(String) or taskIds(Collection) in the same query");
        }
        this.withoutTaskId = true;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery scopeId(String scopeId) {
        if (StringUtils.isEmpty(scopeId)) {
            throw new FlowableIllegalArgumentException("scopeId is empty");
        }
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery scopeIds(Collection<String> scopeIds) {
        this.scopeIds = scopeIds;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery subScopeId(String subScopeId) {
        if (StringUtils.isEmpty(subScopeId)) {
            throw new FlowableIllegalArgumentException("subScopeId is empty");
        }

        if (withoutSubScopeId) {
            throw new FlowableIllegalArgumentException("Cannot combine subScopeId(String) with withoutSubScopeId() in the same query");
        }

        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery subScopeIds(Collection<String> subScopeIds) {
        if (withoutSubScopeId) {
            throw new FlowableIllegalArgumentException("Cannot combine subScopeIds with withoutSubScopeId in the same query");
        }

        this.subScopeIds = subScopeIds;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery withoutSubScopeId() {
        if (subScopeId != null) {
            throw new FlowableIllegalArgumentException("Cannot combine withoutSubScopeId() with subScopeId(String) in the same query");
        }
        this.withoutSubScopeId = true;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery scopeType(String scopeType) {
        if (StringUtils.isEmpty(scopeType)) {
            throw new FlowableIllegalArgumentException("scopeType is empty");
        }
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery scopeTypes(Collection<String> scopeTypes) {
        this.scopeTypes = scopeTypes;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery name(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new FlowableIllegalArgumentException("name is empty");
        }
        this.name = name;
        return this;
    }

    @Override
    public InternalVariableInstanceQuery names(Collection<String> names) {
        this.names = names;
        return this;
    }

    @Override
    public List<VariableInstanceEntity> list() {
        return dataManager.findVariablesInstancesByQuery(this);
    }

    @Override
    public VariableInstanceEntity singleResult() {
        return dataManager.findVariablesInstanceByQuery(this);
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public Collection<String> getTaskIds() {
        return taskIds;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Collection<String> getExecutionIds() {
        return executionIds;
    }

    public boolean isWithoutTaskId() {
        return withoutTaskId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public Collection<String> getScopeIds() {
        return scopeIds;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public Collection<String> getSubScopeIds() {
        return subScopeIds;
    }

    public boolean isWithoutSubScopeId() {
        return withoutSubScopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public Collection<String> getScopeTypes() {
        return scopeTypes;
    }

    public String getName() {
        return name;
    }

    public Collection<String> getNames() {
        return names;
    }

    // This method is needed because we have a different way of querying list and single objects via MyBatis.
    // Querying lists wraps the object in a ListQueryParameterObject
    public InternalVariableInstanceQueryImpl getParameter() {
        return this;
    }

    @Override
    public boolean isRetained(Collection<VariableInstanceEntity> databaseEntities, Collection<CachedEntity> cachedEntities,
            VariableInstanceEntity entity, Object param) {
        return isRetained(entity, (InternalVariableInstanceQueryImpl) param);
    }

    @Override
    public boolean isRetained(VariableInstanceEntity entity, Object param) {
        return isRetained(entity, (InternalVariableInstanceQueryImpl) param);
    }

    public boolean isRetained(VariableInstanceEntity entity, InternalVariableInstanceQueryImpl param) {

        if (param.executionId != null && !param.executionId.equals(entity.getExecutionId())) {
            return false;
        }

        if (param.scopeId != null && !param.scopeId.equals(entity.getScopeId())) {
            return false;
        }

        if (param.scopeIds != null && !param.scopeIds.contains(entity.getScopeId())) {
            return false;
        }

        if (param.taskId != null && !param.taskId.equals(entity.getTaskId())) {
            return false;
        }

        if (param.processInstanceId != null && !param.processInstanceId.equals(entity.getProcessInstanceId())) {
            return false;
        }

        if (param.withoutTaskId && entity.getTaskId() != null) {
            return false;
        }

        if (param.subScopeId != null && !param.subScopeId.equals(entity.getSubScopeId())) {
            return false;
        }

        if (param.subScopeIds != null && !param.subScopeIds.contains(entity.getSubScopeId())) {
            return false;
        }

        if (param.withoutSubScopeId && entity.getSubScopeId() != null) {
            return false;
        }

        if (param.scopeType != null && !param.scopeType.equals(entity.getScopeType())) {
            return false;
        }

        if (param.scopeTypes != null && !param.scopeTypes.isEmpty() && !param.scopeTypes.contains(entity.getScopeType())) {
            return false;
        }

        if (param.id != null && !param.id.equals(entity.getId())) {
            return false;
        }

        if (param.taskIds != null && !param.taskIds.contains(entity.getTaskId())) {
            return false;
        }

        if (param.executionIds != null && !param.executionIds.contains(entity.getExecutionId())) {
            return false;
        }

        if (param.name != null && !param.name.equals(entity.getName())) {
            return false;
        }

        if (param.names != null && !param.names.isEmpty() && !param.names.contains(entity.getName())) {
            return false;
        }

        return true;
    }
}
