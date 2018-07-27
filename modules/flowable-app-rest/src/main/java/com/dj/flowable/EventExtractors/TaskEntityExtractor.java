package com.dj.flowable.EventExtractors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class TaskEntityExtractor extends AbstractExtractor implements EntityExtractor {
	
	TaskEntity taskEntity;
	
	@Override
	public boolean isAbleToExtract(Object event) {
		boolean isAble = event instanceof FlowableEntityEvent && ((FlowableEntityEventImpl) event).getEntity() instanceof TaskEntity;
		if(isAble) {
			taskEntity = (TaskEntity) ((FlowableEntityEventImpl)event).getEntity();
		}
		return isAble;
	}
	
	
	@Override
	public Map<String, Object> getProperties(){

		
		HashMap<String, Object> props = new HashMap<>();
		
        putSafe(props, "createTime",    taskEntity.getCreateTime());
        putSafe(props, "claimTime",     taskEntity.getClaimTime());
        putSafe(props, "taskId",        taskEntity.getId());
        putSafe(props, "taskName",      taskEntity.getName());
        putSafe(props, "definitionKey", taskEntity.getTaskDefinitionKey());
        putSafe(props, "eventName",     taskEntity.getEventName());

        putSafe(props, "assigne",       taskEntity.getAssignee());
        putSafe(props, "owner",         taskEntity.getOwner());

        putSafe(props, "processVariables", taskEntity.getProcessVariables());
        putSafe(props, "taskVariables", taskEntity.getVariables());
        
        return props;
	}

     
	@Override
	public Optional<String> getTaskKey() {
		return Optional.of(taskEntity.getTaskDefinitionKey());
	}


	@Override
	public Optional<String> getProcessId() {
		return Optional.empty();
	}


	@Override
	public Optional<String> getUser() {
		return Optional.of(taskEntity.getAssignee());
	}

}
