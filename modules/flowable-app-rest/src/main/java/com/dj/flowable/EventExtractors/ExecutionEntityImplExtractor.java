package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;

public class ExecutionEntityImplExtractor extends AbstractExtractor implements EntityExtractor {
	
	@Override
	public boolean isAbleToExtract(Object event) {
		return event instanceof FlowableEntityEvent && ((FlowableEntityEventImpl) event).getEntity() instanceof ExecutionEntityImpl;
	}
	
	
	@Override
	public Map<String, Object> getProperties(Object event) {

		ExecutionEntityImpl executionEntityImpl = (ExecutionEntityImpl) ((FlowableEntityEventImpl)event).getEntity();
		
		HashMap<String, Object> props = new HashMap<>();

		putSafe(props, "activityName", executionEntityImpl.getActivityName());
		putSafe(props, "processDefName", executionEntityImpl.getProcessDefinitionName());

		putSafe(props, "processVariables", executionEntityImpl.getProcessVariables());
		putSafe(props, "taskVariables", executionEntityImpl.getVariables());

		return props;
	}


	@Override
	public Optional<String> getTaskKey(Object event) {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId(Object event) {
		ExecutionEntityImpl executionEntityImpl = (ExecutionEntityImpl) ((FlowableEntityEventImpl)event).getEntity();
		return Optional.of(executionEntityImpl.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser(Object event) {
		ExecutionEntityImpl executionEntityImpl = (ExecutionEntityImpl) ((FlowableEntityEventImpl)event).getEntity();
		return Optional.of(executionEntityImpl.getStartUserId());
	}


	

}
