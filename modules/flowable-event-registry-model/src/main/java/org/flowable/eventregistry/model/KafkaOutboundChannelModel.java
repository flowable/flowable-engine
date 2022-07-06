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
package org.flowable.eventregistry.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Filip Hrisafov
 */
@JsonInclude(Include.NON_NULL)
public class KafkaOutboundChannelModel extends OutboundChannelModel {

    protected String topic;
    protected String recordKey;

    protected KafkaPartition partition;

    public KafkaOutboundChannelModel() {
        super();
        setType("kafka");
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public KafkaPartition getPartition() {
        return partition;
    }

    public void setPartition(KafkaPartition partition) {
        this.partition = partition;
    }

    @JsonInclude(Include.NON_NULL)
    public static class KafkaPartition {

        protected String eventField;
        protected String roundRobin;
        protected String delegateExpression;

        public String getEventField() {
            return eventField;
        }

        public void setEventField(String eventField) {
            this.eventField = eventField;
        }

        public String getRoundRobin() {
            return roundRobin;
        }

        public void setRoundRobin(String roundRobin) {
            this.roundRobin = roundRobin;
        }

        public String getDelegateExpression() {
            return delegateExpression;
        }

        public void setDelegateExpression(String delegateExpression) {
            this.delegateExpression = delegateExpression;
        }
    }
}
