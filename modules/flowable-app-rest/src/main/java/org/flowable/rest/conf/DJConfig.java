package org.flowable.rest.conf;

import javax.annotation.PostConstruct;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.variable.service.impl.types.SerializableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dj.flowable.CustomEventListener;
import com.dj.flowable.JsonVariableType;

@Component
public class DJConfig {
	
	@Autowired
	ProcessEngineConfigurationImpl springProcessEngineConfiguration;

	@Value("${url.dj.adapter:http://localhost:8080}")
	private String defaulDjAdapterUrl;
	
	
	
	
	@PostConstruct
	void init() {
		
		//Add custom event listener
		//Add listener into eventDispatcher 
		springProcessEngineConfiguration.getEventDispatcher()
			.addEventListener(new CustomEventListener(defaulDjAdapterUrl));
		
		//Add json DDBB serializator
		springProcessEngineConfiguration.getVariableTypes().addType(new JsonVariableType(true), 
				springProcessEngineConfiguration.getVariableTypes().getTypeIndex(SerializableType.TYPE_NAME));
		
	}


}
