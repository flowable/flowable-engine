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
package org.flowable.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.instance.ConditionExpression;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.junit.Before;
import org.junit.Test;

public class ConditionalSequenceFlowTest {

    protected BpmnModelInstance modelInstance;
    protected SequenceFlow flow1;
    protected SequenceFlow flow2;
    protected ConditionExpression conditionExpression1;
    protected ConditionExpression conditionExpression2;

    @Before
    public void parseModel() {
        modelInstance = BpmnModelBuilder.readModelFromStream(getClass().getResourceAsStream(getClass().getSimpleName() + ".xml"));
        flow1 = modelInstance.getModelElementById("flow1");
        flow2 = modelInstance.getModelElementById("flow2");
        conditionExpression1 = flow1.getConditionExpression();
        conditionExpression2 = flow2.getConditionExpression();
    }

    @Test
    public void shouldHaveTypeTFormalExpression() {
        assertThat(conditionExpression1.getType()).isEqualTo("tFormalExpression");
        assertThat(conditionExpression2.getType()).isEqualTo("tFormalExpression");
    }

    @Test
    public void shouldHaveSourceCode() {
        assertThat(conditionExpression1.getTextContent()).isEqualTo("test");
        assertThat(conditionExpression2.getTextContent()).isEqualTo("${test}");
    }
}
