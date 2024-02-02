package org.flowable.eventregistry.spring.test.kafka;

import java.util.function.Function;

import org.flowable.eventregistry.api.OutboundEvent;
import org.flowable.eventregistry.spring.kafka.KafkaPartitionProvider;

public class TestKafkaPartitionProvider implements KafkaPartitionProvider {

    Function<OutboundEvent<?>, Integer> partitionProvider;

    @Override
    public Integer determinePartition(OutboundEvent<?> outboundEvent) {
        if (partitionProvider != null) {
            return partitionProvider.apply(outboundEvent);
        }
        return null;
    }

    public void setPartitionProvider(Function<OutboundEvent<?>, Integer> partitionProvider) {
        this.partitionProvider = partitionProvider;
    }

    public void clear() {
        this.partitionProvider = null;
    }
}
