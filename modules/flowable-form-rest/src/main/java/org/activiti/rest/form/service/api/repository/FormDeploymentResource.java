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
package org.activiti.rest.form.service.api.repository;

import org.activiti.form.api.FormDeployment;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.engine.ActivitiFormObjectNotFoundException;
import org.activiti.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Yvo Swillens
 */
@RestController
public class FormDeploymentResource {

  @Autowired
  protected FormRestResponseFactory formRestResponseFactory;

  @Autowired
  protected FormRepositoryService formRepositoryService;

  @RequestMapping(value = "/form-repository/deployments/{deploymentId}", method = RequestMethod.GET, produces = "application/json")
  public FormDeploymentResponse getFormDeployment(@PathVariable String deploymentId) {
    FormDeployment deployment = formRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();

    if (deployment == null) {
      throw new ActivitiFormObjectNotFoundException("Could not find a form deployment with id '"+deploymentId);
    }

    return formRestResponseFactory.createFormDeploymentResponse(deployment);
  }

  @RequestMapping(value = "/form-repository/deployments/{deploymentId}", method = RequestMethod.DELETE, produces = "application/json")
  public void deleteFormDeployment(@PathVariable String deploymentId, HttpServletResponse response) {
    formRepositoryService.deleteDeployment(deploymentId);
    response.setStatus(HttpStatus.NO_CONTENT.value());
  }
}
