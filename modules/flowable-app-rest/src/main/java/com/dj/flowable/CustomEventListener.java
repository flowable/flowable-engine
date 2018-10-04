package com.dj.flowable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Created by kychoms on 10/05/17.
 */
public class CustomEventListener implements FlowableEventListener {

	private static Logger logger = LoggerFactory.getLogger(CustomEventListener.class);

	private static final String BPM_ACTION = "/bpm-action";
    
    private String baseEntryPoint;
    
    public CustomEventListener(String url) {
    	super();
   		baseEntryPoint = url;
    	
    }

  

	public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        // Be careful without this all Tomcat can freeze by not responding request !!!
        factory.setReadTimeout(2000);
        factory.setConnectTimeout(2000);
        return factory;
    }


    @Override
    public void onEvent(FlowableEvent event) {
    	
        try {
			FlowableEventType eventType = event.getType();

			
			if (eventType == FlowableEngineEventType.TASK_COMPLETED) {

			    if(logger.isInfoEnabled()) {
			    	String message = "EventListener raised .....of Type: " + eventType + " Class: " + event.getClass();
			    	logger.info(message);
			    }
			    			    
			    Map<String, Object> body = new HashMap<>();
			    body.put("taskId", ((TaskEntity)((FlowableEntityEventImpl) event).getEntity()).getId());

			    if(logger.isInfoEnabled()) {
			    	logger.info("Calling with: " + body);
			    }
			    
			    restTemplate().put(baseEntryPoint + BPM_ACTION, body);
			    
			    if(logger.isInfoEnabled()) {
			    	logger.info("Called Rest without exceptions!!");
			    }
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}


    }


	@Override
    public boolean isFailOnException() {
        logger.error( "FailOnException !!");
        return false;
    }


}
