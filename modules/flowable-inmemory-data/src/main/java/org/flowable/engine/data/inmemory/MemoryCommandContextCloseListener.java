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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryCommandContextCloseListener implements CommandContextCloseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCommandContextCloseListener.class);

    @Override
    public void closing(CommandContext commandContext) {
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
    }

    @Override
    public void closed(CommandContext commandContext) {
        // do not ask DataManagers to clean up here, let the command stack run
        // fully.
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
        ProcessEngineConfigurationImpl config = CommandContextUtil.getProcessEngineConfiguration();
        if (!(config instanceof MemoryDataProcessEngineConfiguration)) {
            LOGGER.warn("Attempt to use {} as CommandContextCloseListener but ProcessEngineConfiguration is not an instance of {}!", this.getClass().getName(),
                            MemoryDataProcessEngineConfiguration.class.getName());
            return;
        }
        ((MemoryDataProcessEngineConfiguration) config).notifyDataManagers(MemoryDataManagerEvent.FAILURE);
    }

    @Override
    public Integer order() {
        return 20;
    }

    @Override
    public boolean multipleAllowed() {
        return false;
    }

}
