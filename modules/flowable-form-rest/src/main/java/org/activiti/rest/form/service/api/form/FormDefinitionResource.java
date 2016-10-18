/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.activiti.rest.form.service.api.form;

import org.activiti.form.api.FormService;
import org.activiti.form.engine.ActivitiFormIllegalArgumentException;
import org.activiti.form.engine.ActivitiFormObjectNotFoundException;
import org.activiti.form.model.CompletedFormDefinition;
import org.activiti.form.model.FormDefinition;
import org.activiti.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Yvo Swillens
 */
public class FormDefinitionResource {

  @Autowired
  protected FormService formService;

  @Autowired
  protected FormRestResponseFactory formRestResponseFactory;

  @RequestMapping(value = "/form/runtime-form-definition", method = RequestMethod.POST, produces = "application/json")
  public FormDefinition getRuntimeFormDefinition(@RequestBody FormDefinitionRequest formDefinitionRequest, HttpServletRequest request) {

    FormDefinition formDefinition;

    if (formDefinitionRequest.getFormDefinitionKey() != null) {
      formDefinition = formService.getTaskFormDefinitionByKeyAndParentDeploymentId(
          formDefinitionRequest.getParentDeploymentId(),
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormDefinitionKey() != null) {
      formDefinition = formService.getTaskFormDefinitionByKey(
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormId() != null) {
      formDefinition = formService.getTaskFormDefinitionById(
          formDefinitionRequest.getFormId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else {
      throw new ActivitiFormIllegalArgumentException("Either form definition key of form id must be provided in the request");
    }

    if (formDefinition == null) {
      throw new ActivitiFormObjectNotFoundException("Could not find a form definition");
    }

    return formRestResponseFactory.createRuntimeFormDefinitionResponse(formDefinition);
  }

  @RequestMapping(value = "/form/completed-form-definition", method = RequestMethod.POST, produces = "application/json")
  public CompletedFormDefinitionResponse getCompletedFormDefinition(@RequestBody FormDefinitionRequest formDefinitionRequest, HttpServletRequest request) {

    CompletedFormDefinition formDefinition;

    if (formDefinitionRequest.getParentDeploymentId() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionByKeyAndParentDeploymentId(
          formDefinitionRequest.getParentDeploymentId(),
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getTaskId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormDefinitionKey() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionByKey(
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getTaskId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormId() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionById(
          formDefinitionRequest.getFormId(),
          formDefinitionRequest.getTaskId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else {
      throw new ActivitiFormIllegalArgumentException("Either form definition key of form id must be provided in the request");
    }

    if (formDefinition == null) {
      throw new ActivitiFormObjectNotFoundException("Could not find a form definition");
    }

    return formRestResponseFactory.createCompletedFormDefinitionResponse(formDefinition);
  }


  @RequestMapping(value = "/form/submitted-form/store", method = RequestMethod.POST, produces = "application/json")
  public void storeSubmittedForm(@RequestBody FormDefinitionRequest formDefinitionRequest, HttpServletRequest request) {

    FormDefinition formDefinition;

    if (formDefinitionRequest.getFormDefinitionKey() != null) {
      formDefinition = formService.getTaskFormDefinitionByKey(
          formDefinitionRequest.getFormDefinitionKey(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else if (formDefinitionRequest.getFormId() != null) {
      formDefinition = formService.getTaskFormDefinitionById(
          formDefinitionRequest.getFormId(),
          formDefinitionRequest.getProcessInstanceId(),
          formDefinitionRequest.getVariables(),
          formDefinitionRequest.getTenantId()
      );
    } else {
      throw new ActivitiFormIllegalArgumentException("Either form definition key of form id must be provided in the request");
    }

    if (formDefinition == null) {
      throw new ActivitiFormObjectNotFoundException("Could not find a form definition");
    }

    formService.storeSubmittedForm(formDefinitionRequest.getVariables(), formDefinition, formDefinitionRequest.getTaskId(),
        formDefinitionRequest.getProcessInstanceId());
  }
}
