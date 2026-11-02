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
package org.flowable.app.engine.test.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.test.BaseFlowableAppTest;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.api.repository.FlowableDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefinitionDeployedEventTest extends BaseFlowableAppTest {

    private static final String APP_RESOURCE = "definitionEvent.app";

    private static final String APP_JSON = """
            {"key":"definitionEventApp","name":"Definition Event App"}""";

    private final CapturingListener listener = new CapturingListener();

    @BeforeEach
    void addListener() {
        appEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    void removeListener() {
        appEngineConfiguration.getEventDispatcher().removeEventListener(listener);
    }

    @Test
    void deployDispatchesDefinitionDeployedEvent() {
        String deploymentId = appRepositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addString(APP_RESOURCE, APP_JSON)
                .deploy()
                .getId();
        try {
            AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery()
                    .deploymentId(deploymentId).singleResult();

            assertThat(listener.deployed).hasSize(1);
            FlowableDefinition deployed = (FlowableDefinition) listener.deployed.get(0).getEntity();
            assertThat(deployed.getKey()).isEqualTo(appDefinition.getKey());
            assertThat(deployed.getVersion()).isEqualTo(appDefinition.getVersion());
            assertThat(deployed.getDeploymentId()).isEqualTo(deploymentId);
        } finally {
            appRepositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    void deleteDeploymentDispatchesDefinitionUndeployedEvent() {
        String deploymentId = appRepositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addString(APP_RESOURCE, APP_JSON)
                .deploy()
                .getId();
        AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery()
                .deploymentId(deploymentId).singleResult();

        appRepositoryService.deleteDeployment(deploymentId, true);

        assertThat(listener.undeployed).hasSize(1);
        FlowableDefinition undeployed = (FlowableDefinition) listener.undeployed.get(0).getEntity();
        assertThat(undeployed.getKey()).isEqualTo(appDefinition.getKey());
        assertThat(undeployed.getVersion()).isEqualTo(appDefinition.getVersion());
        assertThat(undeployed.getDeploymentId()).isEqualTo(deploymentId);
    }

    @Test
    void duplicateDeploymentDoesNotDispatchDefinitionDeployedEvent() {
        String firstDeploymentId = appRepositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addString(APP_RESOURCE, APP_JSON)
                .enableDuplicateFiltering()
                .deploy()
                .getId();
        listener.deployed.clear();

        String secondDeploymentId = appRepositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addString(APP_RESOURCE, APP_JSON)
                .enableDuplicateFiltering()
                .deploy()
                .getId();
        try {
            assertThat(secondDeploymentId).isEqualTo(firstDeploymentId);
            assertThat(listener.deployed).isEmpty();
        } finally {
            appRepositoryService.deleteDeployment(firstDeploymentId, true);
        }
    }

    static class CapturingListener extends AbstractFlowableEventListener {

        final List<FlowableEntityEvent> deployed = new ArrayList<>();
        final List<FlowableEntityEvent> undeployed = new ArrayList<>();

        @Override
        public void onEvent(FlowableEvent event) {
            if (event.getType() == FlowableEngineEventType.DEFINITION_DEPLOYED) {
                deployed.add((FlowableEntityEvent) event);
            } else if (event.getType() == FlowableEngineEventType.DEFINITION_UNDEPLOYED) {
                undeployed.add((FlowableEntityEvent) event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }

        @Override
        public Collection<? extends FlowableEventType> getTypes() {
            return List.of(FlowableEngineEventType.DEFINITION_DEPLOYED, FlowableEngineEventType.DEFINITION_UNDEPLOYED);
        }
    }
}
