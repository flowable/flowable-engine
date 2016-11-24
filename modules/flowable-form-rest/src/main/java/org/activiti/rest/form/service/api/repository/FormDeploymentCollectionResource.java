/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.rest.form.service.api.repository;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.common.api.ActivitiIllegalArgumentException;
import org.activiti.engine.common.api.query.QueryProperty;
import org.activiti.form.api.FormDeployment;
import org.activiti.form.api.FormDeploymentBuilder;
import org.activiti.form.api.FormDeploymentQuery;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.engine.impl.DeploymentQueryProperty;
import org.activiti.rest.api.DataResponse;
import org.activiti.rest.form.FormRestResponseFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * @author Yvo Swillens
 */
public class FormDeploymentCollectionResource {

  private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

  static {
    allowedSortProperties.put("id", DeploymentQueryProperty.DEPLOYMENT_ID);
    allowedSortProperties.put("name", DeploymentQueryProperty.DEPLOYMENT_NAME);
    allowedSortProperties.put("deployTime", DeploymentQueryProperty.DEPLOY_TIME);
    allowedSortProperties.put("tenantId", DeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
  }

  @Autowired
  protected FormRestResponseFactory formRestResponseFactory;

  @Autowired
  protected FormRepositoryService formRepositoryService;

  @RequestMapping(value = "/form-repository/deployments", method = RequestMethod.GET, produces = "application/json")
  public DataResponse getDeployments(@RequestParam Map<String, String> allRequestParams) {
    FormDeploymentQuery deploymentQuery = formRepositoryService.createDeploymentQuery();

    // Apply filters
    if (allRequestParams.containsKey("name")) {
      deploymentQuery.deploymentName(allRequestParams.get("name"));
    }
    if (allRequestParams.containsKey("nameLike")) {
      deploymentQuery.deploymentNameLike(allRequestParams.get("nameLike"));
    }
    if (allRequestParams.containsKey("category")) {
      deploymentQuery.deploymentCategory(allRequestParams.get("category"));
    }
    if (allRequestParams.containsKey("categoryNotEquals")) {
      deploymentQuery.deploymentCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
    }
    if (allRequestParams.containsKey("tenantId")) {
      deploymentQuery.deploymentTenantId(allRequestParams.get("tenantId"));
    }
    if (allRequestParams.containsKey("tenantIdLike")) {
      deploymentQuery.deploymentTenantIdLike(allRequestParams.get("tenantIdLike"));
    }
    if (allRequestParams.containsKey("withoutTenantId")) {
      Boolean withoutTenantId = Boolean.valueOf(allRequestParams.get("withoutTenantId"));
      if (withoutTenantId) {
        deploymentQuery.deploymentWithoutTenantId();
      }
    }

    DataResponse response = new FormDeploymentsPaginateList(formRestResponseFactory).paginateList(allRequestParams, deploymentQuery, "id", allowedSortProperties);
    return response;
  }

  @RequestMapping(value = "/form-repository/deployments", method = RequestMethod.POST, produces = "application/json")
  public FormDeploymentResponse uploadDeployment(@RequestParam(value = "tenantId", required = false) String tenantId, HttpServletRequest request, HttpServletResponse response) {

    if (request instanceof MultipartHttpServletRequest == false) {
      throw new ActivitiIllegalArgumentException("Multipart request is required");
    }

    MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

    if (multipartRequest.getFileMap().size() == 0) {
      throw new ActivitiIllegalArgumentException("Multipart request with file content is required");
    }

    MultipartFile file = multipartRequest.getFileMap().values().iterator().next();

    try {
      FormDeploymentBuilder deploymentBuilder = formRepositoryService.createDeployment();
      String fileName = file.getOriginalFilename();
      if (StringUtils.isEmpty(fileName) || !(fileName.endsWith(".form") || fileName.endsWith(".xml"))) {
        fileName = file.getName();
      }

      if (fileName.endsWith(".form") || fileName.endsWith(".xml")) {
        deploymentBuilder.addInputStream(fileName, file.getInputStream());
      } else {
        throw new ActivitiIllegalArgumentException("File must be of type .xml or .form");
      }
      deploymentBuilder.name(fileName);

      if (tenantId != null) {
        deploymentBuilder.tenantId(tenantId);
      }

      FormDeployment deployment = deploymentBuilder.deploy();
      response.setStatus(HttpStatus.CREATED.value());

      return formRestResponseFactory.createFormDeploymentResponse(deployment);

    } catch (Exception e) {
      if (e instanceof ActivitiException) {
        throw (ActivitiException) e;
      }
      throw new ActivitiException(e.getMessage(), e);
    }

  }


}
