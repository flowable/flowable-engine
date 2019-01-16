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
package org.flowable.app.rest.service.api.repository;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.api.repository.AppDeploymentQuery;
import org.flowable.app.engine.impl.repository.AppDeploymentQueryProperty;
import org.flowable.app.rest.AppRestApiInterceptor;
import org.flowable.app.rest.AppRestResponseFactory;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "App Deployments" }, description = "Manage App Deployments", authorizations = { @Authorization(value = "basicAuth") })
public class AppDeploymentCollectionResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("id", AppDeploymentQueryProperty.DEPLOYMENT_ID);
        allowedSortProperties.put("name", AppDeploymentQueryProperty.DEPLOYMENT_NAME);
        allowedSortProperties.put("deployTime", AppDeploymentQueryProperty.DEPLOY_TIME);
        allowedSortProperties.put("tenantId", AppDeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }

    @Autowired
    protected AppRestResponseFactory appRestResponseFactory;

    @Autowired
    protected AppRepositoryService appRepositoryService;
    
    @Autowired(required=false)
    protected AppRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List of App Deployments", nickname = "listAppDeployments", tags = { "Form Deployments" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return app deployments with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return app deployments with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return app deployments with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return app deployments which do not have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return app deployments with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return app deployments with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns app deployments without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "id,name,deployTime,tenantId", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the request was successful."),
    })
    @GetMapping(value = "/app-repository/deployments", produces = "application/json")
    public DataResponse<AppDeploymentResponse> getDeployments(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        AppDeploymentQuery deploymentQuery = appRepositoryService.createDeploymentQuery();

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
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessDeploymentsWithQuery(deploymentQuery);
        }

        return paginateList(allRequestParams, deploymentQuery, "id", allowedSortProperties, appRestResponseFactory::createAppDeploymentResponseList);
    }

    @ApiOperation(value = "Create a new app deployment", tags = {
            "App Deployments" }, consumes = "multipart/form-data", produces = "application/json", notes = "The request body should contain data of type multipart/form-data. There should be exactly one file in the request, any additional files will be ignored. The deployment name is the name of the file-field passed in. Make sure the file-name ends with .app, .zip or .bar.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the app deployment was created."),
            @ApiResponse(code = 400, message = "Indicates there was no content present in the request body or the content mime-type is not supported for app deployment. The status-description contains additional information.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name="file", paramType = "form", dataType = "java.io.File")
    })
    @PostMapping(value = "/app-repository/deployments", produces = "application/json", consumes = "multipart/form-data")
    public AppDeploymentResponse uploadDeployment(@ApiParam(name = "tenantId") @RequestParam(value = "tenantId", required = false) String tenantId, HttpServletRequest request, HttpServletResponse response) {

        if (restApiInterceptor != null) {
            restApiInterceptor.executeNewDeploymentForTenantId(tenantId);
        }
        
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
            AppDeploymentBuilder deploymentBuilder = appRepositoryService.createDeployment();
            String fileName = file.getOriginalFilename();
            if (StringUtils.isEmpty(fileName) || !(fileName.endsWith(".app") || fileName.toLowerCase().endsWith(".bar") || fileName.toLowerCase().endsWith(".zip"))) {
                fileName = file.getName();
            }

            if (fileName.endsWith(".app")) {
                deploymentBuilder.addInputStream(fileName, file.getInputStream());
            } else if (fileName.toLowerCase().endsWith(".bar") || fileName.toLowerCase().endsWith(".zip")) {
                deploymentBuilder.addZipInputStream(new ZipInputStream(file.getInputStream()));
            } else {
                throw new FlowableIllegalArgumentException("File must be of type .app");
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
            deploymentBuilder.name(fileName);

            if (tenantId != null) {
                deploymentBuilder.tenantId(tenantId);
            }

            if (restApiInterceptor != null) {
                restApiInterceptor.enhanceDeployment(deploymentBuilder);
            }

            AppDeployment deployment = deploymentBuilder.deploy();
            response.setStatus(HttpStatus.CREATED.value());

            return appRestResponseFactory.createAppDeploymentResponse(deployment);

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
