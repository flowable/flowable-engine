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
package org.flowable.dmn.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.dmn.api.DmnDecisionTableQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.impl.DecisionTableQueryProperty;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Decision Tables" }, description = "Manage Decision Tables", authorizations = { @Authorization(value = "basicAuth") })
public class DecisionTableCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", DecisionTableQueryProperty.DECISION_TABLE_ID);
        properties.put("key", DecisionTableQueryProperty.DECISION_TABLE_KEY);
        properties.put("category", DecisionTableQueryProperty.DECISION_TABLE_CATEGORY);
        properties.put("name", DecisionTableQueryProperty.DECISION_TABLE_NAME);
        properties.put("version", DecisionTableQueryProperty.DECISION_TABLE_VERSION);
        properties.put("deploymentId", DecisionTableQueryProperty.DEPLOYMENT_ID);
        properties.put("tenantId", DecisionTableQueryProperty.DECISION_TABLE_TENANT_ID);
    }

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnRepositoryService dmnRepositoryService;

    @ApiOperation(value = "List of decision tables", tags = { "Decision Tables" }, nickname = "listDecisionTables")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return decision tables with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return decision tables with a category like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return decision tables which don’t have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return decision tables with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return decision tables with a name like the given key.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return decision tables with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return decision tables with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return decision tables with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return decision tables with a name like the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "version", dataType = "integer", value = "Only return decision tables with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "Only return the latest decision tables versions. Can only be used together with key and keyLike parameters, using any other parameter will result in a 400-response.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return decision tables with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return decision tables with the given tenant ID.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "name,id,key,category,deploymentId,version", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process-definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that latest is used with other parameters other than key and keyLike. The status-message contains additional information.")
    })
    @GetMapping(value = "/dmn-repository/decision-tables", produces = "application/json")
    public DataResponse<DecisionTableResponse> getDecisionTables(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        DmnDecisionTableQuery decisionTableQuery = dmnRepositoryService.createDecisionTableQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            decisionTableQuery.decisionTableCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            decisionTableQuery.decisionTableCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            decisionTableQuery.decisionTableCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            decisionTableQuery.decisionTableKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            decisionTableQuery.decisionTableKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("name")) {
            decisionTableQuery.decisionTableName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            decisionTableQuery.decisionTableNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            decisionTableQuery.decisionTableResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            decisionTableQuery.decisionTableResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            decisionTableQuery.decisionTableVersion(Integer.valueOf(allRequestParams.get("version")));
        }

        if (allRequestParams.containsKey("latest")) {
            Boolean latest = Boolean.valueOf(allRequestParams.get("latest"));
            if (latest != null && latest) {
                decisionTableQuery.latestVersion();
            }
        }
        if (allRequestParams.containsKey("deploymentId")) {
            decisionTableQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            decisionTableQuery.decisionTableTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            decisionTableQuery.decisionTableTenantIdLike(allRequestParams.get("tenantIdLike"));
        }

        return new DecisionTablesDmnPaginateList(dmnRestResponseFactory).paginateList(allRequestParams, decisionTableQuery, "name", properties);
    }
}
