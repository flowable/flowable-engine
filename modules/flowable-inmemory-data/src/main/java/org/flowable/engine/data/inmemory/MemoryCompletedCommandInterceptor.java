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
package org.flowable.engine.data.inmemory;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * Informs Memory
 * {@link org.flowable.common.engine.impl.persistence.entity.data.DataManager}
 * implementations of failures in command execution.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryCompletedCommandInterceptor extends AbstractCommandInterceptor {

    @Override
    public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {
        Context.getCommandContext().addCloseListener(new MemoryCommandContextCloseListener());
        T r = next.execute(config, command, commandExecutor);

        // Only call completion if command succeeds,
        // MemoryDataManagerCommandContextFailureListener handles the failure
        ProcessEngineConfigurationImpl processEngineConfig = CommandContextUtil.getProcessEngineConfiguration();

        if ((processEngineConfig instanceof MemoryDataProcessEngineConfiguration)) {
            ((MemoryDataProcessEngineConfiguration) processEngineConfig).notifyDataManagers(MemoryDataManagerEvent.COMPLETE);
        }

        return r;

    }
}
