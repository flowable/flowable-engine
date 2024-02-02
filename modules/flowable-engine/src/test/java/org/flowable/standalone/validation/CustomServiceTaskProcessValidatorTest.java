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
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ValidatorSetNames;
import org.flowable.validation.validator.impl.ServiceTaskValidator;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
public class CustomServiceTaskProcessValidatorTest {

    @Test
    public void verifyValidationWithCustomServiceTask() throws Exception {
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        ProcessValidator processValidator = processValidatorFactory.createDefaultProcessValidator();

        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/validation/customServiceTaskProcess.bpmn20.xml");
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(xmlStream, StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
        assertThat(bpmnModel).isNotNull();

        // set custom type on service task
        ServiceTask javaService = (ServiceTask) bpmnModel.getFlowElement("javaService");
        javaService.setType("custom-service-task");

        List<ValidationError> allErrors = processValidator.validate(bpmnModel);
        assertThat(allErrors).hasSize(1);

        String setName = ValidatorSetNames.FLOWABLE_EXECUTABLE_PROCESS; // shortening it a bit

        // invalid type error should be present
        List<ValidationError> problems = findErrors(allErrors, setName, Problems.SERVICE_TASK_INVALID_TYPE, 1);
        assertCommonErrorFields(problems.get(0));
    }

    @Test
    public void verifyValidationWithCustomServiceTaskValidator() throws Exception {
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        processValidatorFactory.setCustomServiceTaskValidator(new CustomServiceTaskValidator());
        ProcessValidator processValidator = processValidatorFactory.createDefaultProcessValidator();

        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/validation/customServiceTaskProcess.bpmn20.xml");
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(xmlStream, StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
        assertThat(bpmnModel).isNotNull();

        // set custom type on service task
        ServiceTask javaService = (ServiceTask) bpmnModel.getFlowElement("javaService");
        javaService.setType("custom-service-task");

        List<ValidationError> allErrors = processValidator.validate(bpmnModel);
        assertThat(allErrors).hasSize(0);
    }

    protected void assertCommonErrorFields(ValidationError error) {
        assertThat(error.getValidatorSetName()).isNotNull();
        assertThat(error.getProblem()).isNotNull();
        assertThat(error.getDefaultDescription()).isNotNull();
        assertThat(error.getXmlLineNumber()).isPositive();
        assertThat(error.getXmlColumnNumber()).isPositive();
    }

    protected List<ValidationError> findErrors(List<ValidationError> errors, String validatorSetName, String problemName, int expectedNrOfProblems) {
        List<ValidationError> results = findErrors(errors, validatorSetName, problemName);
        assertThat(results).hasSize(expectedNrOfProblems);
        for (ValidationError result : results) {
            assertThat(result.getValidatorSetName()).isEqualTo(validatorSetName);
            assertThat(result.getProblem()).isEqualTo(problemName);
        }
        return results;
    }

    protected List<ValidationError> findErrors(List<ValidationError> errors, String validatorSetName, String problemName) {
        List<ValidationError> results = new ArrayList<>();
        for (ValidationError error : errors) {
            if (error.getValidatorSetName().equals(validatorSetName) && error.getProblem().equals(problemName)) {
                results.add(error);
            }
        }
        return results;
    }

    class CustomServiceTaskValidator extends ServiceTaskValidator {

        @Override
        protected void validateUnknownServiceTaskType(Process process, ServiceTask serviceTask, List<ValidationError> errors) {
            if (!"custom-service-task".equals(serviceTask.getType())) {
                addError(errors, Problems.SERVICE_TASK_INVALID_TYPE, process, serviceTask, "Invalid or unsupported service task type");
            }
        }
    }

}
