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
package org.flowable.cmmn.test.eventlistener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.converter.GenericEventListenerXmlConverter;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.engine.impl.parser.handler.AbstractPlanItemParseHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a custom {@link EventListener} can be wired end-to-end via the public
 * {@code customEventListenerTypeFactories} and {@code customCmmnParseHandlers} extension points without
 * modifying the engine source.
 */
public class CustomEventListenerTest extends CustomCmmnConfigurationFlowableTestCase {

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.addCustomEventListenerTypeFactory("myTest", xtr -> {
            MyTestEventListener listener = new MyTestEventListener();
            listener.setCustomKey(xtr.getAttributeValue("http://flowable.org/cmmn", "customKey"));
            return listener;
        });
        cmmnEngineConfiguration.setPostCmmnParseHandlers(Collections.singletonList(new MyTestEventListenerParseHandler()));
    }

    @AfterAll
    static void cleanupRegistry() {
        // addCustomEventListenerTypeFactory wrote into the JVM-static registry on
        // GenericEventListenerXmlConverter; closing the engine doesn't undo it.
        GenericEventListenerXmlConverter.removeCustomListenerTypeFactory("myTest");
    }

    @Test
    public void testCustomEventListener() {
        addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/eventlistener/CustomEventListenerTest.cmmn")
                .deploy());

        // Start the case — the custom listener should land in AVAILABLE state, mirroring how the
        // built-in GenericEventListener behaves.
        MyTestEventListenerBehavior.OBSERVED_KEYS.clear();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("customListenerCase").start();

        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("customListener")
                .singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(MyTestEventListenerBehavior.OBSERVED_KEYS).containsExactly("create-widget-42");

        // Trigger the listener — our custom behavior records the customKey and routes through OCCUR,
        // completing the listener and (since it's the only plan item) the case.
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());

        assertThat(MyTestEventListenerBehavior.OBSERVED_KEYS).containsExactly("create-widget-42", "trigger-widget-42", "execute-widget-42", "occur-widget-42");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    // ---- Custom EventListener model ----

    public static class MyTestEventListener extends EventListener {

        protected String customKey;

        public String getCustomKey() {
            return customKey;
        }

        public void setCustomKey(String customKey) {
            this.customKey = customKey;
        }
    }

    // ---- Custom parse handler registered via postCmmnParseHandlers ----

    public static class MyTestEventListenerParseHandler extends AbstractPlanItemParseHandler<MyTestEventListener> {

        @Override
        public Collection<Class<? extends BaseElement>> getHandledTypes() {
            return Collections.singletonList(MyTestEventListener.class);
        }

        @Override
        protected void executePlanItemParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, PlanItem planItem, MyTestEventListener listener) {
            // The parse handler knows the typed model, so it constructs the behavior directly — no factory
            // dispatch indirection needed.
            planItem.setBehavior(new MyTestEventListenerBehavior(listener.getCustomKey()));
        }
    }

    public static class MyTestEventListenerBehavior extends CoreCmmnTriggerableActivityBehavior implements PlanItemActivityBehavior {

        // Records the keys observed during trigger(). Test-only side channel so we can assert the engine
        // actually invoked our behavior end-to-end.
        public static final List<String> OBSERVED_KEYS = new CopyOnWriteArrayList<>();

        protected final String customKey;

        public MyTestEventListenerBehavior(String customKey) {
            this.customKey = customKey;
        }

        public String getCustomKey() {
            return customKey;
        }

        @Override
        public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
            OBSERVED_KEYS.add(transition + "-" + customKey);
        }

        @Override
        public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
            OBSERVED_KEYS.add("execute-" + customKey);
            CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
        }

        @Override
        public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
            OBSERVED_KEYS.add("trigger-" + customKey);
            execute(commandContext, planItemInstanceEntity);
        }
    }
}
