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
package org.flowable.engine.test.api.event;

import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.TransactionFlowableEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestTransactionFlowableEventListener implements TransactionFlowableEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTransactionFlowableEventListener.class);
    private List<FlowableEvent> eventsReceived;
    private String onTransaction = "";
    
    public TestTransactionFlowableEventListener() {
        eventsReceived = new ArrayList<>();
    }

    public List<FlowableEvent> getEventsReceived() {
        return eventsReceived;
    }

    public void clearEventsReceived() {
        eventsReceived.clear();
    }

    @Override
    public String getOnTransaction() {
        return onTransaction;
    }

    @Override
    public void setOnTransaction(String onTransaction) {

        this.onTransaction = onTransaction;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        eventsReceived.add(event);
        LOGGER.debug("{} {} event triggered ... {}", onTransaction, event.getType(), eventsReceived.size());
    }

    public boolean isEventTriggered(FlowableEngineEventType flowableEvent) {
        for (FlowableEvent event : eventsReceived) {
            if (event.getType() == flowableEvent) return true;
        }
        return false;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
