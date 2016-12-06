package org.flowable.engine.impl.cmd;

import java.util.List;

import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ValidationError;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;

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
      throw new FlowableException("No process validator defined");
    }

    return processValidator.validate(bpmnModel);
  }

}
