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
package org.flowable.dmn.engine.impl.persistence.tenant;

import java.util.Collections;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdManager;
import org.flowable.common.engine.impl.tenant.ExecuteChangeTenantIdCmd;
import org.flowable.common.engine.impl.tenant.SimulateChangeTenantIdCmd;
import org.flowable.dmn.api.DmnChangeTenantIdEntityTypes;

/**
 * @author Filip Hrisafov
 */
public class DmnMyBatisChangeTenantIdManager implements ChangeTenantIdManager {

    protected final CommandExecutor commandExecutor;

    public DmnMyBatisChangeTenantIdManager(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public ChangeTenantIdResult simulate(ChangeTenantIdRequest request) {
        return commandExecutor.execute(
                new SimulateChangeTenantIdCmd(request, ScopeTypes.DMN, Collections.singleton(DmnChangeTenantIdEntityTypes.HISTORIC_DECISION_EXECUTIONS)));
    }

    @Override
    public ChangeTenantIdResult complete(ChangeTenantIdRequest request) {
        return commandExecutor.execute(
                new ExecuteChangeTenantIdCmd(request, ScopeTypes.DMN, Collections.singleton(DmnChangeTenantIdEntityTypes.HISTORIC_DECISION_EXECUTIONS)));
    }

}
