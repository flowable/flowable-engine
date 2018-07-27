package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;

public class ExecutionEntityImplExtractor extends AbstractExtractor implements EntityExtractor {
	
	
	ExecutionEntityImpl executionEntityImpl;
	
	@Override
	public boolean isAbleToExtract(Object event) {
		boolean isAble = event instanceof FlowableEntityEvent && ((FlowableEntityEventImpl) event).getEntity() instanceof ExecutionEntityImpl;
		if(isAble) {
			executionEntityImpl = (ExecutionEntityImpl) ((FlowableEntityEventImpl)event).getEntity();
		}
		return isAble;
	}
	
	
	@Override
	public Map<String, Object> getProperties() {

		HashMap<String, Object> props = new HashMap<>();

		putSafe(props, "activityName", executionEntityImpl.getActivityName());
		putSafe(props, "processDefName", executionEntityImpl.getProcessDefinitionName());

		putSafe(props, "processVariables", executionEntityImpl.getProcessVariables());
		putSafe(props, "taskVariables", executionEntityImpl.getVariables());

		return props;
	}


	@Override
	public Optional<String> getTaskKey() {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId() {
		return Optional.of(executionEntityImpl.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser() {
		return Optional.of(executionEntityImpl.getStartUserId());
	}


	

}
