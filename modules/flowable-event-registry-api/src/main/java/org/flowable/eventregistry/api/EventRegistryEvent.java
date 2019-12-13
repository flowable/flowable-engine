package org.flowable.eventregistry.api;

public interface EventRegistryEvent {

    String getType();
    
    Object getEventObject();
}
