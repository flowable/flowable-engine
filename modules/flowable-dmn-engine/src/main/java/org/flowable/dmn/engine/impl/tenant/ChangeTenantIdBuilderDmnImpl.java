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

package org.flowable.dmn.engine.impl.tenant;

import org.flowable.dmn.engine.DmnEngineConfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.cmd.ChangeTenantIdCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdRequest;

public class ChangeTenantIdBuilderDmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;
    private final String defaultTenantId;
    private final Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions;

    public ChangeTenantIdBuilderDmnImpl(CommandExecutor commandExecutor, DmnEngineConfiguration dmnEngineConfiguration,
            String fromTenantId, String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
        this.defaultTenantId = dmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(fromTenantId,
        ScopeTypes.DMN, null);
        this.mapOfEntitiesAndFunctions = Collections.singletonMap(ChangeTenantIdEntityTypes.HISTORIC_DECISION_EXECUTIONS,
                r -> dmnEngineConfiguration.getHistoricDecisionExecutionEntityManager()
                        .changeTenantIdHistoricDecisionExecutions(r));
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions() {
        this.onlyInstancesFromDefaultTenantDefinitions = true;
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.DMN).dryRun(true) // This is a drill.
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.DMN).dryRun(false) // This is NOT a drill!
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

}