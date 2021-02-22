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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableHttpRequestHandler;
import org.flowable.bpmn.model.FlowableHttpResponseHandler;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class HttpServiceTaskConverterTest {

    @BpmnXmlConverterTest("httpservicetaskmodel.bpmn")
    void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetask");
        assertThat(flowElement)
                .isInstanceOfSatisfying(HttpServiceTask.class, httpServiceTask -> {
                    assertThat(httpServiceTask.getId()).isEqualTo("servicetask");
                    assertThat(httpServiceTask.getName()).isEqualTo("Service task");
                    assertThat(httpServiceTask.getFieldExtensions())
                            .extracting(FieldExtension::getFieldName, FieldExtension::getStringValue, FieldExtension::getExpression)
                            .containsExactly(
                                    tuple("url", "test", null),
                                    tuple("method", null, "GET")
                            );
                    assertThat(httpServiceTask.getHttpRequestHandler())
                            .extracting(FlowableHttpRequestHandler::getImplementationType, FlowableHttpRequestHandler::getImplementation)
                            .containsExactly(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${delegateExpression}");
                    assertThat(httpServiceTask.getHttpResponseHandler())
                            .extracting(FlowableHttpResponseHandler::getImplementationType, FlowableHttpResponseHandler::getImplementation)
                            .containsExactly(ImplementationType.IMPLEMENTATION_TYPE_CLASS, "org.flowable.Test");
                });
    }
}
