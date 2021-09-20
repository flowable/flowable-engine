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

package org.flowable.form.engine.impl.tenant;

import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.FORM_INSTANCES;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.cmd.ChangeTenantIdCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdRequest;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdResult;
import org.flowable.form.engine.FormEngineConfiguration;

public class ChangeTenantIdBuilderFormInstanceImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;
    private final String defaultTenantId;
    private final Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions;

    public ChangeTenantIdBuilderFormInstanceImpl(CommandExecutor commandExecutor,
            FormEngineConfiguration formEngineConfiguration, String fromTenantId, String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
        this.defaultTenantId = formEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(fromTenantId,
                ScopeTypes.FORM, null);
        this.mapOfEntitiesAndFunctions = Collections.singletonMap(FORM_INSTANCES,
                r -> formEngineConfiguration.getFormInstanceEntityManager().changeTenantIdFormInstances(r));
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions() {
        this.onlyInstancesFromDefaultTenantDefinitions = true;
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        ChangeTenantIdRequest changeTenantIdRequestBpmn = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.BPMN).dryRun(true) // This is a drill.
                .build();
        ChangeTenantIdResult resultBpmn = commandExecutor
                .execute(new ChangeTenantIdCmd(changeTenantIdRequestBpmn, mapOfEntitiesAndFunctions));

        ChangeTenantIdRequest changeTenantIdRequestCmmn = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.CMMN).dryRun(true) // This is a drill.
                .build();
        ChangeTenantIdResult resultCmmn = commandExecutor
                .execute(new ChangeTenantIdCmd(changeTenantIdRequestCmmn, mapOfEntitiesAndFunctions));

        return new DefaultChangeTenantIdResult(Collections.singletonMap(FORM_INSTANCES,
                resultBpmn.getChangedInstances(FORM_INSTANCES) + resultCmmn.getChangedInstances(FORM_INSTANCES)));
    }

    @Override
    public ChangeTenantIdResult complete() {
        ChangeTenantIdRequest changeTenantIdRequestBpmn = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.BPMN).dryRun(false) // This is NOT a drill!
                .build();
        ChangeTenantIdResult resultBpmn = commandExecutor
                .execute(new ChangeTenantIdCmd(changeTenantIdRequestBpmn, mapOfEntitiesAndFunctions));

        ChangeTenantIdRequest changeTenantIdRequestCmmn = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.CMMN).dryRun(false) // This is NOT a drill!
                .build();
        ChangeTenantIdResult resultCmmn = commandExecutor
                .execute(new ChangeTenantIdCmd(changeTenantIdRequestCmmn, mapOfEntitiesAndFunctions));

        return new DefaultChangeTenantIdResult(Collections.singletonMap(FORM_INSTANCES,
                resultBpmn.getChangedInstances(FORM_INSTANCES) + resultCmmn.getChangedInstances(FORM_INSTANCES)));
    }

}