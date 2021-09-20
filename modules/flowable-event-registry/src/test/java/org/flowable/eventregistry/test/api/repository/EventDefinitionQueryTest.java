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
package org.flowable.eventregistry.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class EventDefinitionQueryTest extends AbstractFlowableEventTest {

    protected String deployment1Id;
    protected String deployment2Id;
    protected String deployment3Id;

    @BeforeEach
    void setUp() {
        deployment1Id = repositoryService.createDeployment()
                .name("Deployment 1")
                .parentDeploymentId("parent1")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.event")
                .deploy()
                .getId();

        deployment2Id = repositoryService.createDeployment()
                .name("Deployment 2")
                .parentDeploymentId("parent2")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.event")
                .addClasspathResource("org/flowable/eventregistry/test/repository/two.event")
                .deploy()
                .getId();

        deployment3Id = repositoryService.createDeployment()
                .name("Deployment 3")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.event")
                .deploy()
                .getId();
    }

    @AfterEach
    void tearDown() {
        repositoryService.deleteDeployment(deployment1Id);
        repositoryService.deleteDeployment(deployment2Id);
        repositoryService.deleteDeployment(deployment3Id);
    }

    @Test
    void queryByParentDeploymentId() {
        assertThat(repositoryService.createEventDefinitionQuery().parentDeploymentId("parent1").list())
                .extracting(EventDefinition::getKey, EventDefinition::getDeploymentId)
                .containsExactlyInAnyOrder(
                        tuple("one", deployment1Id)
                );
        assertThat(repositoryService.createEventDefinitionQuery().parentDeploymentId("parent1").count()).isEqualTo(1);

        assertThat(repositoryService.createEventDefinitionQuery().parentDeploymentId("parent2").list())
                .extracting(EventDefinition::getKey, EventDefinition::getDeploymentId)
                .containsExactlyInAnyOrder(
                        tuple("one", deployment2Id),
                        tuple("two", deployment2Id)
                );
        assertThat(repositoryService.createEventDefinitionQuery().parentDeploymentId("parent2").count()).isEqualTo(2);

        assertThat(repositoryService.createEventDefinitionQuery().parentDeploymentId("unknown").list()).isEmpty();
        assertThat(repositoryService.createEventDefinitionQuery().parentDeploymentId("unknown").count()).isEqualTo(0);
    }
}
