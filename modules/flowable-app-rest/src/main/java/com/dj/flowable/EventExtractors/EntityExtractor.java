package com.dj.flowable.EventExtractors;

import java.util.Map;
import java.util.Optional;

public interface EntityExtractor {

	boolean isAbleToExtract(Object event);

	Map<String, Object> getProperties(Object event);
	
	public Optional<String> getTaskKey(Object event);
	public Optional<String> getProcessId(Object event);
	public Optional<String> getUser(Object event);

	  
}