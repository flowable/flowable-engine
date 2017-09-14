package org.flowable.engine.test.bpmn.event.error;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThrowingDelegate implements JavaDelegate {
	private static final Logger logger = LoggerFactory.getLogger(ThrowingDelegate.class);
	
	@Override
	public void execute(DelegateExecution execution) {
		logger.info("Entered throwing delegate");
		Boolean localError = (Boolean) execution.getVariable("localError");
		
		if (localError) {
			logger.info("Throwing local error");
			throw new BpmnError("localError");			
		}
	}

}
