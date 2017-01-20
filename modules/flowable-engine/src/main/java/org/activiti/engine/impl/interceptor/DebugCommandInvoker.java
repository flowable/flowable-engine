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
package org.activiti.engine.impl.interceptor;

import org.activiti.engine.runtime.Debugger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements debugging functionality during command invocation
 *
 * @author martin.grofcik
 */
public class DebugCommandInvoker extends CommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(DebugCommandInvoker.class);

    private final Debugger debugger;

    public DebugCommandInvoker(Debugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public void executeOperations(final CommandContext commandContext) {
        while (!commandContext.getAgenda().isEmpty()) {
            Runnable runnable = commandContext.getAgenda().peekOperation();
            if (!this.debugger.isBreakPoint(runnable)) {
                executeOperation(commandContext.getAgenda().getNextOperation());
            } else {
                logger.debug("Runnable {} was identified as a break point. Stopping operation execution.", runnable);
                this.debugger.breakOperationExecution(commandContext);
            }
        }
    }

}
