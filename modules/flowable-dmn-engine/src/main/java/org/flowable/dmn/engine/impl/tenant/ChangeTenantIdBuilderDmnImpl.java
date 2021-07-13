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

import org.flowable.dmn.engine.impl.cmd.ChangeTenantIdDmnCompleteCmd;
import org.flowable.dmn.engine.impl.cmd.ChangeTenantIdDmnSimulateCmd;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

public class ChangeTenantIdBuilderDmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;

    public ChangeTenantIdBuilderDmnImpl(CommandExecutor commandExecutor, String fromTenantId,
            String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions(
            boolean onlyInstancesFromDefaultTenantDefinitionsEnabled) {
        this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitionsEnabled;
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        return commandExecutor.execute(new ChangeTenantIdDmnSimulateCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        return commandExecutor.execute(new ChangeTenantIdDmnCompleteCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

}