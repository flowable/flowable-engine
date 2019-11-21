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

import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
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

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity2);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity3 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity3.setEventName("messageName2");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().eventName("messageName").list();
        assertEquals(2, list.size());

        list = newEventSubscriptionQuery().eventName("messageName2").list();
        assertEquals(1, list.size());

        cleanDb();

    }

    @Test
    public void testQueryByEventType() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity2);

                SignalEventSubscriptionEntity signalEventSubscriptionEntity3 = CommandContextUtil.getEventSubscriptionService(commandContext).createSignalEventSubscription();
                signalEventSubscriptionEntity3.setEventName("messageName2");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(signalEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().eventType("signal").list();
        assertEquals(1, list.size());

        list = newEventSubscriptionQuery().eventType("message").list();
        assertEquals(2, list.size());

        cleanDb();

    }

    @Test
    public void testQueryByActivityId() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                messageEventSubscriptionEntity1.setActivityId("someActivity");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                messageEventSubscriptionEntity2.setActivityId("someActivity");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity2);

                SignalEventSubscriptionEntity signalEventSubscriptionEntity3 = CommandContextUtil.getEventSubscriptionService(commandContext).createSignalEventSubscription();
                signalEventSubscriptionEntity3.setEventName("messageName2");
                signalEventSubscriptionEntity3.setActivityId("someOtherActivity");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(signalEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().activityId("someOtherActivity").list();
        assertEquals(1, list.size());

        list = newEventSubscriptionQuery().activityId("someActivity").eventType("message").list();
        assertEquals(2, list.size());

        cleanDb();

    }

    @Test
    public void testQueryByEventSubscriptionId() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                messageEventSubscriptionEntity1.setActivityId("someActivity");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionService(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                messageEventSubscriptionEntity2.setActivityId("someOtherActivity");
                CommandContextUtil.getEventSubscriptionService(commandContext).insertEventSubscription(messageEventSubscriptionEntity2);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().activityId("someOtherActivity").list();
        assertEquals(1, list.size());

        final EventSubscription entity = list.get(0);

        list = newEventSubscriptionQuery().id(entity.getId()).list();

        assertEquals(1, list.size());

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
        assertNotNull(subscription);

        Execution executionWaitingForSignal = runtimeService.createExecutionQuery().activityId("signalEvent").processInstanceId(processInstance.getId()).singleResult();

        // test query by execution id
        EventSubscription signalSubscription = newEventSubscriptionQuery().executionId(executionWaitingForSignal.getId()).singleResult();
        assertNotNull(signalSubscription);

        assertEquals(signalSubscription, subscription);

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
                    EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService(commandContext);
                    eventSubscriptionService.deleteEventSubscription((EventSubscriptionEntity) eventSubscriptionEntity);

                    return null;
                }
            });
        }
    }
}
