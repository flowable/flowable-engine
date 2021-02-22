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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormInfo;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class ProcessInstanceBuilderFormTest extends AbstractFlowableFormEngineConfiguratorTest {

    @Test
    @Deployment(resources = {
            "org/flowable/form/engine/test/deployment/oneTaskProcess.bpmn20.xml",
            "org/flowable/form/engine/test/deployment/simpleInt.form"
    })
    public void startProcessInstanceWithFormVariables() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        FormInfo formInfo = formRepositoryService.getFormModelByKey("simpleIntForm");
        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), formInfo, "simple")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(procId))
                .containsOnly(
                        entry("intVar", 42L),
                        entry("form_simpleIntForm_outcome", "simple")
                );
    }

    @Test
    public void startProcessInstanceWithInvalidFormVariables() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        assertThatThrownBy(() -> runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), null, "simple"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("formInfo is null");
    }
}
