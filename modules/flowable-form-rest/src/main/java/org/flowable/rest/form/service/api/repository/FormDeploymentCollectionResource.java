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
package org.flowable.rest.form.service.api.repository;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormDeploymentBuilder;
import org.flowable.form.api.FormDeploymentQuery;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.impl.DeploymentQueryProperty;
import org.flowable.rest.api.DataResponse;
import org.flowable.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Deployments" }, description = "Manage Form Deployments")
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

  @ApiOperation(value = "List of Form Deployments", tags = {"Form Deployments"})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "name", dataType = "string", value = "Only return form deployments with the given name.", paramType = "query"),
      @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return form deployments with a name like the given name.", paramType = "query"),
      @ApiImplicitParam(name = "category", dataType = "string", value = "Only return form deployments with the given category.", paramType = "query"),
      @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return form deployments which don’t have the given category.", paramType = "query"),
      @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return form deployments with the given tenantId.", paramType = "query"),
      @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return form deployments with a tenantId like the given value.", paramType = "query"),
      @ApiImplicitParam(name = "withoutTenantId", dataType = "string", value = "If true, only returns form deployments without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
      @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues ="id,name,deployTime,tenantId", paramType = "query"),
  })
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Indicates the request was successful."),
  })
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

  @ApiOperation(value = "Create a new form deployment", tags = {"Form Deployments"}, consumes = "multipart/form-data", produces = "application/json",
      notes = "The request body should contain data of type multipart/form-data. There should be exactly one file in the request, any additional files will be ignored. The deployment name is the name of the file-field passed in. Make sure the file-name ends with .form or .xml.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Indicates the form deployment was created."),
      @ApiResponse(code = 400, message = "Indicates there was no content present in the request body or the content mime-type is not supported for form deployment. The status-description contains additional information.")
  })
  @RequestMapping(value = "/form-repository/deployments", method = RequestMethod.POST, produces = "application/json")
  public FormDeploymentResponse uploadDeployment(@ApiParam(name = "tenantId") @RequestParam(value = "tenantId", required = false) String tenantId, HttpServletRequest request, HttpServletResponse response) {

    if (!(request instanceof MultipartHttpServletRequest)) {
      throw new FlowableIllegalArgumentException("Multipart request is required");
    }

    MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

    if (multipartRequest.getFileMap().size() == 0) {
      throw new FlowableIllegalArgumentException("Multipart request with file content is required");
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
        throw new FlowableIllegalArgumentException("File must be of type .xml or .form");
      }
      deploymentBuilder.name(fileName);

      if (tenantId != null) {
        deploymentBuilder.tenantId(tenantId);
      }

      FormDeployment deployment = deploymentBuilder.deploy();
      response.setStatus(HttpStatus.CREATED.value());

      return formRestResponseFactory.createFormDeploymentResponse(deployment);

    } catch (Exception e) {
      if (e instanceof FlowableException) {
        throw (FlowableException) e;
      }
      throw new FlowableException(e.getMessage(), e);
    }
  }
}
