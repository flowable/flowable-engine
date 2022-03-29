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
package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * This command dispatches an event within the case engine.
 *
 * @author Micha Kiener
 */
public class DispatchEventCommand implements Command<Void> {

    protected FlowableEvent event;

    public DispatchEventCommand(FlowableEvent event) {
        this.event = event;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (event == null) {
            throw new FlowableIllegalArgumentException("The event to be dispatched must not be null.");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        FlowableEventDispatcher eventDispatcher = cmmnEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(event, cmmnEngineConfiguration.getEngineCfgKey());
        } else {
            throw new FlowableException("Message dispatcher is disabled, cannot dispatch event.");
        }

        return null;
    }
}
