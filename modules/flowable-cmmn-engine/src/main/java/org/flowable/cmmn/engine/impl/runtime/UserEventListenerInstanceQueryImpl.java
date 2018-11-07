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
package org.flowable.cmmn.engine.impl.runtime;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstanceQuery;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dennis Federico
 */
public class UserEventListenerInstanceQueryImpl implements UserEventListenerInstanceQuery {

    protected PlanItemInstanceQuery innerQuery;

    UserEventListenerInstanceQueryImpl(CommandExecutor commandExecutor) {
        innerQuery = new PlanItemInstanceQueryImpl(commandExecutor).planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER);
    }

    @Override
    public UserEventListenerInstanceQuery id(String id) {
        innerQuery.planItemInstanceId(id);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery caseInstanceId(String caseInstanceId) {
        innerQuery.caseInstanceId(caseInstanceId);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery caseDefinitionId(String caseDefinitionId) {
        innerQuery.caseDefinitionId(caseDefinitionId);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery elementId(String elementId) {
        innerQuery.planItemInstanceElementId(elementId);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery planItemDefinitionId(String planItemDefinitionId) {
        innerQuery.planItemDefinitionId(planItemDefinitionId);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery name(String name) {
        innerQuery.planItemInstanceName(name);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery stageInstanceId(String stageInstanceId) {
        innerQuery.stageInstanceId(stageInstanceId);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery stateAvailable() {
        innerQuery.planItemInstanceStateAvailable();
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery stateSuspended() {
        innerQuery.planItemInstanceState(PlanItemInstanceState.SUSPENDED);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery orderByName() {
        innerQuery.orderByName();
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery asc() {
        innerQuery.asc();
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery desc() {
        innerQuery.desc();
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery orderBy(QueryProperty property) {
        innerQuery.orderBy(property);
        return this;
    }

    @Override
    public UserEventListenerInstanceQuery orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder) {
        innerQuery.orderBy(property, nullHandlingOnOrder);
        return this;
    }

    @Override
    public long count() {
        return innerQuery.count();
    }

    @Override
    public UserEventListenerInstance singleResult() {
        PlanItemInstance instance = innerQuery.singleResult();
        return UserEventListenerInstanceImpl.fromPlanItemInstance(instance);
    }

    @Override
    public List<UserEventListenerInstance> list() {
        return convertPlanItemInstances(innerQuery.list());
    }

    @Override
    public List<UserEventListenerInstance> listPage(int firstResult, int maxResults) {
        return convertPlanItemInstances(innerQuery.listPage(firstResult, maxResults));
    }

    protected List<UserEventListenerInstance> convertPlanItemInstances(List<PlanItemInstance> instances) {
        if (instances == null) {
            return null;
        }
        return instances.stream().map(UserEventListenerInstanceImpl::fromPlanItemInstance).collect(Collectors.toList());
    }
}
