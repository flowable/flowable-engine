package org.activiti5.engine.impl.cmd;

import java.util.List;

import org.activiti5.engine.ActivitiException;
import org.activiti5.engine.impl.interceptor.Command;
import org.activiti5.engine.impl.interceptor.CommandContext;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ValidationError;

/**
 * @author Joram Barrez
 */
public class ValidateBpmnModelCmd implements Command<List<ValidationError>> {
	
	protected BpmnModel bpmnModel;
	
	public ValidateBpmnModelCmd(BpmnModel bpmnModel) {
		this.bpmnModel = bpmnModel;
	}

	@Override
	public List<ValidationError> execute(CommandContext commandContext) {
		ProcessValidator processValidator = commandContext.getProcessEngineConfiguration().getProcessValidator();
		if (processValidator == null) {
			throw new ActivitiException("No process validator defined");
		}
		
		return processValidator.validate(bpmnModel);
	}
	
}
