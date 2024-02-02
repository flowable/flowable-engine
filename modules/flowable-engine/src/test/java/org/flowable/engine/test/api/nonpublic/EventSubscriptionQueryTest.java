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

package org.flowable.engine.test.api.nonpublic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionQueryTest extends PluggableFlowableTestCase {

    @Test
    public void testQueryByEventName() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity2);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity3 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity3.setEventName("messageName2");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().eventName("messageName").list();
        assertThat(list).hasSize(2);

        list = newEventSubscriptionQuery().eventName("messageName2").list();
        assertThat(list).hasSize(1);

        cleanDb();

    }

    @Test
    public void testQueryByEventType() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity2);

                SignalEventSubscriptionEntity signalEventSubscriptionEntity3 = eventSubscriptionService.createSignalEventSubscription();
                signalEventSubscriptionEntity3.setEventName("messageName2");
                eventSubscriptionService.insertEventSubscription(signalEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().eventType("signal").list();
        assertThat(list).hasSize(1);

        list = newEventSubscriptionQuery().eventType("message").list();
        assertThat(list).hasSize(2);

        cleanDb();

    }

    @Test
    public void testQueryByActivityId() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                messageEventSubscriptionEntity1.setActivityId("someActivity");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                messageEventSubscriptionEntity2.setActivityId("someActivity");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity2);

                SignalEventSubscriptionEntity signalEventSubscriptionEntity3 = eventSubscriptionService.createSignalEventSubscription();
                signalEventSubscriptionEntity3.setEventName("messageName2");
                signalEventSubscriptionEntity3.setActivityId("someOtherActivity");
                eventSubscriptionService.insertEventSubscription(signalEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().activityId("someOtherActivity").list();
        assertThat(list).hasSize(1);

        list = newEventSubscriptionQuery().activityId("someActivity").eventType("message").list();
        assertThat(list).hasSize(2);

        cleanDb();

    }

    @Test
    public void testQueryByEventSubscriptionId() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                messageEventSubscriptionEntity1.setActivityId("someActivity");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = eventSubscriptionService.createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                messageEventSubscriptionEntity2.setActivityId("someOtherActivity");
                eventSubscriptionService.insertEventSubscription(messageEventSubscriptionEntity2);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().activityId("someOtherActivity").list();
        assertThat(list).hasSize(1);

        final EventSubscription entity = list.get(0);

        list = newEventSubscriptionQuery().id(entity.getId()).list();

        assertThat(list).hasSize(1);

        cleanDb();

    }

    @Test
    @Deployment
    public void testQueryByExecutionId() {

        // starting two instances:
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");
        runtimeService.startProcessInstanceByKey("catchSignal");

        // test query by process instance id
        EventSubscription subscription = newEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(subscription).isNotNull();

        Execution executionWaitingForSignal = runtimeService.createExecutionQuery().activityId("signalEvent").processInstanceId(processInstance.getId()).singleResult();

        // test query by execution id
        EventSubscription signalSubscription = newEventSubscriptionQuery().executionId(executionWaitingForSignal.getId()).singleResult();
        assertThat(signalSubscription).isNotNull();

        assertThat(subscription).isEqualTo(signalSubscription);

        cleanDb();

    }

    protected EventSubscriptionQuery newEventSubscriptionQuery() {
        return runtimeService.createEventSubscriptionQuery();
    }

    protected void cleanDb() {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery().list();
        for (final EventSubscription eventSubscriptionEntity : subscriptions) {
            commandExecutor.execute(new Command<Void>() {

                @Override
                public Void execute(CommandContext commandContext) {
                    EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
                    eventSubscriptionService.deleteEventSubscription((EventSubscriptionEntity) eventSubscriptionEntity);

                    return null;
                }
            });
        }
    }
}
