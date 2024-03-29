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
package org.flowable.engine.test.bpmn.event.error.mapError;

import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author Saeid Mirzaei
 */
public class FlagDelegate implements JavaDelegate {
    static boolean visited;
    static Map<String, Object> variables;

    public static void reset() {
        visited = false;
        variables = null;
    }

    public static boolean isVisited() {
        return visited;
    }

    public static Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public void execute(DelegateExecution execution) {
        visited = true;
        variables = execution.getVariables();
    }

}
