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

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.SignalEventListenerInstance;
import org.flowable.cmmn.api.runtime.SignalEventListenerInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Tijs Rademakers
 */
public class SignalEventListenerInstanceQueryImpl implements SignalEventListenerInstanceQuery {

    protected PlanItemInstanceQuery innerQuery;

    public SignalEventListenerInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        innerQuery = new PlanItemInstanceQueryImpl(commandExecutor, cmmnEngineConfiguration)
                .planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER);
    }

    @Override
    public SignalEventListenerInstanceQuery id(String id) {
        innerQuery.planItemInstanceId(id);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery caseInstanceId(String caseInstanceId) {
        innerQuery.caseInstanceId(caseInstanceId);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery caseDefinitionId(String caseDefinitionId) {
        innerQuery.caseDefinitionId(caseDefinitionId);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery elementId(String elementId) {
        innerQuery.planItemInstanceElementId(elementId);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery planItemDefinitionId(String planItemDefinitionId) {
        innerQuery.planItemDefinitionId(planItemDefinitionId);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery name(String name) {
        innerQuery.planItemInstanceName(name);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery stageInstanceId(String stageInstanceId) {
        innerQuery.stageInstanceId(stageInstanceId);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery stateAvailable() {
        innerQuery.planItemInstanceStateAvailable();
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery stateSuspended() {
        innerQuery.planItemInstanceState(PlanItemInstanceState.SUSPENDED);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery orderByName() {
        innerQuery.orderByName();
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery asc() {
        innerQuery.asc();
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery desc() {
        innerQuery.desc();
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery orderBy(QueryProperty property) {
        innerQuery.orderBy(property);
        return this;
    }

    @Override
    public SignalEventListenerInstanceQuery orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder) {
        innerQuery.orderBy(property, nullHandlingOnOrder);
        return this;
    }

    @Override
    public long count() {
        return innerQuery.count();
    }

    @Override
    public SignalEventListenerInstance singleResult() {
        PlanItemInstance instance = innerQuery.singleResult();
        return SignalEventListenerInstanceImpl.fromPlanItemInstance(instance);
    }

    @Override
    public List<SignalEventListenerInstance> list() {
        return convertPlanItemInstances(innerQuery.list());
    }

    @Override
    public List<SignalEventListenerInstance> listPage(int firstResult, int maxResults) {
        return convertPlanItemInstances(innerQuery.listPage(firstResult, maxResults));
    }

    protected List<SignalEventListenerInstance> convertPlanItemInstances(List<PlanItemInstance> instances) {
        if (instances == null) {
            return null;
        }
        return instances.stream().map(SignalEventListenerInstanceImpl::fromPlanItemInstance).collect(Collectors.toList());
    }
}
