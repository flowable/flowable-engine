package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.variable.api.event.FlowableVariableEvent;

public class FlowableVariableEventExtractor extends AbstractExtractor implements EntityExtractor {
	
	FlowableVariableEvent flowableVariableEvent;
	
	@Override
	public boolean isAbleToExtract(Object event) {
		boolean isAble = event instanceof FlowableVariableEvent;
		if(isAble) {
			flowableVariableEvent = (FlowableVariableEvent) event;
		}
		return event instanceof FlowableVariableEvent;
	}
	
	
	@Override
	public Map<String, Object> getProperties(){
		
		HashMap<String, Object> props = new HashMap<>();
		

        putSafe(props, "name",flowableVariableEvent.getVariableName());
        putSafe(props, "value",flowableVariableEvent.getVariableValue());

        putSafe(props, "processDefinitionId", flowableVariableEvent.getProcessDefinitionId());
        putSafe(props, "taskId", flowableVariableEvent.getTaskId());

    
		
		return props;
	}


	@Override
	public Optional<String> getTaskKey() {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId() {
		return Optional.of(flowableVariableEvent.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser() {
		return Optional.empty();
	}


	

}
