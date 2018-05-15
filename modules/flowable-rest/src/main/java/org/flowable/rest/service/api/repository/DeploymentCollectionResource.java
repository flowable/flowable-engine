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

package org.flowable.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.DeploymentQueryProperty;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * @author Tijs Rademakers
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Deployment" }, description = "Manage Deployment", authorizations = { @Authorization(value = "basicAuth") })
public class DeploymentCollectionResource {

    protected static final String DEPRECATED_API_DEPLOYMENT_SEGMENT = "deployment";

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("id", DeploymentQueryProperty.DEPLOYMENT_ID);
        allowedSortProperties.put("name", DeploymentQueryProperty.DEPLOYMENT_NAME);
        allowedSortProperties.put("deployTime", DeploymentQueryProperty.DEPLOY_TIME);
        allowedSortProperties.put("tenantId", DeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RepositoryService repositoryService;

    @ApiOperation(value = "List Deployments", tags = { "Deployment" }, nickname="listDeployments")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return deployments with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return deployments with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return deployments with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return deployments which don’t have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "parentDeploymentId", dataType = "string", value = "Only return deployments with the given parent deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "parentDeploymentIdLike", dataType = "string", value = "Only return deployments with a parent deployment id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return deployments with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return deployments with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns deployments without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,name,deployTime,tenantId", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the request was successful."),
    })
    @GetMapping(value = "/repository/deployments", produces = "application/json")
    public DataResponse<DeploymentResponse> getDeployments(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

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
        if (allRequestParams.containsKey("parentDeploymentId")) {
            deploymentQuery.parentDeploymentId(allRequestParams.get("parentDeploymentId"));
        }
        if (allRequestParams.containsKey("parentDeploymentIdLike")) {
            deploymentQuery.parentDeploymentIdLike(allRequestParams.get("parentDeploymentIdLike"));
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

        return new DeploymentsPaginateList(restResponseFactory).paginateList(allRequestParams, deploymentQuery, "id", allowedSortProperties);
    }

    @ApiOperation(value = "Create a new deployment", tags = {
            "Deployment" }, consumes = "multipart/form-data", produces = "application/json", notes = "The request body should contain data of type multipart/form-data. There should be exactly one file in the request, any additional files will be ignored. The deployment name is the name of the file-field passed in. If multiple resources need to be deployed in a single deployment, compress the resources in a zip and make sure the file-name ends with .bar or .zip.\n"
                    + "\n"
                    + "An additional parameter (form-field) can be passed in the request body with name tenantId. The value of this field will be used as the id of the tenant this deployment is done in.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the deployment was created."),
            @ApiResponse(code = 400, message = "Indicates there was no content present in the request body or the content mime-type is not supported for deployment. The status-description contains additional information.")
    })

    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form", required = true)
    })
    @PostMapping(value = "/repository/deployments", produces = "application/json", consumes = "multipart/form-data")
    public DeploymentResponse uploadDeployment(@ApiParam(name = "deploymentKey") @RequestParam(value = "deploymentKey", required = false) String deploymentKey,
            @ApiParam(name = "deploymentName") @RequestParam(value = "deploymentName", required = false) String deploymentName,
            @ApiParam(name = "tenantId") @RequestParam(value = "tenantId", required = false) String tenantId,
            HttpServletRequest request, HttpServletResponse response) {

        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new FlowableIllegalArgumentException("Multipart request is required");
        }

        String queryString = request.getQueryString();
        Map<String, String> decodedQueryStrings = splitQueryString(queryString);

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        if (multipartRequest.getFileMap().size() == 0) {
            throw new FlowableIllegalArgumentException("Multipart request with file content is required");
        }

        MultipartFile file = multipartRequest.getFileMap().values().iterator().next();

        try {
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
            String fileName = file.getOriginalFilename();
            if (StringUtils.isEmpty(fileName) || !(fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn") || fileName.toLowerCase().endsWith(".bar") || fileName.toLowerCase().endsWith(".zip"))) {

                fileName = file.getName();
            }

            if (fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn")) {
                deploymentBuilder.addInputStream(fileName, file.getInputStream());
            } else if (fileName.toLowerCase().endsWith(".bar") || fileName.toLowerCase().endsWith(".zip")) {
                deploymentBuilder.addZipInputStream(new ZipInputStream(file.getInputStream()));
            } else {
                throw new FlowableIllegalArgumentException("File must be of type .bpmn20.xml, .bpmn, .bar or .zip");
            }

            if (!decodedQueryStrings.containsKey("deploymentName") || StringUtils.isEmpty(decodedQueryStrings.get("deploymentName"))) {
                String fileNameWithoutExtension = fileName.split("\\.")[0];

                if (StringUtils.isNotEmpty(fileNameWithoutExtension)) {
                    fileName = fileNameWithoutExtension;
                }

                deploymentBuilder.name(fileName);
            } else {
                deploymentBuilder.name(decodedQueryStrings.get("deploymentName"));
            }

            if (decodedQueryStrings.containsKey("deploymentKey") && StringUtils.isNotEmpty(decodedQueryStrings.get("deploymentKey"))) {
                deploymentBuilder.key(decodedQueryStrings.get("deploymentKey"));
            }

            if (tenantId != null) {
                deploymentBuilder.tenantId(tenantId);
            }

            Deployment deployment = deploymentBuilder.deploy();

            response.setStatus(HttpStatus.CREATED.value());

            return restResponseFactory.createDeploymentResponse(deployment);

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            }
            throw new FlowableException(e.getMessage(), e);
        }
    }

    public Map<String, String> splitQueryString(String queryString) {
        if (StringUtils.isEmpty(queryString)) {
            return Collections.emptyMap();
        }
        Map<String, String> queryMap = new HashMap<>();
        for (String param : queryString.split("&")) {
            queryMap.put(StringUtils.substringBefore(param, "="), decode(StringUtils.substringAfter(param, "=")));
        }
        return queryMap;
    }

    protected String decode(String string) {
        if (string != null) {
            try {
                return URLDecoder.decode(string, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException("JVM does not support UTF-8 encoding.", uee);
            }
        }
        return null;
    }
}
