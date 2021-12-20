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

import java.util.Collection;
import java.util.HashSet;

import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class EventDefinitionQueryTest extends AbstractFlowableEventTest {

    protected Collection<String> deploymentsToDelete = new HashSet<>();

    @AfterEach
    void tearDown() {
        deploymentsToDelete.forEach(repositoryService::deleteDeployment);
        deploymentsToDelete.clear();
    }

    @Test
    void queryByParentDeploymentId() {
        String deployment1Id = repositoryService.createDeployment()
                .name("Deployment 1")
                .parentDeploymentId("parent1")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.event")
                .deploy()
                .getId();
        deploymentsToDelete.add(deployment1Id);

        String deployment2Id = repositoryService.createDeployment()
                .name("Deployment 2")
                .parentDeploymentId("parent2")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.event")
                .addClasspathResource("org/flowable/eventregistry/test/repository/two.event")
                .deploy()
                .getId();
        deploymentsToDelete.add(deployment2Id);

        String deployment3Id = repositoryService.createDeployment()
                .name("Deployment 3")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.event")
                .deploy()
                .getId();
        deploymentsToDelete.add(deployment3Id);

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

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.event",
            "org/flowable/eventregistry/test/repository/two.event"
    })
    void queryByNameLikeIgnoreCase() {
        assertThat(repositoryService.createEventDefinitionQuery().list())
                .extracting(EventDefinition::getKey, EventDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "My first Event"),
                        tuple("two", "My second event")
                );
        assertThat(repositoryService.createEventDefinitionQuery().count()).isEqualTo(2);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLike("%event").list())
                .extracting(EventDefinition::getKey, EventDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("two", "My second event")
                );
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLike("%event").count()).isEqualTo(1);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLike("%Event").list())
                .extracting(EventDefinition::getKey, EventDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "My first Event")
                );
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLike("%Event").count()).isEqualTo(1);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLikeIgnoreCase("%event").list())
                .extracting(EventDefinition::getKey, EventDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "My first Event"),
                        tuple("two", "My second event")
                );
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLikeIgnoreCase("%event").count()).isEqualTo(2);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLikeIgnoreCase("%Event").list())
                .extracting(EventDefinition::getKey, EventDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "My first Event"),
                        tuple("two", "My second event")
                );
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLikeIgnoreCase("%Event").count()).isEqualTo(2);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLikeIgnoreCase("%dummy").list())
                .extracting(EventDefinition::getKey, EventDefinition::getName)
                .isEmpty();
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionNameLikeIgnoreCase("%dummy").count()).isZero();
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.event",
            "org/flowable/eventregistry/test/repository/one-test.event",
            "org/flowable/eventregistry/test/repository/two.event"
    })
    void queryByKeyLikeIgnoreCase() {
        assertThat(repositoryService.createEventDefinitionQuery().list())
                .extracting(EventDefinition::getKey)
                .containsExactlyInAnyOrder("one", "one-test", "two");
        assertThat(repositoryService.createEventDefinitionQuery().count()).isEqualTo(3);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionKeyLike("%Ne%").list())
                .extracting(EventDefinition::getKey)
                .isEmpty();
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionKeyLike("%Ne%").count()).isZero();

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionKeyLikeIgnoreCase("%Ne%").list())
                .extracting(EventDefinition::getKey)
                .containsExactlyInAnyOrder("one", "one-test");
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionKeyLikeIgnoreCase("%Ne%").count()).isEqualTo(2);

        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionKeyLikeIgnoreCase("%dummy").list())
                .extracting(EventDefinition::getKey)
                .isEmpty();
        assertThat(repositoryService.createEventDefinitionQuery().eventDefinitionKeyLikeIgnoreCase("%dummy").count()).isZero();
    }
}
