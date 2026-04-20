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

import org.flowable.cmmn.api.runtime.IntentEventListenerInstance;
import org.flowable.cmmn.api.runtime.IntentEventListenerInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Tijs Rademakers
 */
public class IntentEventListenerInstanceQueryImpl implements IntentEventListenerInstanceQuery {

    protected PlanItemInstanceQuery innerQuery;

    public IntentEventListenerInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        innerQuery = new PlanItemInstanceQueryImpl(commandExecutor, cmmnEngineConfiguration)
                .planItemDefinitionType(PlanItemDefinitionType.INTENT_EVENT_LISTENER);
    }

    @Override
    public IntentEventListenerInstanceQuery id(String id) {
        innerQuery.planItemInstanceId(id);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery caseInstanceId(String caseInstanceId) {
        innerQuery.caseInstanceId(caseInstanceId);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery caseDefinitionId(String caseDefinitionId) {
        innerQuery.caseDefinitionId(caseDefinitionId);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery elementId(String elementId) {
        innerQuery.planItemInstanceElementId(elementId);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery planItemDefinitionId(String planItemDefinitionId) {
        innerQuery.planItemDefinitionId(planItemDefinitionId);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery name(String name) {
        innerQuery.planItemInstanceName(name);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery stageInstanceId(String stageInstanceId) {
        innerQuery.stageInstanceId(stageInstanceId);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery stateAvailable() {
        innerQuery.planItemInstanceStateAvailable();
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery stateSuspended() {
        innerQuery.planItemInstanceState(PlanItemInstanceState.SUSPENDED);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery orderByName() {
        innerQuery.orderByName();
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery asc() {
        innerQuery.asc();
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery desc() {
        innerQuery.desc();
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery orderBy(QueryProperty property) {
        innerQuery.orderBy(property);
        return this;
    }

    @Override
    public IntentEventListenerInstanceQuery orderBy(QueryProperty property, NullHandlingOnOrder nullHandlingOnOrder) {
        innerQuery.orderBy(property, nullHandlingOnOrder);
        return this;
    }

    @Override
    public long count() {
        return innerQuery.count();
    }

    @Override
    public IntentEventListenerInstance singleResult() {
        PlanItemInstance instance = innerQuery.singleResult();
        return IntentEventListenerInstanceImpl.fromPlanItemInstance(instance);
    }

    @Override
    public List<IntentEventListenerInstance> list() {
        return convertPlanItemInstances(innerQuery.list());
    }

    @Override
    public List<IntentEventListenerInstance> listPage(int firstResult, int maxResults) {
        return convertPlanItemInstances(innerQuery.listPage(firstResult, maxResults));
    }

    protected List<IntentEventListenerInstance> convertPlanItemInstances(List<PlanItemInstance> instances) {
        if (instances == null) {
            return null;
        }
        return instances.stream().map(IntentEventListenerInstanceImpl::fromPlanItemInstance).collect(Collectors.toList());
    }
}
