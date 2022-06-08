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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Filip Hrisafov
 */
@JsonInclude(Include.NON_NULL)
public class KafkaInboundChannelModel extends InboundChannelModel {

    protected String groupId;
    protected Collection<String> topics;
    protected String topicPattern;
    protected String clientIdPrefix;
    protected String concurrency;
    protected RetryConfiguration retry;
    protected List<CustomProperty> customProperties;
    
    public KafkaInboundChannelModel() {
        super();
        setType("kafka");
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Collection<String> getTopics() {
        return topics;
    }

    public void setTopics(Collection<String> topics) {
        this.topics = topics;
    }

    public String getTopicPattern() {
        return topicPattern;
    }

    public void setTopicPattern(String topicPattern) {
        this.topicPattern = topicPattern;
    }

    public String getClientIdPrefix() {
        return clientIdPrefix;
    }

    public void setClientIdPrefix(String clientIdPrefix) {
        this.clientIdPrefix = clientIdPrefix;
    }

    public String getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(String concurrency) {
        this.concurrency = concurrency;
    }

    public RetryConfiguration getRetry() {
        return retry;
    }

    public void setRetry(RetryConfiguration retry) {
        this.retry = retry;
    }

    public List<CustomProperty> getCustomProperties() {
        return customProperties;
    }

    public void addCustomProperty(String name, String value) {
        if (customProperties == null) {
            customProperties = new ArrayList<>();
        }

        customProperties.add(new CustomProperty(name, value));
    }

    public void setCustomProperties(List<CustomProperty> properties) {
        this.customProperties = properties;
    }

    public static class CustomProperty {

        protected String name;
        protected String value;

        public CustomProperty() {

        }

        public CustomProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    public static class RetryConfiguration {

        protected String attempts;
        protected String dltTopicSuffix;
        protected String retryTopicSuffix;
        protected String fixedDelayTopicStrategy;
        protected String topicSuffixingStrategy;
        protected NonBlockingRetryBackOff nonBlockingBackOff;

        protected String autoCreateTopics;
        protected String numPartitions;
        protected String replicationFactor;

        public String getAttempts() {
            return attempts;
        }

        public void setAttempts(String attempts) {
            this.attempts = attempts;
        }

        public String getDltTopicSuffix() {
            return dltTopicSuffix;
        }

        public void setDltTopicSuffix(String dltTopicSuffix) {
            this.dltTopicSuffix = dltTopicSuffix;
        }

        public String getRetryTopicSuffix() {
            return retryTopicSuffix;
        }

        public void setRetryTopicSuffix(String retryTopicSuffix) {
            this.retryTopicSuffix = retryTopicSuffix;
        }

        public String getFixedDelayTopicStrategy() {
            return fixedDelayTopicStrategy;
        }

        public void setFixedDelayTopicStrategy(String fixedDelayTopicStrategy) {
            this.fixedDelayTopicStrategy = fixedDelayTopicStrategy;
        }

        public String getTopicSuffixingStrategy() {
            return topicSuffixingStrategy;
        }

        public void setTopicSuffixingStrategy(String topicSuffixingStrategy) {
            this.topicSuffixingStrategy = topicSuffixingStrategy;
        }

        public NonBlockingRetryBackOff getNonBlockingBackOff() {
            return nonBlockingBackOff;
        }

        public void setNonBlockingBackOff(NonBlockingRetryBackOff nonBlockingBackOff) {
            this.nonBlockingBackOff = nonBlockingBackOff;
        }

        public String getAutoCreateTopics() {
            return autoCreateTopics;
        }

        public void setAutoCreateTopics(String autoCreateTopics) {
            this.autoCreateTopics = autoCreateTopics;
        }

        public String getNumPartitions() {
            return numPartitions;
        }

        public void setNumPartitions(String numPartitions) {
            this.numPartitions = numPartitions;
        }

        public String getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(String replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }

    public static class NonBlockingRetryBackOff {

        protected String delay;
        protected String maxDelay;
        protected String multiplier;
        protected String random;

        public String getDelay() {
            return delay;
        }

        public void setDelay(String delay) {
            this.delay = delay;
        }

        public String getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(String maxDelay) {
            this.maxDelay = maxDelay;
        }

        public String getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(String multiplier) {
            this.multiplier = multiplier;
        }

        public String getRandom() {
            return random;
        }

        public void setRandom(String random) {
            this.random = random;
        }
    }

}
