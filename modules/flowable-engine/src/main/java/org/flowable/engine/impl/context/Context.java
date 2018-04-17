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

package org.flowable.engine.impl.context;

import org.flowable.common.engine.impl.cfg.TransactionContext;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.transaction.TransactionContextHolder;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * Quick access methods (only useable when within a command execution) to the current 
 * 
 * - {@link org.flowable.common.engine.impl.interceptor.CommandContext},
 * - {@link ProcessEngineConfigurationImpl}
 * - {@link org.flowable.common.engine.impl.cfg.TransactionContext}
 * 
 * Note that this class is here for backwards compatibility.
 * Use the engine-independent {@link org.flowable.common.engine.impl.context.Context} and {@link CommandContextUtil} when possible.
 */
public class Context {

    public static CommandContext getCommandContext() {
        return CommandContextUtil.getCommandContext();
    }

    public static FlowableEngineAgenda getAgenda() {
        return CommandContextUtil.getAgenda();
    }

    public static ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return CommandContextUtil.getProcessEngineConfiguration();
    }
    
    public static ProcessEngineConfigurationImpl getProcessEngineConfiguration(CommandContext commandContext) {
        return CommandContextUtil.getProcessEngineConfiguration(commandContext);
    }
    
    public static TransactionContext getTransactionContext() {
        return TransactionContextHolder.getTransactionContext();
    }

    public static Flowable5CompatibilityHandler getFlowable5CompatibilityHandler() {
        return getProcessEngineConfiguration().getFlowable5CompatibilityHandler();
    }

    public static Flowable5CompatibilityHandler getFallbackFlowable5CompatibilityHandler() {
        return Flowable5CompatibilityContext.getFallbackFlowable5CompatibilityHandler();
    }

}
