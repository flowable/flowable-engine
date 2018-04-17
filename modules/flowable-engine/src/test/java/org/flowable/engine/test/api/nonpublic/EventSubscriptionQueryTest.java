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
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.EventSubscriptionQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionQueryTest extends PluggableFlowableTestCase {

    public void testQueryByEventName() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity2);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity3 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity3.setEventName("messageName2");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().eventName("messageName").list();
        assertEquals(2, list.size());

        list = newEventSubscriptionQuery().eventName("messageName2").list();
        assertEquals(1, list.size());

        cleanDb();

    }

    public void testQueryByEventType() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity2);

                SignalEventSubscriptionEntity signalEventSubscriptionEntity3 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createSignalEventSubscription();
                signalEventSubscriptionEntity3.setEventName("messageName2");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(signalEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().eventType("signal").list();
        assertEquals(1, list.size());

        list = newEventSubscriptionQuery().eventType("message").list();
        assertEquals(2, list.size());

        cleanDb();

    }

    public void testQueryByActivityId() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                messageEventSubscriptionEntity1.setActivityId("someActivity");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                messageEventSubscriptionEntity2.setActivityId("someActivity");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity2);

                SignalEventSubscriptionEntity signalEventSubscriptionEntity3 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createSignalEventSubscription();
                signalEventSubscriptionEntity3.setEventName("messageName2");
                signalEventSubscriptionEntity3.setActivityId("someOtherActivity");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(signalEventSubscriptionEntity3);

                return null;
            }
        });

        List<EventSubscription> list = newEventSubscriptionQuery().activityId("someOtherActivity").list();
        assertEquals(1, list.size());

        list = newEventSubscriptionQuery().activityId("someActivity").eventType("message").list();
        assertEquals(2, list.size());

        cleanDb();

    }

    public void testQueryByEventSubscriptionId() {

        processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                MessageEventSubscriptionEntity messageEventSubscriptionEntity1 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity1.setEventName("messageName");
                messageEventSubscriptionEntity1.setActivityId("someActivity");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity1);

                MessageEventSubscriptionEntity messageEventSubscriptionEntity2 = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
                messageEventSubscriptionEntity2.setEventName("messageName");
                messageEventSubscriptionEntity2.setActivityId("someOtherActivity");
                CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(messageEventSubscriptionEntity2);

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
                    EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
                    eventSubscriptionEntityManager.delete((EventSubscriptionEntity) eventSubscriptionEntity);

                    return null;
                }
            });
        }
    }
}
