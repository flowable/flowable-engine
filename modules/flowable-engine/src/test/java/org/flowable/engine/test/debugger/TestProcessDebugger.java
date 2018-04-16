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
package org.flowable.engine.test.debugger;

import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessDebugger;

/**
 * This class evaluates each execution as a breakpoint
 */
public class TestProcessDebugger implements ProcessDebugger {

    @Override
    public boolean isBreakpoint(Execution execution) {
        return true;
    }

}
