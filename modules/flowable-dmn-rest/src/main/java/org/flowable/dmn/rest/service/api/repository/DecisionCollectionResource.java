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

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.dmn.api.DmnDecisionQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.impl.DecisionQueryProperty;
import org.flowable.dmn.rest.service.api.DmnRestApiInterceptor;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
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
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Decisions" }, authorizations = { @Authorization(value = "basicAuth") })
public class DecisionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", DecisionQueryProperty.DECISION_ID);
        properties.put("key", DecisionQueryProperty.DECISION_KEY);
        properties.put("category", DecisionQueryProperty.DECISION_CATEGORY);
        properties.put("name", DecisionQueryProperty.DECISION_NAME);
        properties.put("version", DecisionQueryProperty.DECISION_VERSION);
        properties.put("deploymentId", DecisionQueryProperty.DECISION_DEPLOYMENT_ID);
        properties.put("tenantId", DecisionQueryProperty.DECISION_TENANT_ID);
        properties.put("decisionType", DecisionQueryProperty.DECISION_TYPE);
    }

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnRepositoryService dmnRepositoryService;
    
    @Autowired(required=false)
    protected DmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List of decision", tags = { "Decisions" }, nickname = "listDecisions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return decision with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return decision with a category like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return decision which do not have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return decision with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return decision with a name like the given key.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return decision with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return decision with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return decision with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return decision with a name like the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "version", dataType = "integer", value = "Only return decision with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "Only return the latest decision versions. Can only be used together with key and keyLike parameters, using any other parameter will result in a 400-response.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return decisions which are part of a deployment with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "parentDeploymentId", dataType = "string", value = "Only return decisions which are part of a deployment with the given parent deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return decision with the given tenant ID.", paramType = "query"),
            @ApiImplicitParam(name = "decisionType", dataType = "string", value = "Only return decision with the given type.", paramType = "query"),
            @ApiImplicitParam(name = "decisionTypeLike", dataType = "string", value = "Only return decision like the given type.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "name,id,key,category,deploymentId,version,decisionType", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process-definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that latest is used with other parameters other than key and keyLike. The status-message contains additional information.")
    })
    @GetMapping(value = "/dmn-repository/decisions", produces = "application/json")
    public DataResponse<DecisionResponse> getDecisions(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        DmnDecisionQuery definitionQuery = dmnRepositoryService.createDecisionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            definitionQuery.decisionCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            definitionQuery.decisionCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            definitionQuery.decisionCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            definitionQuery.decisionKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            definitionQuery.decisionKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("name")) {
            definitionQuery.decisionName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            definitionQuery.decisionNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            definitionQuery.decisionResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            definitionQuery.decisionResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            definitionQuery.decisionVersion(Integer.valueOf(allRequestParams.get("version")));
        }

        if (allRequestParams.containsKey("latest")) {
            if (Boolean.parseBoolean(allRequestParams.get("latest"))) {
                definitionQuery.latestVersion();
            }
        }
        if (allRequestParams.containsKey("deploymentId")) {
            definitionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("parentDeploymentId")) {
            definitionQuery.parentDeploymentId(allRequestParams.get("parentDeploymentId"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            definitionQuery.decisionTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            definitionQuery.decisionTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        if (allRequestParams.containsKey("decisionType")) {
            definitionQuery.decisionType(allRequestParams.get("decisionType"));
        }
        if (allRequestParams.containsKey("decisionTypeLike")) {
            definitionQuery.decisionTypeLike(allRequestParams.get("decisionType"));
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessDecisionTableInfoWithQuery(definitionQuery);
        }

        return paginateList(allRequestParams, definitionQuery, "name", properties, dmnRestResponseFactory::createDecisionResponseList);
    }
}
