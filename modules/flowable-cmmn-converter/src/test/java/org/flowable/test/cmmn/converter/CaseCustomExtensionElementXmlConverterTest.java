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

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class CaseCustomExtensionElementXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/case-custom-extension-elements.cmmn";

    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }

    public void validateModel(CmmnModel cmmnModel) {

        assertThat(cmmnModel).isNotNull();
        Case primaryCase = cmmnModel.getPrimaryCase();
        assertThat(primaryCase).isNotNull();
        assertThat(primaryCase.getExtensionElements()).containsOnlyKeys("customElement");

        List<ExtensionElement> customElements = primaryCase.getExtensionElements().get("customElement");
        assertThat(customElements).hasSize(1);

        ExtensionElement customElement = customElements.get(0);
        assertThat(customElement.getElementText()).isEqualTo("Element text");
        assertThat(customElement.getNamespacePrefix()).isEqualTo("flowable");
        assertThat(customElement.getNamespace()).isEqualTo("http://flowable.org/cmmn");
        assertThat(customElement.getAttributeValue(null, "attribute")).isEqualTo("Value");
    }

}
