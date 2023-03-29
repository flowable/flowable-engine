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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Filip Hrisafov
 */
@JsonInclude(Include.NON_NULL)
public class KafkaOutboundChannelModel extends OutboundChannelModel {

    protected String topic;
    protected RecordKey recordKey;

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

    public RecordKey getRecordKey() {
        return recordKey;
    }

    @JsonDeserialize(using = RecordKeyDeserializer.class)
    public void setRecordKey(RecordKey recordKey) {
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

    @JsonInclude(Include.NON_NULL)
    public static class RecordKey {

        protected String staticKey;

        protected String eventField;

        protected String delegateExpression;

        protected String expression;

        public String getStaticKey() {
            return staticKey;
        }

        public void setStaticKey(String staticKey) {
            this.staticKey = staticKey;
        }

        public String getEventField() {
            return eventField;
        }

        public void setEventField(String eventField) {
            this.eventField = eventField;
        }

        public String getDelegateExpression() {
            return delegateExpression;
        }

        public void setDelegateExpression(String delegateExpression) {
            this.delegateExpression = delegateExpression;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }
    }

    // backward compatibility
    static class RecordKeyDeserializer extends JsonDeserializer<RecordKey> {

        @Override
        public RecordKey deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            final JsonToken token = jsonParser.currentToken();

            if (JsonToken.START_OBJECT.equals(token)) {
                return (RecordKey) deserializationContext.findRootValueDeserializer(deserializationContext.constructType(RecordKey.class)).deserialize(jsonParser, deserializationContext);
            } else {
                RecordKey recordKey = new RecordKey();
                recordKey.setStaticKey((String) deserializationContext.findRootValueDeserializer(deserializationContext.constructType(String.class)).deserialize(jsonParser, deserializationContext));
                return recordKey;
            }
        }
    }
}
