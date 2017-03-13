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

/**
 * @author martin.grofcik
 */
public interface ProcessDebugger {

    /**
     * Indicates that execution is in the breakpoint state
     *
     * @param execution execution to evaluate
     *
     * @return true in the case when breakpoint was reached in the execution, false in the case when not
     * @throws RuntimeException in the case when it was not possible to decide
     */
    boolean isBreakpoint(Execution execution);

}
