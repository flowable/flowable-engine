package org.flowable.eventregistry.spring.test.kafka;

import java.util.function.Function;

import org.flowable.eventregistry.api.OutboundEvent;
import org.flowable.eventregistry.spring.kafka.KafkaMessageKeyProvider;

public class TestKafkaMessageKeyProvider implements KafkaMessageKeyProvider {

    protected Function<OutboundEvent<?>, Object> messageKeyProvider;

    @Override
    public Object determineMessageKey(OutboundEvent<?> eventInstance) {
        if (messageKeyProvider != null) {
            return messageKeyProvider.apply(eventInstance);
        }
        return null;
    }

    public void setMessageKeyProvider(Function<OutboundEvent<?>, Object> messageKeyProvider) {
        this.messageKeyProvider = messageKeyProvider;
    }

    public void clear() {
        this.messageKeyProvider = null;
    }
}
