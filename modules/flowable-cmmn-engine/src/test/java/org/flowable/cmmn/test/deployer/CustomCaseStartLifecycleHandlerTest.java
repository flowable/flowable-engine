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
package org.flowable.cmmn.test.deployer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CaseDefinitionStartDeployContext;
import org.flowable.cmmn.engine.impl.deployer.CaseDefinitionStartLifecycleHandler;
import org.flowable.cmmn.engine.impl.deployer.CaseDefinitionStartUndeployContext;
import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.engine.impl.parser.handler.AbstractCmmnParseHandler;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a custom {@link CaseDefinitionStartLifecycleHandler} installed via the public
 * {@code postCmmnParseHandlers} extension point receives the deploy / undeploy / restore lifecycle
 * across deployment, redeployment, and deletion-of-latest. Mirrors the BPMN
 * {@code CustomEventDefinitionTest.testCustomEventDefinitionOnProcessStartEvent} scenario.
 */
public class CustomCaseStartLifecycleHandlerTest extends CustomCmmnConfigurationFlowableTestCase {

    private static final String RESOURCE = "org/flowable/cmmn/test/deployer/CustomCaseStartLifecycleHandlerTest.cmmn.xml";

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setPostCmmnParseHandlers(Collections.singletonList(new RecordingCaseStartLifecycleParseHandler()));
    }

    @Test
    public void testDeployRedeployDeleteLifecycle() {
        RecordingCaseStartLifecycleHandler.RECORDED.clear();

        // First deployment.
        CmmnDeployment v1 = cmmnRepositoryService.createDeployment().addClasspathResource(RESOURCE).deploy();
        assertThat(RecordingCaseStartLifecycleHandler.RECORDED).containsExactly("deploy@1");

        // Redeploy — undeploy(v1) before deploy(v2).
        CmmnDeployment v2 = cmmnRepositoryService.createDeployment().addClasspathResource(RESOURCE).deploy();
        assertThat(RecordingCaseStartLifecycleHandler.RECORDED).containsExactly("deploy@1", "undeploy@1", "deploy@2");

        // Delete the latest deployment — restore(v1) via deploy() with the restoringPreviousVersion flag.
        cmmnRepositoryService.deleteDeployment(v2.getId(), true);
        assertThat(RecordingCaseStartLifecycleHandler.RECORDED).containsExactly("deploy@1", "undeploy@1", "deploy@2", "restore@1");

        // Cleanup the v1 deployment we created manually.
        cmmnRepositoryService.deleteDeployment(v1.getId(), true);
    }

    @Test
    public void testBulkFlushDeletesObsoleteSubscriptionsOnRedeploy() {
        RecordingCaseStartLifecycleHandler.RECORDED.clear();

        CmmnDeployment v1 = cmmnRepositoryService.createDeployment().addClasspathResource(RESOURCE).deploy();
        String v1CaseDefinitionId = caseDefinitionId(v1.getId());
        assertThat(findSubscriptions(v1CaseDefinitionId)).hasSize(1);

        CmmnDeployment v2 = cmmnRepositoryService.createDeployment().addClasspathResource(RESOURCE).deploy();
        String v2CaseDefinitionId = caseDefinitionId(v2.getId());

        assertThat(findSubscriptions(v1CaseDefinitionId)).isEmpty();
        assertThat(findSubscriptions(v2CaseDefinitionId)).hasSize(1);

        cmmnRepositoryService.deleteDeployment(v2.getId(), true);
        cmmnRepositoryService.deleteDeployment(v1.getId(), true);
    }

    private String caseDefinitionId(String deploymentId) {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deploymentId).singleResult();
        return caseDefinition.getId();
    }

    private List<EventSubscriptionEntity> findSubscriptions(String caseDefinitionId) {
        return cmmnEngineConfiguration.getCommandExecutor().execute(commandContext ->
                cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                        .findEventSubscriptionsByTypesAndScopeDefinitionId(
                                Collections.singleton(RecordingCaseStartLifecycleHandler.OBSOLETE_TYPE),
                                caseDefinitionId, ScopeTypes.CMMN, null));
    }

    public static class RecordingCaseStartLifecycleParseHandler extends AbstractCmmnParseHandler<Case> {
        @Override
        public Collection<Class<? extends BaseElement>> getHandledTypes() {
            return Collections.singletonList(Case.class);
        }

        @Override
        protected void executeParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, Case caze) {
            caze.addStartLifecycleHandler(new RecordingCaseStartLifecycleHandler());
        }
    }

    public static class RecordingCaseStartLifecycleHandler implements CaseDefinitionStartLifecycleHandler {

        public static final String OBSOLETE_TYPE = "myTestCaseStartObsoleteType";

        public static final List<String> RECORDED = new CopyOnWriteArrayList<>();

        @Override
        public void deploy(CaseDefinitionStartDeployContext context) {
            String prefix = context.isRestoringPreviousVersion() ? "restore@" : "deploy@";
            RECORDED.add(prefix + context.getCaseDefinition().getVersion());
            context.getEventSubscriptionService().createEventSubscriptionBuilder()
                    .eventType(OBSOLETE_TYPE)
                    .scopeDefinitionId(context.getCaseDefinition().getId())
                    .scopeType(ScopeTypes.CMMN)
                    .create();
        }

        @Override
        public void undeploy(CaseDefinitionStartUndeployContext context) {
            RECORDED.add("undeploy@" + context.getPreviousCaseDefinition().getVersion());
            context.registerObsoleteEventSubscriptionType(OBSOLETE_TYPE);
        }
    }
}
