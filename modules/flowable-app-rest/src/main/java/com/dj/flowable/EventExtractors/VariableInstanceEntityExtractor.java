package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class VariableInstanceEntityExtractor extends AbstractExtractor implements EntityExtractor {
	
	VariableInstanceEntity variableInstanceEntity;
	
	@Override
	public boolean isAbleToExtract(Object event) {
		boolean isAble = event instanceof VariableInstanceEntity;
		if(isAble) {
			variableInstanceEntity = (VariableInstanceEntity) event; 
		}
		return isAble;
	}
	
	
	@Override
	public Map<String, Object> getProperties(){
		
		HashMap<String, Object> props = new HashMap<>();


        putSafe(props, "name", variableInstanceEntity.getName());
        putSafe(props, "value", variableInstanceEntity.getTextValue());
        putSafe(props, "value2", variableInstanceEntity.getTextValue2());

        putSafe(props, "taskId", variableInstanceEntity.getTaskId());

		
		return props;
	}


	@Override
	public Optional<String> getTaskKey() {
		return Optional.empty();
	}


	@Override
	public Optional<String> getProcessId() {
		return Optional.of(variableInstanceEntity.getProcessInstanceId());
	}


	@Override
	public Optional<String> getUser() {
		return Optional.empty();
	}

}
