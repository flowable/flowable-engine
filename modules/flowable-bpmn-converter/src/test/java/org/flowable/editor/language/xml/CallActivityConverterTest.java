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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class CallActivityConverterTest {

    @BpmnXmlConverterTest("callactivity.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("callactivity");
        assertThat(flowElement).isInstanceOf(CallActivity.class);
        CallActivity callActivity = (CallActivity) flowElement;
        assertThat(callActivity.getId()).isEqualTo("callactivity");
        assertThat(callActivity.getName()).isEqualTo("Call activity");

        assertThat(callActivity.getCalledElement()).isEqualTo("processId");

        assertThat(callActivity.getFallbackToDefaultTenant()).isTrue();

        List<IOParameter> parameters = callActivity.getInParameters();
        assertThat(parameters)
                .extracting(IOParameter::getSource, IOParameter::getTarget, IOParameter::getSourceExpression)
                .containsExactly(
                        tuple("test", "test", null),
                        tuple(null, "test", "${test}")
                );

        parameters = callActivity.getOutParameters();
        assertThat(parameters)
                .extracting(IOParameter::getSource, IOParameter::getTarget)
                .containsExactly(
                        tuple("test", "test")
                );
        
        List<MapExceptionEntry> mapExceptions = callActivity.getMapExceptions();
        assertThat(mapExceptions).hasSize(1);
        MapExceptionEntry mapExectionEntry = mapExceptions.get(0);
        assertThat(mapExectionEntry.getErrorCode()).isEqualTo("myErrorCode");
        assertThat(mapExectionEntry.getClassName()).isEqualTo("org.flowable.Something");
        assertThat(mapExectionEntry.getRootCause()).isEqualTo("org.flowable.Exception");
    }
}
