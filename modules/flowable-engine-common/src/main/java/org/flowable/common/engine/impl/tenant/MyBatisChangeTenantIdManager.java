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
package org.flowable.common.engine.impl.tenant;

import java.util.Set;

import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Filip Hrisafov
 */
public class MyBatisChangeTenantIdManager implements ChangeTenantIdManager {

    protected final CommandExecutor commandExecutor;
    protected final String engineScopeType;
    protected final Set<String> entityTypes;

    public MyBatisChangeTenantIdManager(CommandExecutor commandExecutor, String engineScopeType, Set<String> entityTypes) {
        this.commandExecutor = commandExecutor;
        this.engineScopeType = engineScopeType;
        this.entityTypes = entityTypes;
    }

    @Override
    public ChangeTenantIdResult simulate(ChangeTenantIdBuilderImpl builder) {
        return commandExecutor.execute(new SimulateChangeTenantIdCmd(builder, engineScopeType, entityTypes));
    }

    @Override
    public ChangeTenantIdResult complete(ChangeTenantIdBuilderImpl builder) {
        return commandExecutor.execute(new ExecuteChangeTenantIdCmd(builder, engineScopeType, entityTypes));
    }
}
