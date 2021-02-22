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

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class CaseCustomExtensionElementXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/case-custom-extension-elements.cmmn")
    public void validateModel(CmmnModel cmmnModel) {

        assertThat(cmmnModel).isNotNull();
        Case primaryCase = cmmnModel.getPrimaryCase();
        assertThat(primaryCase).isNotNull();
        assertThat(primaryCase.getExtensionElements()).containsOnlyKeys("customElement");

        List<ExtensionElement> customElements = primaryCase.getExtensionElements().get("customElement");
        assertThat(customElements)
                .extracting(ExtensionElement::getElementText,
                        ExtensionElement::getNamespacePrefix,
                        ExtensionElement::getNamespace,
                        extensionElement -> extensionElement.getAttributeValue(null, "attribute"))
                .containsExactly(tuple("Element text", "flowable", "http://flowable.org/cmmn", "Value"));
    }

}
