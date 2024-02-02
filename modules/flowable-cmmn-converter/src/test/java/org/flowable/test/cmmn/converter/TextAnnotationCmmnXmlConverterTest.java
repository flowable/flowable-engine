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

import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.TextAnnotation;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Joram Barrez
 */
public class TextAnnotationCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/text-annotation.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel.getTextAnnotations()).extracting(TextAnnotation::getText).containsOnly("Hello World");

        List<Association> associations = cmmnModel.getAssociations();
        Association association = associations.stream().filter(a -> a.getSourceElement() instanceof TextAnnotation).findFirst().get();
        assertThat(association.getSourceRef()).isEqualTo("testAnnotation");
        assertThat(association.getTargetRef()).isEqualTo("planItem1");

        assertThat(association.getSourceElement().getId()).isEqualTo("testAnnotation");
        assertThat(association.getTargetElement().getId()).isEqualTo("planItem1");

    }

}
