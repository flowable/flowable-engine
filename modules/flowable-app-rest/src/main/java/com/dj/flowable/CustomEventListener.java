package com.dj.flowable;

import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.impl.cmd.GetModelEditorSourceExtraCmd;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.event.FlowableVariableEvent;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.dj.flowable.EventExtractors.EntityExtractor;
import com.dj.flowable.EventExtractors.ExecutionEntityImplExtractor;
import com.dj.flowable.EventExtractors.FlowableActivityEventExtractor;
import com.dj.flowable.EventExtractors.FlowableVariableEventExtractor;
import com.dj.flowable.EventExtractors.TaskEntityExtractor;
import com.dj.flowable.EventExtractors.VariableInstanceEntityExtractor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kychoms on 10/05/17.
 */
public class CustomEventListener implements FlowableEventListener {

	private static Logger logger = LoggerFactory.getLogger(CustomEventListener.class);

	private static final String BPM_ACTION = "/bpm-action";
	private static final String OVL = "OVL";

    
    private String baseEntryPoint;

    
    public List<EntityExtractor> extractors;
    
    
    
    public CustomEventListener(String url) {
    	super();
   		baseEntryPoint = url;
    	initExtractors();
    	
    }

    private void initExtractors() {
    	extractors = new ArrayList<>();
    	extractors.add(new ExecutionEntityImplExtractor());
    	extractors.add(new FlowableActivityEventExtractor());
    	extractors.add(new FlowableVariableEventExtractor());
    	extractors.add(new TaskEntityExtractor());
    	extractors.add(new VariableInstanceEntityExtractor());
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

			    Map<String, Object> body = new HashMap<>();


			    if(logger.isInfoEnabled()) {
			    	String message = "EventListener raised .....of Type: " + eventType + " Class: " + event.getClass();
			    	logger.info(message);
			    }

			    //Get Extrator
			    EntityExtractor extractor = getExtractor(event);

			    //Extract data
			    Map<String, Object> props = extractor.getProperties(event);
			    props.put("timestamp", Calendar.getInstance().getTime());
			    
			    body.put("properties", props);
			    body.put("processId", extractor.getProcessId(event).orElse(""));
			    body.put("user", extractor.getUser(event).orElse(""));
			    body.put("taskKey", extractor.getTaskKey(event).orElse(""));
			    body.put("eventType", eventType);
			    body.put("userProcessResult", OVL);

			    if(logger.isInfoEnabled()) {
			    	logger.info( "Calling with: " + body);
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

    private EntityExtractor getExtractor(FlowableEvent event) throws Exception {
		return extractors.stream()
			.filter(e -> e.isAbleToExtract(event))
			.findFirst()
			.orElseThrow(() -> new Exception("No extractor finded to: " + event.getClass()));
	}

	@Override
    public boolean isFailOnException() {
        logger.error( "FailOnException !!");
        return false;
    }


}
