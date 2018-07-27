package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.delegate.event.FlowableActivityEvent;

public class FlowableActivityEventExtractor extends AbstractExtractor implements EntityExtractor {
	
	
	@Override
	public boolean isAbleToExtract(Object event) {
		return  event instanceof FlowableActivityEvent;
	}
	
	
	@Override
	public Map<String, Object> getProperties(Object event){
	
		FlowableActivityEvent flowableActivityEvent = (FlowableActivityEvent) event;
		
		HashMap<String, Object> props = new HashMap<>();
		
		putSafe(props, "activityId", flowableActivityEvent.getActivityId());
		putSafe(props, "activityName", flowableActivityEvent.getActivityName());
		
		return props;
	}


	@Override
	public Optional<String> getTaskKey(Object event) {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId(Object event) {
		FlowableActivityEvent flowableActivityEvent = (FlowableActivityEvent) event;
		return Optional.of(flowableActivityEvent.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser(Object event) {
		return Optional.empty();
	}


	

}
