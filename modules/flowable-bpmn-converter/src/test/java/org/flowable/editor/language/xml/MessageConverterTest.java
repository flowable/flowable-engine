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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Message;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class MessageConverterTest {

    @BpmnXmlConverterTest("message.bpmn")
    void validateModel(BpmnModel model) {
        Message message = model.getMessage("writeReport");
        assertThat(message).isNotNull();
        assertThat(message)
                .extracting(Message::getItemRef, Message::getName, Message::getId)
                .containsExactly("Examples:writeReportItem", "newWriteReport", "writeReport");

        Message message2 = model.getMessage("writeReport2");
        assertThat(message2).isNotNull();
        assertThat(message2)
                .extracting(Message::getItemRef, Message::getName, Message::getId)
                .containsExactly("http://foo.bar.com/Examples:writeReportItem2", "newWriteReport2", "writeReport2");
    }
}
