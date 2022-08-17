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
package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnExtensionAttribute;
import org.junit.jupiter.api.Test;

public class CustomAttributesTest extends AbstractConverterTest {


    @Test
    public void convertXMLToModel() throws Exception {
        DmnDefinition definition = readXMLFile();
        validateModel(definition);
    }

    @Test
    public void convertModelToXML() throws Exception {
        DmnDefinition bpmnModel = readXMLFile();
        DmnDefinition parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "customAttributes.dmn";
    }

    private void validateModel(DmnDefinition model) {
        assertThat(model.getAttributes().get("customAttribute"))
                .extracting(DmnExtensionAttribute::getNamespacePrefix, DmnExtensionAttribute::getValue)
                .containsExactlyInAnyOrder(
                        tuple("flowable", "definition"),
                        tuple("custom", "definitionCustom")
                );
        List<Decision> decisions = model.getDecisions();
        assertThat(decisions.get(0).getAttributes().get("customAttribute"))
                .extracting(DmnExtensionAttribute::getNamespacePrefix, DmnExtensionAttribute::getValue)
                .containsExactlyInAnyOrder(
                        tuple("flowable", "decision"),
                        tuple("custom", "decisionCustom")
                );
    }

}
