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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Joram Barrez
 */
public class EventListenerWithEventTypeXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/event-listener-with-event-type.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition("eventListener");
        assertThat(planItemDefinition).isInstanceOf(GenericEventListener.class);

        GenericEventListener genericEventListener = (GenericEventListener) planItemDefinition;
        assertThat(genericEventListener.getEventType()).isEqualTo("myEvent");

        List<ExtensionElement> correlationParameters = genericEventListener.getExtensionElements().getOrDefault("eventCorrelationParameter", new ArrayList<>());
        assertThat(correlationParameters)
                .extracting(extensionElement -> extensionElement.getAttributeValue(null, "name"),
                        extensionElement -> extensionElement.getAttributeValue(null, "value"))
                .containsExactly(tuple("customerId", "${customerIdVar}"));
    }

}
