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
package org.flowable.bpm.model.bpmn.instance;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

public class TextAnnotationTest
        extends BpmnModelElementInstanceTest {

    protected static BpmnModelInstance modelInstance;

    @Override
    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(Artifact.class, false);
    }

    @Override
    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.singletonList(
                new ChildElementAssumption(Text.class, 0, 1));
    }

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Collections.singletonList(
                new AttributeAssumption("textFormat", false, false, "text/plain"));
    }

    @BeforeClass
    public static void parseModel() {
        modelInstance = BpmnModelBuilder.readModelFromStream(TextAnnotationTest.class
                .getResourceAsStream("TextAnnotationTest.bpmn"));
    }

    @Test
    public void getTextAnnotationsByType() {
        Collection<TextAnnotation> textAnnotations = modelInstance.getModelElementsByType(TextAnnotation.class);
        assertThat(textAnnotations)
                .isNotNull()
                .hasSize(2);
    }

    @Test
    public void getTextAnnotationById() {
        TextAnnotation textAnnotation = modelInstance.getModelElementById("textAnnotation2");
        assertThat(textAnnotation).isNotNull();
        assertThat(textAnnotation.getTextFormat()).isEqualTo("text/plain");
        Text text = textAnnotation.getText();
        assertThat(text.getTextContent()).isEqualTo("Attached text annotation");
    }

    @Test
    public void textAnnotationAsAssociationSource() {
        Association association = modelInstance.getModelElementById("Association_1");
        BaseElement source = association.getSource();
        assertThat(source).isInstanceOf(TextAnnotation.class);
        assertThat(source.getId()).isEqualTo("textAnnotation2");
    }

    @Test
    public void textAnnotationAsAssociationTarget() {
        Association association = modelInstance.getModelElementById("Association_2");
        BaseElement target = association.getTarget();
        assertThat(target).isInstanceOf(TextAnnotation.class);
        assertThat(target.getId()).isEqualTo("textAnnotation1");
    }

}
