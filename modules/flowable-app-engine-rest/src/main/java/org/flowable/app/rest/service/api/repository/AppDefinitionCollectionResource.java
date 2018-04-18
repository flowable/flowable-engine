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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.engine.impl.repository.AppDefinitionQueryProperty;
import org.flowable.app.rest.AppRestResponseFactory;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@Api(tags = { "App Definitions" }, description = "Manage App Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class AppDefinitionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", AppDefinitionQueryProperty.APP_DEFINITION_ID);
        properties.put("key", AppDefinitionQueryProperty.APP_DEFINITION_KEY);
        properties.put("category", AppDefinitionQueryProperty.APP_DEFINITION_CATEGORY);
        properties.put("name", AppDefinitionQueryProperty.APP_DEFINITION_NAME);
        properties.put("version", AppDefinitionQueryProperty.APP_DEFINITION_VERSION);
        properties.put("deploymentId", AppDefinitionQueryProperty.APP_DEFINITION_DEPLOYMENT_ID);
        properties.put("tenantId", AppDefinitionQueryProperty.APP_DEFINITION_TENANT_ID);
    }

    @Autowired
    protected AppRestResponseFactory appRestResponseFactory;

    @Autowired
    protected AppRepositoryService appRepositoryService;

    @ApiOperation(value = "List of app definitions",  nickname = "listAppDefinitions", tags = { "App Definitions" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return app definitions with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return app definitions with a category like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return app definitions not with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return app definitions with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return app definitions with a key like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return app definitions with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return app definitions with a name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return app definitions with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return app definitions a resource name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "version", dataType = "string", value = "Only return app definitions with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "versionGreaterThan", dataType = "string", value = "Only return app definitions with a version greater than the given value.", paramType = "query"),
            @ApiImplicitParam(name = "versionGreaterThanOrEquals", dataType = "string", value = "Only return app definitions with a version greater than or equal to the given value.", paramType = "query"),
            @ApiImplicitParam(name = "versionLowerThan", dataType = "string", value = "Only return app definitions with a version lower than the given value.", paramType = "query"),
            @ApiImplicitParam(name = "versionLowerThanOrEquals", dataType = "string", value = "Only return app definitions with a version lower than or equal to the given value.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return app definitions with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return app definitions with the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return app definitions with a tenant id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "string", value = "Only return app definitions without a tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "If true; only the latest versions will be returned.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "key,category,id,version,name,deploymentId,tenantId", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the app definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status-message contains additional information.")
    })
    @GetMapping(value = "/app-repository/app-definitions", produces = "application/json")
    public DataResponse<AppDefinitionResponse> getForms(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        AppDefinitionQuery appDefinitionQuery = appRepositoryService.createAppDefinitionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            appDefinitionQuery.appDefinitionCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            appDefinitionQuery.appDefinitionCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            appDefinitionQuery.appDefinitionCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            appDefinitionQuery.appDefinitionKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            appDefinitionQuery.appDefinitionKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("name")) {
            appDefinitionQuery.appDefinitionName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            appDefinitionQuery.appDefinitionNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            appDefinitionQuery.appDefinitionResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            appDefinitionQuery.appDefinitionResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            appDefinitionQuery.appDefinitionVersion(Integer.valueOf(allRequestParams.get("version")));
        }
        if (allRequestParams.containsKey("versionGreaterThan")) {
            appDefinitionQuery.appDefinitionVersionGreaterThan(Integer.valueOf(allRequestParams.get("versionGreaterThan")));
        }
        if (allRequestParams.containsKey("versionGreaterThanOrEquals")) {
            appDefinitionQuery.appDefinitionVersionGreaterThanOrEquals(Integer.valueOf(allRequestParams.get("versionGreaterThanOrEquals")));
        }
        if (allRequestParams.containsKey("versionLowerThan")) {
            appDefinitionQuery.appDefinitionVersionLowerThan(Integer.valueOf(allRequestParams.get("versionLowerThan")));
        }
        if (allRequestParams.containsKey("versionLowerThanOrEquals")) {
            appDefinitionQuery.appDefinitionVersionLowerThanOrEquals(Integer.valueOf(allRequestParams.get("versionLowerThanOrEquals")));
        }
        if (allRequestParams.containsKey("deploymentId")) {
            appDefinitionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            appDefinitionQuery.appDefinitionTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            appDefinitionQuery.appDefinitionTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        if (allRequestParams.containsKey("withoutTenantId")) {
            Boolean withoutTenantId = Boolean.valueOf(allRequestParams.get("withoutTenantId"));
            if (withoutTenantId) {
                appDefinitionQuery.appDefinitionWithoutTenantId();
            }
        }
        if (allRequestParams.containsKey("latest")) {
            Boolean latest = Boolean.valueOf(allRequestParams.get("latest"));
            if (latest) {
                appDefinitionQuery.latestVersion();
            }
        }

        return new AppDefinitionPaginateList(appRestResponseFactory).paginateList(allRequestParams, appDefinitionQuery, "name", properties);
    }
}
