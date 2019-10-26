package org.flowable.common.engine.api.eventbus;

import java.time.LocalDateTime;
import java.util.Map;

public interface FlowableEventBusEvent {

    String getType();

    void setType(String type);

    String getScopeId();

    void setScopeId(String scopeId);

    String getScopeType();

    void setScopeType(String scopeType);

    String getScopeDefinitionId();

    void setScopeDefinitionId(String scopeDefinitionId);

    String getScopeDefinitionKey();

    void setScopeDefinitionKey(String scopeDefinitionKey);

    String getCorrelationKey();

    void setCorrelationKey(String correlationKey);

    //TODO why not instant?
    LocalDateTime getCreated();

    void setCreated(LocalDateTime created);

    Map<String, Object> getData();

    void setData(Map<String, Object> data);
}
