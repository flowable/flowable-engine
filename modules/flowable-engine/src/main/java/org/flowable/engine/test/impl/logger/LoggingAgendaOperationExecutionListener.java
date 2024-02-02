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
package org.flowable.engine.test.impl.logger;

import org.flowable.common.engine.impl.agenda.AgendaOperationExecutionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.agenda.AbstractOperation;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class LoggingAgendaOperationExecutionListener implements AgendaOperationExecutionListener {

    protected final ProcessExecutionLogger processExecutionLogger;

    public LoggingAgendaOperationExecutionListener(ProcessExecutionLogger processExecutionLogger) {
        this.processExecutionLogger = processExecutionLogger;
    }

    @Override
    public void beforeExecute(CommandContext commandContext, Runnable runnable) {
        DebugInfoOperationExecuted debugInfo = null;
        if (runnable instanceof AbstractOperation operation) {

            debugInfo = new DebugInfoOperationExecuted(operation);
            debugInfo.setPreExecutionTime(System.currentTimeMillis());

            processExecutionLogger.addDebugInfo(debugInfo, true);
            commandContext.addAttribute(getDebugInfoKey(runnable), debugInfo);
        }
    }

    @Override
    public void afterExecute(CommandContext commandContext, Runnable runnable) {
        if (runnable instanceof AbstractOperation) {
            String debugInfoKey = getDebugInfoKey(runnable);
            Object attribute = commandContext.getAttribute(debugInfoKey);
            commandContext.removeAttribute(debugInfoKey);
            if (attribute instanceof DebugInfoOperationExecuted debugInfo) {
                debugInfo.setPostExecutionTime(System.currentTimeMillis());
            }
        }
    }

    @Override
    public void afterExecuteException(CommandContext commandContext, Runnable runnable, Throwable error) {
        afterExecute(commandContext, runnable);
    }

    protected String getDebugInfoKey(Runnable runnable) {
        return "debug-info-" + runnable.getClass().getName() + "@" + Integer.toHexString(runnable.hashCode());
    }

}
