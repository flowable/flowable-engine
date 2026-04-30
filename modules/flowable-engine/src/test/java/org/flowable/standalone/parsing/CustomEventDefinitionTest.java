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
package org.flowable.standalone.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.bpmn.converter.CustomEventDefinitionXmlWriter;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CustomBpmnEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessStartEventInitializerContext;
import org.flowable.engine.impl.bpmn.behavior.FlowNodeActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventDeployContext;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventUndeployContext;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a custom {@link EventDefinition} can be wired end-to-end via the public
 * {@code customChildElementParsers}, {@code customEventDefinitionWriters}, and
 * {@code customDefaultBpmnParseHandlers} extension points without modifying the engine source.
 */
public class CustomEventDefinitionTest extends ResourceFlowableTestCase {

    public CustomEventDefinitionTest() {
        super("flowable.cfg.xml", "customEventDefinitionTest");
    }

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        // customDefaultBpmnParseHandlers REPLACES handlers for matching handled types — no built-in handler
        // matches MyTestEventDefinition, so use postBpmnParseHandlers (or preBpmnParseHandlers) to ADD a new
        // handler instead. The validators learn the type's allowed contexts from
        // MyTestEventDefinition.getSupportedLocations() — no engine-config registration needed.
        ProcessEngineConfigurationImpl cfg = (ProcessEngineConfigurationImpl) processEngineConfiguration;
        cfg.setCustomChildElementParsers(Collections.singletonList(new MyTestEventDefinitionParser()));
        cfg.addCustomEventDefinitionWriter(MyTestEventDefinition.class, new MyTestEventDefinitionWriter());
        cfg.setPostBpmnParseHandlers(Collections.singletonList(new MyTestEventDefinitionParseHandler()));
    }

    @AfterAll
    static void cleanupRegistries() {
        // Both setCustomChildElementParsers and addCustomEventDefinitionWriter write into JVM-static
        // BpmnXMLUtil registries at engine init; closing the engine doesn't undo them.
        BpmnXMLUtil.removeChildElementParser("myTestEventDefinition");
        BpmnXMLUtil.removeCustomEventDefinitionWriter(MyTestEventDefinition.class);
    }

    @Test
    @Deployment(resources = "org/flowable/standalone/parsing/CustomEventDefinitionTest.bpmn20.xml")
    public void testCustomEventDefinitionOnIntermediateCatchEvent() {
        MyTestEventCatchBehavior.RECORDED_KEYS.clear();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("customEventDefinitionProcess");
        assertThat(MyTestEventCatchBehavior.RECORDED_KEYS).containsExactly("widget-42");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/standalone/parsing/CustomEventDefinitionTest.eventSubProcess.bpmn20.xml")
    public void testCustomEventDefinitionOnEventSubProcessStartEvent() {
        // Validates that a custom EventDefinition on an event-sub-process start event parses, is accepted by
        // the validator, the custom parse handler installs the typed behavior, and the engine creates a
        // waiting execution that can be triggered via runtimeService.trigger(...).
        MyTestEventSubProcessStartBehavior.RECORDED_KEYS.clear();

        // Start the process; the engine creates a waiting execution at the custom start event with
        // eventScope=true and active=false. behavior.execute() is not invoked at this point — that mirrors
        // the standard pattern shared with built-in subscription-based start events (Message, Signal, etc.):
        // the behavior is invoked only on trigger. initializeEventSubProcessStart() IS invoked and records
        // its key so we can assert the engine called the new EventSubProcessStartEventActivityBehavior hook.
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("customEventDefinitionEventSubProcess");
        assertThat(MyTestEventSubProcessStartBehavior.RECORDED_KEYS).containsExactly("initialize-esp-7");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName())
                .isEqualTo("Main task");

        // The event-sub-process start event execution is created with active=false (standard event-scope
        // pattern), so the activityId-filtered query won't match it. Iterate all executions of the instance
        // to locate it.
        Execution startExecution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .list().stream()
                .filter(e -> "customEventSubProcessStart".equals(e.getActivityId()))
                .findFirst().orElse(null);
        assertThat(startExecution).isNotNull();

        // Trigger the start event — the behavior records and leaves to the event sub-process body, which
        // completes (no further activity) while the main user task remains.
        runtimeService.trigger(startExecution.getId());
        assertThat(MyTestEventSubProcessStartBehavior.RECORDED_KEYS).containsExactly("initialize-esp-7", "trigger-esp-7");

        // Complete the main user task; the process completes.
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    public void testCustomEventDefinitionOnProcessStartEvent() {
        // Validates that a custom EventDefinition on a process-level start event whose ActivityBehavior
        // implements ProcessLevelStartEventActivityBehavior receives deploy() at deploy time and
        // undeploy() when superseded by a new version.
        MyTestProcessStartBehavior.RECORDED_KEYS.clear();

        // First deployment.
        String firstDeploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/parsing/CustomEventDefinitionTest.processStart.bpmn20.xml")
                .deploy().getId();
        deploymentIdsForAutoCleanup.add(firstDeploymentId);

        // After v1 deploy: only deploy@1 is recorded (no previous process definition to undeploy).
        assertThat(MyTestProcessStartBehavior.RECORDED_KEYS).containsExactly("deploy-proc-start-1@1");

        // Redeploy the same resource — this creates v2; v1 is the previous process definition that should be undeployed.
        String secondDeploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/parsing/CustomEventDefinitionTest.processStart.bpmn20.xml")
                .deploy().getId();
        deploymentIdsForAutoCleanup.add(secondDeploymentId);

        // After v2 deploy: undeploy is called for v1 BEFORE deploy is called for v2.
        assertThat(MyTestProcessStartBehavior.RECORDED_KEYS)
                .containsExactly("deploy-proc-start-1@1", "undeploy-proc-start-1@1", "deploy-proc-start-1@2");

    }

    @Test
    public void testCustomEventDefinitionOnProcessStartEventBulkFlushDeletesObsoleteSubscriptions() {
        MyTestProcessStartBehavior.RECORDED_KEYS.clear();

        String firstDeploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/parsing/CustomEventDefinitionTest.processStart.bpmn20.xml")
                .deploy().getId();
        deploymentIdsForAutoCleanup.add(firstDeploymentId);
        String v1ProcessDefinitionId = repositoryService.createProcessDefinitionQuery()
                .deploymentId(firstDeploymentId).singleResult().getId();

        assertThat(findSubscriptions(v1ProcessDefinitionId, MyTestProcessStartBehavior.OBSOLETE_TYPE)).hasSize(1);

        String secondDeploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/parsing/CustomEventDefinitionTest.processStart.bpmn20.xml")
                .deploy().getId();
        deploymentIdsForAutoCleanup.add(secondDeploymentId);
        String v2ProcessDefinitionId = repositoryService.createProcessDefinitionQuery()
                .deploymentId(secondDeploymentId).singleResult().getId();

        assertThat(findSubscriptions(v1ProcessDefinitionId, MyTestProcessStartBehavior.OBSOLETE_TYPE)).isEmpty();
        assertThat(findSubscriptions(v2ProcessDefinitionId, MyTestProcessStartBehavior.OBSOLETE_TYPE)).hasSize(1);
    }

    private List<EventSubscriptionEntity> findSubscriptions(String processDefinitionId, String type) {
        return managementService.executeCommand(commandContext ->
                ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())
                        .getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                        .findEventSubscriptionsByTypesAndProcessDefinitionId(Collections.singleton(type), processDefinitionId, null));
    }

    @Test
    public void testCustomEventDefinitionOnProcessStartEventDeletionRestore() {
        MyTestProcessStartBehavior.RECORDED_KEYS.clear();

        String firstDeploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/parsing/CustomEventDefinitionTest.processStart.bpmn20.xml")
                .deploy().getId();
        deploymentIdsForAutoCleanup.add(firstDeploymentId);

        String secondDeploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/parsing/CustomEventDefinitionTest.processStart.bpmn20.xml")
                .deploy().getId();

        repositoryService.deleteDeployment(secondDeploymentId, true);

        assertThat(MyTestProcessStartBehavior.RECORDED_KEYS)
                .containsExactly(
                        "deploy-proc-start-1@1",
                        "undeploy-proc-start-1@1",
                        "deploy-proc-start-1@2",
                        "deploy-proc-start-1@1[restoring]");
    }

    @Test
    @Deployment(resources = "org/flowable/standalone/parsing/CustomEventDefinitionTest.boundary.bpmn20.xml")
    public void testCustomEventDefinitionOnBoundaryEvent() {
        MyTestBoundaryBehavior.RECORDED_KEYS.clear();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("customEventDefinitionBoundaryProcess");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName())
                .isEqualTo("Wait for boundary");

        assertThat(MyTestBoundaryBehavior.RECORDED_KEYS).containsExactly("execute-boundary-99");

        Execution boundaryExecution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("customBoundary")
                .singleResult();
        assertThat(boundaryExecution).isNotNull();
        runtimeService.trigger(boundaryExecution.getId());

        assertThat(MyTestBoundaryBehavior.RECORDED_KEYS).containsExactly("execute-boundary-99", "trigger-boundary-99");

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    // ---- Custom EventDefinition model ----

    public static class MyTestEventDefinition extends EventDefinition implements CustomBpmnEventDefinition {

        private static final Set<EventDefinitionLocation> SUPPORTED_LOCATIONS = EnumSet.of(
                EventDefinitionLocation.START_EVENT,
                EventDefinitionLocation.INTERMEDIATE_CATCH_EVENT,
                EventDefinitionLocation.BOUNDARY_EVENT,
                EventDefinitionLocation.EVENT_SUBPROCESS_START_EVENT);

        protected String customKey;

        @Override
        public Set<EventDefinitionLocation> getSupportedLocations() {
            return SUPPORTED_LOCATIONS;
        }

        public String getCustomKey() {
            return customKey;
        }

        public void setCustomKey(String customKey) {
            this.customKey = customKey;
        }

        @Override
        public MyTestEventDefinition clone() {
            MyTestEventDefinition clone = new MyTestEventDefinition();
            clone.setValues(this);
            clone.setCustomKey(customKey);
            return clone;
        }
    }

    // ---- Custom XML parser registered via customChildElementParsers ----

    public static class MyTestEventDefinitionParser extends BaseChildElementParser {

        @Override
        public String getElementName() {
            return "myTestEventDefinition";
        }

        @Override
        public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) {
            if (!(parentElement instanceof Event event)) {
                return;
            }
            MyTestEventDefinition definition = new MyTestEventDefinition();
            definition.setCustomKey(xtr.getAttributeValue(null, "customKey"));
            event.addEventDefinition(definition);
        }
    }

    // ---- Custom XML writer registered via customEventDefinitionWriters; the write-side counterpart ----

    public static class MyTestEventDefinitionWriter implements CustomEventDefinitionXmlWriter {

        @Override
        public void write(Event parentEvent, EventDefinition eventDefinition, XMLStreamWriter xtw) throws Exception {
            MyTestEventDefinition myTest = (MyTestEventDefinition) eventDefinition;
            xtw.writeStartElement("flowable", "myTestEventDefinition", "http://flowable.org/bpmn");
            if (myTest.getCustomKey() != null) {
                xtw.writeAttribute("customKey", myTest.getCustomKey());
            }
            xtw.writeEndElement();
        }
    }

    // ---- Custom parse handler registered via customDefaultBpmnParseHandlers ----

    public static class MyTestEventDefinitionParseHandler extends AbstractBpmnParseHandler<MyTestEventDefinition> {

        @Override
        public Class<? extends BaseElement> getHandledType() {
            return MyTestEventDefinition.class;
        }

        @Override
        protected void executeParse(BpmnParse bpmnParse, MyTestEventDefinition eventDefinition) {
            // The parse handler knows the typed model, so it constructs the behavior directly — no factory
            // dispatch indirection needed.
            if (bpmnParse.getCurrentFlowElement() instanceof IntermediateCatchEvent intermediateCatchEvent) {
                intermediateCatchEvent.setBehavior(new MyTestEventCatchBehavior(eventDefinition.getCustomKey()));
            } else if (bpmnParse.getCurrentFlowElement() instanceof BoundaryEvent boundaryEvent) {
                boundaryEvent.setBehavior(new MyTestBoundaryBehavior(eventDefinition.getCustomKey(), boundaryEvent.isCancelActivity()));
            } else if (bpmnParse.getCurrentFlowElement() instanceof StartEvent startEvent) {
                if (startEvent.getSubProcess() instanceof EventSubProcess) {
                    startEvent.setBehavior(new MyTestEventSubProcessStartBehavior(eventDefinition.getCustomKey()));
                } else {
                    startEvent.setBehavior(new MyTestProcessStartBehavior(eventDefinition.getCustomKey()));
                }
            }
        }
    }

    public static class MyTestEventCatchBehavior extends AbstractBpmnActivityBehavior {

        private static final long serialVersionUID = 1L;

        // Records the keys observed during execute(). Test-only side channel so we can assert the engine
        // actually invoked our behavior end-to-end.
        public static final List<String> RECORDED_KEYS = new CopyOnWriteArrayList<>();

        protected final String customKey;

        public MyTestEventCatchBehavior(String customKey) {
            this.customKey = customKey;
        }

        public String getCustomKey() {
            return customKey;
        }

        @Override
        public void execute(DelegateExecution execution) {
            RECORDED_KEYS.add(customKey);
            leave(execution);
        }
    }

    public static class MyTestEventSubProcessStartBehavior extends AbstractBpmnActivityBehavior implements EventSubProcessStartEventActivityBehavior {

        private static final long serialVersionUID = 1L;

        public static final List<String> RECORDED_KEYS = new CopyOnWriteArrayList<>();

        protected final String customKey;

        public MyTestEventSubProcessStartBehavior(String customKey) {
            this.customKey = customKey;
        }

        public String getCustomKey() {
            return customKey;
        }

        @Override
        public void initializeEventSubProcessStart(EventSubProcessStartEventInitializerContext context) {
            RECORDED_KEYS.add("initialize-" + customKey);
            // Create the standard waiting child execution so external code can later trigger us. A real
            // integration would also wire up a subscription / callback that resolves the trigger; we keep
            // it simple here and just make the execution triggerable via runtimeService.trigger(...).
            context.createEventScopeChildExecution();
        }

        @Override
        public void execute(DelegateExecution execution) {
            RECORDED_KEYS.add("execute-" + customKey);
        }

        @Override
        public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
            RECORDED_KEYS.add("trigger-" + customKey);
            // Test-only mock: full event-sub-process trigger semantics (interrupting/non-interrupting,
            // sibling cleanup, scope creation) are out of scope here. We only verify the engine actually
            // invokes our behavior.
        }
    }

    public static class MyTestBoundaryBehavior extends BoundaryEventActivityBehavior {

        private static final long serialVersionUID = 1L;
        // Records the keys observed during execute(). Test-only side channel so we can assert the engine
        // actually invoked our behavior end-to-end.
        public static final List<String> RECORDED_KEYS = new CopyOnWriteArrayList<>();

        protected final String customKey;

        public MyTestBoundaryBehavior(String customKey, boolean interrupting) {
            super(interrupting);
            this.customKey = customKey;
        }

        public String getCustomKey() {
            return customKey;
        }

        @Override
        public void execute(DelegateExecution execution) {
            RECORDED_KEYS.add("execute-" + customKey);
            super.execute(execution);
        }

        @Override
        public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
            RECORDED_KEYS.add("trigger-" + customKey);
            super.trigger(execution, triggerName, triggerData);
        }
    }

    public static class MyTestProcessStartBehavior extends FlowNodeActivityBehavior implements ProcessLevelStartEventActivityBehavior {

        private static final long serialVersionUID = 1L;

        public static final String OBSOLETE_TYPE = "myTestProcessStartObsoleteType";

        // Suffix [restoring] marks deploys where ProcessLevelStartEventDeployContext.isRestoringPreviousVersion() is true.
        public static final List<String> RECORDED_KEYS = new CopyOnWriteArrayList<>();

        protected final String customKey;

        public MyTestProcessStartBehavior(String customKey) {
            this.customKey = customKey;
        }

        public String getCustomKey() {
            return customKey;
        }

        @Override
        public void deploy(ProcessLevelStartEventDeployContext context) {
            String suffix = context.isRestoringPreviousVersion() ? "[restoring]" : "";
            RECORDED_KEYS.add("deploy-" + customKey + "@" + context.getProcessDefinition().getVersion() + suffix);
            context.getEventSubscriptionService().createEventSubscriptionBuilder()
                    .eventType(OBSOLETE_TYPE)
                    .activityId(context.getStartEvent().getId())
                    .processDefinitionId(context.getProcessDefinition().getId())
                    .scopeType(ScopeTypes.BPMN)
                    .create();
        }

        @Override
        public void undeploy(ProcessLevelStartEventUndeployContext context) {
            RECORDED_KEYS.add("undeploy-" + customKey + "@" + context.getPreviousProcessDefinition().getVersion());
            // Exercises the bulk-flush hook: the deployer iterates registered types and bulk-deletes
            // matching event subscriptions on the previous process definition.
            context.registerObsoleteEventSubscriptionType(OBSOLETE_TYPE);
        }
    }
}
