package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.variable.api.event.FlowableVariableEvent;

public class FlowableVariableEventExtractor extends AbstractExtractor implements EntityExtractor {
	
	
	@Override
	public boolean isAbleToExtract(Object event) {
		return event instanceof FlowableVariableEvent;
	}
	
	
	@Override
	public Map<String, Object> getProperties(Object event){
		
		FlowableVariableEvent flowableVariableEvent = (FlowableVariableEvent) event;
		
		HashMap<String, Object> props = new HashMap<>();

        putSafe(props, "name",flowableVariableEvent.getVariableName());
        putSafe(props, "value",flowableVariableEvent.getVariableValue());

        putSafe(props, "processDefinitionId", flowableVariableEvent.getProcessDefinitionId());
        putSafe(props, "taskId", flowableVariableEvent.getTaskId());

    
		
		return props;
	}


	@Override
	public Optional<String> getTaskKey(Object event) {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId(Object event) {
		FlowableVariableEvent flowableVariableEvent = (FlowableVariableEvent) event;
		return Optional.of(flowableVariableEvent.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser(Object event) {
		return Optional.empty();
	}


	

}
