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
package org.flowable.standalone.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultipleStartEventValidatorTest {

    protected ProcessValidator processValidator;

    @BeforeEach
    public void setupProcessValidator() {
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        this.processValidator = processValidatorFactory.createDefaultProcessValidator();
    }

    @Test
    public void verifyValidation() throws Exception {

        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/validation/multipleStartEvent.bpmn20.xml");
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(xmlStream, StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
        assertThat(bpmnModel).isNotNull();

        List<ValidationError> allErrors = processValidator.validate(bpmnModel);
        assertThat(allErrors).hasSize(0);
    }

    protected BpmnModel readBpmnModelFromXml(String resource) {
        InputStreamProvider xmlStream = () -> MultipleStartEventValidatorTest.class.getClassLoader().getResourceAsStream(resource);
        return new BpmnXMLConverter().convertToBpmnModel(xmlStream, true, true);
    }

}
