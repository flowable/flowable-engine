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

package org.flowable.content.engine.impl.tenant;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.cmd.ChangeTenantIdCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdRequest;
import org.flowable.content.engine.ContentEngineConfiguration;

public class ChangeTenantIdBuilderContentImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private final String defaultTenantId;
    private final Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions;

    public ChangeTenantIdBuilderContentImpl(CommandExecutor commandExecutor,
            ContentEngineConfiguration contentEngineConfiguration, String fromTenantId, String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
        this.defaultTenantId = contentEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(fromTenantId,
                null, null);
        this.mapOfEntitiesAndFunctions = Collections.singletonMap(ChangeTenantIdEntityTypes.CONTENT_ITEM_INSTANCES,
                r -> contentEngineConfiguration.getContentItemEntityManager().changeTenantIdContentItemInstances(r));
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions() {
        throw new UnsupportedOperationException("Content items do not have definitions. Unsupported builder option.");
    }

    @Override
    public ChangeTenantIdResult simulate() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .dryRun(true) // This is a drill.
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .dryRun(false) // This is NOT a drill!
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

}