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
import static org.assertj.core.api.Assertions.tuple;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.engine.test.util.TestProcessUtil;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ValidatorSetNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author jbarrez
 */
public class DefaultProcessValidatorTest {

    protected ProcessValidator processValidator;

    @BeforeEach
    public void setupProcessValidator() {
        ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
        this.processValidator = processValidatorFactory.createDefaultProcessValidator();
    }

    @Test
    public void verifyValidation() throws Exception {

        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/validation/invalidProcess.bpmn20.xml");
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(xmlStream, StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
        assertThat(bpmnModel).isNotNull();

        List<ValidationError> allErrors = processValidator.validate(bpmnModel);
        assertThat(allErrors).hasSize(71);

        String setName = ValidatorSetNames.FLOWABLE_EXECUTABLE_PROCESS; // shortening it a bit

        // isExecutable should be true
        List<ValidationError> problems = findErrors(allErrors, setName, Problems.ALL_PROCESS_DEFINITIONS_NOT_EXECUTABLE, 1);
        assertThat(problems.get(0).getValidatorSetName()).isNotNull();
        assertThat(problems.get(0).getProblem()).isNotNull();
        assertThat(problems.get(0).getDefaultDescription()).isNotNull();

        // Event listeners
        problems = findErrors(allErrors, setName, Problems.EVENT_LISTENER_IMPLEMENTATION_MISSING, 1);
        assertProcessElementError(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.EVENT_LISTENER_INVALID_THROW_EVENT_TYPE, 1);
        assertProcessElementError(problems.get(0));

        // Execution listeners
        problems = findErrors(allErrors, setName, Problems.EXECUTION_LISTENER_IMPLEMENTATION_MISSING, 2);
        assertProcessElementError(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));

        // Association
        problems = findErrors(allErrors, setName, Problems.ASSOCIATION_INVALID_SOURCE_REFERENCE, 1);
        assertProcessElementError(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.ASSOCIATION_INVALID_TARGET_REFERENCE, 1);
        assertProcessElementError(problems.get(0));

        // Signals
        problems = findErrors(allErrors, setName, Problems.SIGNAL_MISSING_ID, 1);
        assertCommonErrorFields(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SIGNAL_MISSING_NAME, 2);
        assertCommonErrorFields(problems.get(0));
        assertCommonErrorFields(problems.get(1));
        problems = findErrors(allErrors, setName, Problems.SIGNAL_DUPLICATE_NAME, 2);
        assertCommonErrorFields(problems.get(0));
        assertCommonErrorFields(problems.get(1));
        problems = findErrors(allErrors, setName, Problems.SIGNAL_INVALID_SCOPE, 1);
        assertCommonErrorFields(problems.get(0));

        // Start event
        problems = findErrors(allErrors, setName, Problems.START_EVENT_MULTIPLE_FOUND, 2);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));
        problems = findErrors(allErrors, setName, Problems.START_EVENT_INVALID_EVENT_DEFINITION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Sequence flow
        problems = findErrors(allErrors, setName, Problems.SEQ_FLOW_INVALID_SRC, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SEQ_FLOW_INVALID_TARGET, 2);
        assertCommonProblemFieldForActivity(problems.get(0));

        // User task
        problems = findErrors(allErrors, setName, Problems.USER_TASK_LISTENER_IMPLEMENTATION_MISSING, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Service task
        problems = findErrors(allErrors, setName, Problems.SERVICE_TASK_RESULT_VAR_NAME_WITH_DELEGATE, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SERVICE_TASK_INVALID_TYPE, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SERVICE_TASK_MISSING_IMPLEMENTATION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SERVICE_TASK_WEBSERVICE_INVALID_OPERATION_REF, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SERVICE_TASK_USE_LOCAL_SCOPE_FOR_RESULT_VAR_WITHOUT_RESULT_VARIABLE_NAME, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Send task
        problems = findErrors(allErrors, setName, Problems.SEND_TASK_INVALID_IMPLEMENTATION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SEND_TASK_INVALID_TYPE, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SEND_TASK_WEBSERVICE_INVALID_OPERATION_REF, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Mail task
        problems = findErrors(allErrors, setName, Problems.MAIL_TASK_NO_RECIPIENT, 2);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));
        problems = findErrors(allErrors, setName, Problems.MAIL_TASK_NO_CONTENT, 4);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));

        // Http task
        problems = findErrors(allErrors, setName, Problems.HTTP_TASK_NO_REQUEST_METHOD, 2);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));
        problems = findErrors(allErrors, setName, Problems.HTTP_TASK_NO_REQUEST_URL, 2);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));

        // Shell task
        problems = findErrors(allErrors, setName, Problems.SHELL_TASK_NO_COMMAND, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Script task
        problems = findErrors(allErrors, setName, Problems.SCRIPT_TASK_MISSING_SCRIPT, 2);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));

        // Exclusive gateway
        problems = findErrors(allErrors, setName, Problems.EXCLUSIVE_GATEWAY_CONDITION_NOT_ALLOWED_ON_SINGLE_SEQ_FLOW, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.EXCLUSIVE_GATEWAY_CONDITION_ON_DEFAULT_SEQ_FLOW, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.EXCLUSIVE_GATEWAY_NO_OUTGOING_SEQ_FLOW, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.EXCLUSIVE_GATEWAY_SEQ_FLOW_WITHOUT_CONDITIONS, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Event gateway
        problems = findErrors(allErrors, setName, Problems.EVENT_GATEWAY_ONLY_CONNECTED_TO_INTERMEDIATE_EVENTS, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Subprocesses
        problems = findErrors(allErrors, setName, Problems.SUBPROCESS_MULTIPLE_START_EVENTS, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SUBPROCESS_START_EVENT_EVENT_DEFINITION_NOT_ALLOWED, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Event subprocesses
        problems = findErrors(allErrors, setName, Problems.EVENT_SUBPROCESS_INVALID_START_EVENT_DEFINITION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.EVENT_SUBPROCESS_BOUNDARY_EVENT, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Boundary events
        problems = findErrors(allErrors, setName, Problems.BOUNDARY_EVENT_NO_EVENT_DEFINITION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.BOUNDARY_EVENT_CANCEL_ONLY_ON_TRANSACTION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.BOUNDARY_EVENT_MULTIPLE_CANCEL_ON_TRANSACTION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Intermediate catch event
        problems = findErrors(allErrors, setName, Problems.INTERMEDIATE_CATCH_EVENT_NO_EVENTDEFINITION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.INTERMEDIATE_CATCH_EVENT_INVALID_EVENTDEFINITION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Intermediate throw event
        problems = findErrors(allErrors, setName, Problems.THROW_EVENT_INVALID_EVENTDEFINITION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Multi instance
        problems = findErrors(allErrors, setName, Problems.MULTI_INSTANCE_MISSING_COLLECTION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Message events
        problems = findErrors(allErrors, setName, Problems.MESSAGE_EVENT_MISSING_MESSAGE_REF, 2);
        assertCommonProblemFieldForActivity(problems.get(0));
        assertCommonProblemFieldForActivity(problems.get(1));

        // Signal events
        problems = findErrors(allErrors, setName, Problems.SIGNAL_EVENT_MISSING_SIGNAL_REF, 1);
        assertCommonProblemFieldForActivity(problems.get(0));
        problems = findErrors(allErrors, setName, Problems.SIGNAL_EVENT_INVALID_SIGNAL_REF, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Compensate event
        problems = findErrors(allErrors, setName, Problems.COMPENSATE_EVENT_INVALID_ACTIVITY_REF, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Timer event
        problems = findErrors(allErrors, setName, Problems.EVENT_TIMER_MISSING_CONFIGURATION, 2);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Data association
        problems = findErrors(allErrors, setName, Problems.DATA_ASSOCIATION_MISSING_TARGETREF, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Data object
        problems = findErrors(allErrors, setName, Problems.DATA_OBJECT_MISSING_NAME, 2);
        assertCommonErrorFields(problems.get(0));
        assertCommonErrorFields(problems.get(1));

        // End event
        problems = findErrors(allErrors, setName, Problems.END_EVENT_CANCEL_ONLY_INSIDE_TRANSACTION, 1);
        assertCommonProblemFieldForActivity(problems.get(0));

        // Messages
        problems = findErrors(allErrors, setName, Problems.MESSAGE_INVALID_ITEM_REF, 1);
        assertCommonErrorFields(problems.get(0));

    }

    @Test
    public void testWarningError() throws UnsupportedEncodingException, XMLStreamException {
        String flowWithoutConditionNoDefaultFlow = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<definitions id='definitions' xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:activiti='http://activiti.org/bpmn' targetNamespace='Examples'>"
                + "  <process id='exclusiveGwDefaultSequenceFlow'> " + "    <startEvent id='theStart' /> "
                + "    <sequenceFlow id='flow1' sourceRef='theStart' targetRef='exclusiveGw' /> "
                + "    <exclusiveGateway id='exclusiveGw' name='Exclusive Gateway' /> "
                // no default = "flow3" !!
                + "    <sequenceFlow id='flow2' sourceRef='exclusiveGw' targetRef='theTask1'> "
                + "      <conditionExpression xsi:type='tFormalExpression'>${input == 1}</conditionExpression> "
                + "    </sequenceFlow> " + "    <sequenceFlow id='flow3' sourceRef='exclusiveGw' targetRef='theTask2'/> "
                // one would be OK
                + "    <sequenceFlow id='flow4' sourceRef='exclusiveGw' targetRef='theTask2'/> "
                // but two unconditional not!
                + "    <userTask id='theTask1' name='Input is one' /> " + "    <userTask id='theTask2' name='Default input' /> " + "  </process>"
                + "</definitions>";

        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(flowWithoutConditionNoDefaultFlow.getBytes()), StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
        assertThat(bpmnModel).isNotNull();
        List<ValidationError> allErrors = processValidator.validate(bpmnModel);
        assertThat(allErrors).hasSize(1);
        assertThat(allErrors.get(0).isWarning()).isTrue();
    }

    /*
     * Test for https://jira.codehaus.org/browse/ACT-2071:
     *
     * If all processes in a deployment are not executable, throw an exception as this doesn't make sense to do.
     */
    @Test
    public void testAllNonExecutableProcesses() {
        BpmnModel bpmnModel = new BpmnModel();
        for (int i = 0; i < 5; i++) {
            org.flowable.bpmn.model.Process process = TestProcessUtil.createOneTaskProcess();
            process.setExecutable(false);
            bpmnModel.addProcess(process);
        }

        List<ValidationError> errors = processValidator.validate(bpmnModel);
        assertThat(errors).hasSize(1);
    }

    /*
     * Test for https://jira.codehaus.org/browse/ACT-2071:
     *
     * If there is at least one process definition which is executable, and the deployment contains other process definitions which are not executable, then add a warning for those non executable
     * process definitions
     */
    @Test
    public void testNonExecutableProcessDefinitionWarning() {
        BpmnModel bpmnModel = new BpmnModel();

        // 3 non-executables
        for (int i = 0; i < 3; i++) {
            org.flowable.bpmn.model.Process process = TestProcessUtil.createOneTaskProcess();
            process.setExecutable(false);
            bpmnModel.addProcess(process);
        }

        // 1 executables
        org.flowable.bpmn.model.Process process = TestProcessUtil.createOneTaskProcess();
        process.setExecutable(true);
        bpmnModel.addProcess(process);

        List<ValidationError> errors = processValidator.validate(bpmnModel);
        assertThat(errors).hasSize(3);
        for (ValidationError error : errors) {
            assertThat(error.isWarning()).isTrue();
            assertThat(error.getValidatorSetName()).isNotNull();
            assertThat(error.getProblem()).isNotNull();
            assertThat(error.getDefaultDescription()).isNotNull();
        }
    }

    @Test
    void testEventSubProcessWithBoundary() {
        BpmnModel bpmnModel = readBpmnModelFromXml("org/flowable/standalone/validation/eventSubProcessWithBoundary.bpmn20.xml");

        List<ValidationError> errors = processValidator.validate(bpmnModel);

        assertThat(errors)
                .extracting(ValidationError::getProblem, ValidationError::getDefaultDescription, ValidationError::getActivityId, ValidationError::isWarning)
                .containsExactlyInAnyOrder(
                        tuple(Problems.EVENT_SUBPROCESS_BOUNDARY_EVENT, "event sub process cannot have attached boundary events", "errorEndEventSubProcess",
                                true)
                );
    }

    @Test
    void testEventSubProcessWithCancelStart() {
        BpmnModel bpmnModel = readBpmnModelFromXml("org/flowable/standalone/validation/eventSubProcessWithCancelStartEvent.bpmn20.xml");

        List<ValidationError> errors = processValidator.validate(bpmnModel);
        assertThat(errors).hasSize(1);
        ValidationError error = errors.get(0);
        assertThat(error.getProblem()).isEqualTo(Problems.EVENT_SUBPROCESS_INVALID_START_EVENT_DEFINITION);
        assertThat(error.getDefaultDescription()).isEqualTo("start event of event subprocess must be of type 'error', 'timer', 'message' or 'signal'");
        assertThat(error.getActivityId()).isEqualTo("cancelStartEventSubProcess");
        assertThat(error.getActivityName()).isEqualTo("Cancel Event Sub Process");
        assertThat(error.isWarning()).isFalse();
        assertThat(error.getXmlLineNumber()).isEqualTo(12);
        assertThat(error.getXmlColumnNumber()).isEqualTo(41);
    }

    @Test
    void testInvalidMultiInstanceActivity() {
        BpmnModel bpmnModel = readBpmnModelFromXml("org/flowable/standalone/validation/invalidMultiInstanceActivity.bpmn20.xml");

        List<ValidationError> errors = processValidator.validate(bpmnModel);
        assertThat(errors).hasSize(1);
        ValidationError error = errors.get(0);
        assertThat(error.getProblem()).isEqualTo(Problems.MULTI_INSTANCE_MISSING_COLLECTION);
        assertThat(error.getDefaultDescription()).isEqualTo("Either loopCardinality or loopDataInputRef/flowable:collection must been set");
        assertThat(error.getActivityId()).isEqualTo("multiInstanceServiceTask");
        assertThat(error.getActivityName()).isEqualTo("Multi Instance Service Task");
        assertThat(error.isWarning()).isFalse();
        assertThat(error.getXmlLineNumber()).isEqualTo(9);
        assertThat(error.getXmlColumnNumber()).isEqualTo(47);
    }

    @Test
    void testIntermediateTimerThrowEvent() {
        BpmnModel bpmnModel = readBpmnModelFromXml("org/flowable/standalone/validation/intermediateTimerThrowEvent.bpmn20.xml");

        List<ValidationError> errors = processValidator.validate(bpmnModel);
        assertThat(errors).hasSize(1);
        ValidationError error = errors.get(0);
        assertThat(error.getProblem()).isEqualTo(Problems.THROW_EVENT_INVALID_EVENTDEFINITION);
        assertThat(error.getDefaultDescription()).isEqualTo("Unsupported intermediate throw event type");
        assertThat(error.getActivityId()).isEqualTo("timerThrow");
        assertThat(error.getActivityName()).isEqualTo("Timer Throw");
        assertThat(error.isWarning()).isFalse();
        assertThat(error.getXmlLineNumber()).isEqualTo(8);
        assertThat(error.getXmlColumnNumber()).isEqualTo(35);
    }

    @Test
    void testExternalWorkerTaskWithoutTopic() {
        BpmnModel bpmnModel = readBpmnModelFromXml("org/flowable/standalone/validation/processWithInvalidExternalWorkerTask.bpmn20.xml");

        List<ValidationError> errors = processValidator.validate(bpmnModel);

        assertThat(errors)
                .extracting(ValidationError::getProblem, ValidationError::getDefaultDescription, ValidationError::getActivityId, ValidationError::isWarning)
                .containsExactlyInAnyOrder(
                        tuple(Problems.EXTERNAL_WORKER_TASK_NO_TOPIC, "No topic is defined on the external worker task", "externalWorkerServiceTask", false)
                );
    }

    @Test
    void testMailTask() {
        BpmnModel bpmnModel = readBpmnModelFromXml("org/flowable/standalone/validation/processWithMailTask.bpmn20.xml");

        List<ValidationError> errors = processValidator.validate(bpmnModel);

        assertThat(errors)
                .extracting(ValidationError::getProblem, ValidationError::getDefaultDescription, ValidationError::getActivityId, ValidationError::isWarning)
                .containsExactlyInAnyOrder(
                        tuple(Problems.MAIL_TASK_NO_RECIPIENT, "No recipient is defined on the mail activity", "sendMailWithoutanything", false)
                );
    }

    protected void assertCommonProblemFieldForActivity(ValidationError error) {
        assertProcessElementError(error);

        assertThat(error.getActivityId()).isNotNull();
        assertThat(error.getActivityName()).isNotNull();

        assertThat(error.getActivityId()).isNotEmpty();
        assertThat(error.getActivityName()).isNotEmpty();
    }

    protected void assertCommonErrorFields(ValidationError error) {
        assertThat(error.getValidatorSetName()).isNotNull();
        assertThat(error.getProblem()).isNotNull();
        assertThat(error.getDefaultDescription()).isNotNull();
        assertThat(error.getXmlLineNumber()).isPositive();
        assertThat(error.getXmlColumnNumber()).isPositive();
    }

    protected void assertProcessElementError(ValidationError error) {
        assertCommonErrorFields(error);
        assertThat(error.getProcessDefinitionId()).isEqualTo("invalidProcess");
        assertThat(error.getProcessDefinitionName()).isEqualTo("The invalid process");
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

    protected BpmnModel readBpmnModelFromXml(String resource) {
        InputStreamProvider xmlStream = () -> DefaultProcessValidatorTest.class.getClassLoader().getResourceAsStream(resource);
        return new BpmnXMLConverter().convertToBpmnModel(xmlStream, true, true);
    }

}
