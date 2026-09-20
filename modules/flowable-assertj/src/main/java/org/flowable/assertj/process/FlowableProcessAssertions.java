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

package org.flowable.assertj.process;

import org.assertj.core.api.Assertions;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author martin.grofcik
 */
public class FlowableProcessAssertions extends Assertions {

    public static ProcessInstanceAssert assertThat(ProcessInstance processInstance) {
        return new ProcessInstanceAssert(processInstance);
    }
    public static HistoricProcessInstanceAssert assertThat(HistoricProcessInstance historicProcessInstance) {
        return new HistoricProcessInstanceAssert(historicProcessInstance);
    }

}
