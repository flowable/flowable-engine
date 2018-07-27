package com.dj.flowable.EventExtractors;

import java.util.Map;
import java.util.Optional;

public interface EntityExtractor {

	boolean isAbleToExtract(Object event);

	Map<String, Object> getProperties();
	
	public Optional<String> getTaskKey();
	public Optional<String> getProcessId();
	public Optional<String> getUser();

	  
}