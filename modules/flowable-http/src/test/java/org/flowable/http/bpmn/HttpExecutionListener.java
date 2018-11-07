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
package org.flowable.http.bpmn;

import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpExecutionListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpExecutionListener.class);

    public static int runs;

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable("runs", ++runs);
        for (Map.Entry e : execution.getVariables().entrySet()) {
            LOGGER.info("key: {}", e.getKey());
            if (e.getValue() != null) {
                LOGGER.info("Value: {}", e.getValue());
            }
        }
    }
}
