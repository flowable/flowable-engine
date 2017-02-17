package org.activiti.rest.editor.model;

import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorFactory;
import org.activiti.validation.ValidationError;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Endpoint for the activiti modeler to validate the current model.
 *
 * Created by Pardo David on 16/02/2017.
 */
@RestController
public class ModelValidationRestResource {

	@RequestMapping(value = "/model/validate",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<ValidationError> validate(@RequestBody JsonNode body){
		BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(body);
		ProcessValidator validator = new ProcessValidatorFactory().createDefaultProcessValidator();
		List<ValidationError> errors = validator.validate(bpmnModel);
		return errors;
	}

}
