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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collection;
import java.util.HashSet;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ChannelDefinitionQueryTest extends AbstractFlowableEventTest {

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
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.channel")
                .deploy()
                .getId();
        deploymentsToDelete.add(deployment1Id);

        String deployment2Id = repositoryService.createDeployment()
                .name("Deployment 2")
                .parentDeploymentId("parent2")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.channel")
                .addClasspathResource("org/flowable/eventregistry/test/repository/two.channel")
                .deploy()
                .getId();
        deploymentsToDelete.add(deployment2Id);

        String deployment3Id = repositoryService.createDeployment()
                .name("Deployment 3")
                .addClasspathResource("org/flowable/eventregistry/test/repository/one.channel")
                .deploy()
                .getId();
        deploymentsToDelete.add(deployment3Id);

        assertThat(repositoryService.createChannelDefinitionQuery().parentDeploymentId("parent1").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getDeploymentId)
                .containsExactlyInAnyOrder(
                        tuple("one", deployment1Id)
                );
        assertThat(repositoryService.createChannelDefinitionQuery().parentDeploymentId("parent1").count()).isEqualTo(1);

        assertThat(repositoryService.createChannelDefinitionQuery().parentDeploymentId("parent2").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getDeploymentId)
                .containsExactlyInAnyOrder(
                        tuple("one", deployment2Id),
                        tuple("two", deployment2Id)
                );
        assertThat(repositoryService.createChannelDefinitionQuery().parentDeploymentId("parent2").count()).isEqualTo(2);

        assertThat(repositoryService.createChannelDefinitionQuery().parentDeploymentId("unknown").list()).isEmpty();
        assertThat(repositoryService.createChannelDefinitionQuery().parentDeploymentId("unknown").count()).isEqualTo(0);
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void queryByNameLikeIgnoreCase() {
        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "One Channel"),
                        tuple("one-outbound", "One Outbound"),
                        tuple("two", "Two channel")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isEqualTo(3);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLike("%channel").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("two", "Two channel")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLike("%channel").count()).isEqualTo(1);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLike("%Channel").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "One Channel")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLike("%Channel").count()).isEqualTo(1);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLikeIgnoreCase("%channel").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "One Channel"),
                        tuple("two", "Two channel")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLikeIgnoreCase("%channel").count()).isEqualTo(2);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLikeIgnoreCase("%Channel").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getName)
                .containsExactlyInAnyOrder(
                        tuple("one", "One Channel"),
                        tuple("two", "Two channel")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLikeIgnoreCase("%Channel").count()).isEqualTo(2);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLikeIgnoreCase("%dummy").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getName)
                .isEmpty();
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionNameLikeIgnoreCase("%dummy").count()).isZero();
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void queryByKeyLikeIgnoreCase() {
        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey)
                .containsExactlyInAnyOrder("one", "one-outbound", "two");
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isEqualTo(3);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKeyLike("%Ne%").list())
                .extracting(ChannelDefinition::getKey)
                .isEmpty();
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKeyLike("%Ne%").count()).isZero();

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKeyLikeIgnoreCase("%Ne%").list())
                .extracting(ChannelDefinition::getKey)
                .containsExactlyInAnyOrder("one", "one-outbound");
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKeyLikeIgnoreCase("%Ne%").count()).isEqualTo(2);

        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKeyLikeIgnoreCase("%dummy").list())
                .extracting(ChannelDefinition::getKey)
                .isEmpty();
        assertThat(repositoryService.createChannelDefinitionQuery().channelDefinitionKeyLikeIgnoreCase("%dummy").count()).isZero();
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void queryOnlyInbound() {
        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType)
                .containsExactlyInAnyOrder(
                        tuple("one", "inbound"),
                        tuple("one-outbound", "outbound"),
                        tuple("two", "inbound")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isEqualTo(3);

        assertThat(repositoryService.createChannelDefinitionQuery().onlyInbound().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType)
                .containsExactlyInAnyOrder(
                        tuple("one", "inbound"),
                        tuple("two", "inbound")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().onlyInbound().count()).isEqualTo(2);
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void queryOnlyOutbound() {
        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType)
                .containsExactlyInAnyOrder(
                        tuple("one", "inbound"),
                        tuple("one-outbound", "outbound"),
                        tuple("two", "inbound")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isEqualTo(3);

        assertThat(repositoryService.createChannelDefinitionQuery().onlyOutbound().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getType)
                .containsExactlyInAnyOrder(
                        tuple("one-outbound", "outbound")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().onlyOutbound().count()).isEqualTo(1);
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void queryCombinedOnlyOutboundAndOnlyInboundShouldNotBePossible() {
        assertThatThrownBy(() -> repositoryService.createChannelDefinitionQuery().onlyInbound().onlyOutbound())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine onlyOutbound() with onlyInbound() in the same query");

        assertThatThrownBy(() -> repositoryService.createChannelDefinitionQuery().onlyOutbound().onlyInbound())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine onlyInbound() with onlyOutbound() in the same query");
    }

    @Test
    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/test/repository/one.channel",
            "org/flowable/eventregistry/test/repository/one-outbound.channel",
            "org/flowable/eventregistry/test/repository/two.channel"
    })
    void queryByImplementation() {
        assertThat(repositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("one", "jms"),
                        tuple("one-outbound", "rabbit"),
                        tuple("two", "jms")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isEqualTo(3);

        assertThat(repositoryService.createChannelDefinitionQuery().implementation("rabbit").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("one-outbound", "rabbit")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().implementation("rabbit").count()).isEqualTo(1);

        assertThat(repositoryService.createChannelDefinitionQuery().implementation("jms").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getImplementation)
                .containsExactlyInAnyOrder(
                        tuple("one", "jms"),
                        tuple("two", "jms")
                );
        assertThat(repositoryService.createChannelDefinitionQuery().implementation("jms").count()).isEqualTo(2);

        assertThat(repositoryService.createChannelDefinitionQuery().implementation("dummy").list())
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getImplementation)
                .isEmpty();
        assertThat(repositoryService.createChannelDefinitionQuery().implementation("dummy").count()).isZero();
    }

}
