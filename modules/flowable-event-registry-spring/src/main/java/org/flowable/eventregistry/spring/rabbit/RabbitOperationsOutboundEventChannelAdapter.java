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
package org.flowable.eventregistry.spring.rabbit;

import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.springframework.amqp.rabbit.core.RabbitOperations;

/**
 * @author Filip Hrisafov
 */
public class RabbitOperationsOutboundEventChannelAdapter implements OutboundEventChannelAdapter<String> {

    protected RabbitOperations rabbitOperations;
    protected String exchange;
    protected String routingKey;

    public RabbitOperationsOutboundEventChannelAdapter(RabbitOperations rabbitOperations, String exchange, String routingKey) {
        this.rabbitOperations = rabbitOperations;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void sendEvent(String rawEvent) {
        if (exchange != null) {
            rabbitOperations.convertAndSend(exchange, routingKey, rawEvent);
        } else {
            rabbitOperations.convertAndSend(routingKey, rawEvent);
        }
    }

    public RabbitOperations getRabbitOperations() {
        return rabbitOperations;
    }

    public void setRabbitOperations(RabbitOperations rabbitOperations) {
        this.rabbitOperations = rabbitOperations;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
