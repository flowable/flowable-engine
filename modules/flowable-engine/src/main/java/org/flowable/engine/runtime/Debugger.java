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
package org.flowable.engine.runtime;

import org.flowable.engine.Agenda;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.interceptor.CommandExecutor;

/**
 * This interface provides methods for the debugging
 *
 * @author martin.grofcik
 */
public interface Debugger {
    /**
     * evaluates whether given invocation is defined as a break point or not
     *
     * @param runnable
     *            the runnable to execute
     * @return true in the case when the execution should be stopped, false otherwise
     */
    boolean isBreakPoint(Runnable runnable);

    /**
     * Continue in the broken operation execution
     *
     * @param commandExecutor
     *            executor to run command with
     * @param agenda
     *            agenda to execute
     */
    void continueOperationExecution(CommandExecutor commandExecutor, Agenda agenda);

    /**
     * Break current operation execution
     *
     * @param commandContext
     *            command context for which execution is broken
     */
    void breakOperationExecution(CommandContext commandContext);
}
