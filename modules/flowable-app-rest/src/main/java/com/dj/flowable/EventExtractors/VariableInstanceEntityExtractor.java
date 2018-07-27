package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class VariableInstanceEntityExtractor extends AbstractExtractor implements EntityExtractor {
	
	
	@Override
	public boolean isAbleToExtract(Object event) {
		return event instanceof VariableInstanceEntity;
	}
	
	
	@Override
	public Map<String, Object> getProperties(Object event){

		VariableInstanceEntity variableInstanceEntity = (VariableInstanceEntity) event;
		
		HashMap<String, Object> props = new HashMap<>();

        putSafe(props, "name", variableInstanceEntity.getName());
        putSafe(props, "value", variableInstanceEntity.getTextValue());
        putSafe(props, "value2", variableInstanceEntity.getTextValue2());

        putSafe(props, "taskId", variableInstanceEntity.getTaskId());

		
		return props;
	}


	@Override
	public Optional<String> getTaskKey(Object event) {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId(Object event) {
		VariableInstanceEntity variableInstanceEntity = (VariableInstanceEntity) event;
		return Optional.of(variableInstanceEntity.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser(Object event) {
		return Optional.empty();
	}

}
