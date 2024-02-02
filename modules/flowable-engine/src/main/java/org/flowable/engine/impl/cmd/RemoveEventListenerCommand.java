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
package org.flowable.engine.impl.cmd;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * Command that removes an event-listener from the process engine.
 * 
 * @author Frederik Heremans
 */
public class RemoveEventListenerCommand implements Command<Void> {

    protected FlowableEventListener listener;

    public RemoveEventListenerCommand(FlowableEventListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (listener == null) {
            throw new FlowableIllegalArgumentException("listener is null.");
        }

        CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().removeEventListener(listener);

        return null;
    }

}
