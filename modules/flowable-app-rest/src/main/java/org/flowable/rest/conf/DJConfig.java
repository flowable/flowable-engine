package org.flowable.rest.conf;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.variable.service.impl.types.SerializableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.dj.flowable.CustomEventListener;
import com.dj.flowable.JsonVariableType;


@Component
@DependsOn("flowableEngineConfiguration")
public class DJConfig {
	
    @Autowired
	ProcessEngineConfigurationImpl processEngineConfiguration;

	@Value("${url.dj.adapter:http://localhost:8080/djadapter}")
	private String defaulDjAdapterUrl;

    private boolean loaded = false;
	
	//runs when Spring context is loaded
	@EventListener
	public synchronized void onApplicationEvent(ContextRefreshedEvent event) {

	    if(!loaded) {
	        loaded=true;
	        processEngineConfiguration.getEventDispatcher()
	        .addEventListener(new CustomEventListener(defaulDjAdapterUrl));
	        
	        processEngineConfiguration.getVariableTypes().addType(new JsonVariableType(true), 
	                processEngineConfiguration.getVariableTypes().getTypeIndex(SerializableType.TYPE_NAME));
	    }
	    
	}


 }
