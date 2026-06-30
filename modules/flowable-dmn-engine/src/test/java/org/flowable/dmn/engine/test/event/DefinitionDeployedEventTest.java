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
package org.flowable.dmn.engine.test.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.api.repository.FlowableDefinition;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefinitionDeployedEventTest extends BaseFlowableDmnTest {

    private static final String DMN_RESOURCE = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn";

    private final CapturingListener listener = new CapturingListener();

    @BeforeEach
    void addListener() {
        dmnEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    void removeListener() {
        dmnEngineConfiguration.getEventDispatcher().removeEventListener(listener);
    }

    @Test
    void deployDispatchesDefinitionDeployedEvent() {
        String deploymentId = repositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addClasspathResource(DMN_RESOURCE)
                .deploy()
                .getId();
        try {
            DmnDecision decision = repositoryService.createDecisionQuery()
                    .deploymentId(deploymentId).singleResult();

            assertThat(listener.deployed).hasSize(1);
            FlowableDefinition deployed = (FlowableDefinition) listener.deployed.get(0).getEntity();
            assertThat(deployed.getKey()).isEqualTo(decision.getKey());
            assertThat(deployed.getVersion()).isEqualTo(decision.getVersion());
            assertThat(deployed.getDeploymentId()).isEqualTo(deploymentId);
        } finally {
            repositoryService.deleteDeployment(deploymentId);
        }
    }

    @Test
    void deleteDeploymentDispatchesDefinitionUndeployedEvent() {
        String deploymentId = repositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addClasspathResource(DMN_RESOURCE)
                .deploy()
                .getId();
        DmnDecision decision = repositoryService.createDecisionQuery()
                .deploymentId(deploymentId).singleResult();

        repositoryService.deleteDeployment(deploymentId);

        assertThat(listener.undeployed).hasSize(1);
        FlowableDefinition undeployed = (FlowableDefinition) listener.undeployed.get(0).getEntity();
        assertThat(undeployed.getKey()).isEqualTo(decision.getKey());
        assertThat(undeployed.getVersion()).isEqualTo(decision.getVersion());
        assertThat(undeployed.getDeploymentId()).isEqualTo(deploymentId);
    }

    @Test
    void duplicateDeploymentDoesNotDispatchDefinitionDeployedEvent() {
        String firstDeploymentId = repositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addClasspathResource(DMN_RESOURCE)
                .enableDuplicateFiltering()
                .deploy()
                .getId();
        listener.deployed.clear();

        String secondDeploymentId = repositoryService.createDeployment()
                .name("definitionEventDeployment")
                .addClasspathResource(DMN_RESOURCE)
                .enableDuplicateFiltering()
                .deploy()
                .getId();
        try {
            assertThat(secondDeploymentId).isEqualTo(firstDeploymentId);
            assertThat(listener.deployed).isEmpty();
        } finally {
            repositoryService.deleteDeployment(firstDeploymentId);
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
