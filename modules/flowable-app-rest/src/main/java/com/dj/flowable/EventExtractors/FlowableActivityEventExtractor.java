package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.delegate.event.FlowableActivityEvent;

public class FlowableActivityEventExtractor extends AbstractExtractor implements EntityExtractor {
	
	FlowableActivityEvent flowableActivityEvent;
	
	@Override
	public boolean isAbleToExtract(Object event) {
		boolean isAble = event instanceof FlowableActivityEvent;
		if(isAble) {
			flowableActivityEvent = (FlowableActivityEvent) event;
		}
		return isAble;
	}
	
	
	@Override
	public Map<String, Object> getProperties(){
		
		HashMap<String, Object> props = new HashMap<>();
		
		putSafe(props, "activityId", flowableActivityEvent.getActivityId());
		putSafe(props, "activityName", flowableActivityEvent.getActivityName());
		
		return props;
	}


	@Override
	public Optional<String> getTaskKey() {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId() {
		return Optional.of(flowableActivityEvent.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser() {
		return Optional.empty();
	}


	

}
