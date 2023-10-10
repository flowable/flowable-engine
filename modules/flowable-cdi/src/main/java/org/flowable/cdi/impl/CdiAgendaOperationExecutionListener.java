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
package org.flowable.cdi.impl;

import org.flowable.cdi.impl.context.ExecutionContextHolder;
import org.flowable.common.engine.impl.agenda.AgendaOperationExecutionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.agenda.AbstractOperation;

/**
 * An {@link AgendaOperationExecutionListener} for use within CDI.
 * <p>
 * The Flowable-CDI integration builds upon the availability of the current execution in a thread local 'execution context'.
 * As this has a (very minimal) impact on performance, this thread local is not set by the default {@link org.flowable.engine.impl.interceptor.CommandInvoker CommandInvoker} and thus this customized version is needed.
 *
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CdiAgendaOperationExecutionListener implements AgendaOperationExecutionListener {

    @Override
    public void beforeExecute(CommandContext commandContext, Runnable runnable) {
        if (runnable instanceof AbstractOperation operation && operation.getExecution() != null) {
            ExecutionContextHolder.setExecutionContext(operation.getExecution());
        }
    }

    @Override
    public void afterExecute(CommandContext commandContext, Runnable runnable) {
        if (runnable instanceof AbstractOperation operation && operation.getExecution() != null) {
            ExecutionContextHolder.removeExecutionContext();
        }
    }

    @Override
    public void afterExecuteException(CommandContext commandContext, Runnable runnable, Throwable error) {
        afterExecute(commandContext, runnable);
    }

}
