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
import org.flowable.engine.impl.agenda.AbstractOperation;
import org.flowable.engine.impl.interceptor.CommandInvoker;

/**
 * A customized version of the default {@link CommandInvoker} for use with CDI.
 * 
 * The Flowable-CDI integration builds upon the availability of the current execution in a thread local 'execution context'. As this has a (very minimal) impact on performance, this thread local is
 * not set by the default {@link CommandInvoker} and thus this customized version is needed.
 * 
 * @author jbarrez
 */
public class CdiCommandInvoker extends CommandInvoker {

    @Override
    public void executeOperation(Runnable runnable) {

        boolean executionContextSet = false;
        if (runnable instanceof AbstractOperation) {
            AbstractOperation operation = (AbstractOperation) runnable;
            if (operation.getExecution() != null) {
                ExecutionContextHolder.setExecutionContext(operation.getExecution());
                executionContextSet = true;
            }
        }

        super.executeOperation(runnable);

        if (executionContextSet) {
            ExecutionContextHolder.removeExecutionContext();
        }

    }

}
