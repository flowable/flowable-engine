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

package org.flowable.eventregistry.rest.service.api.repository;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.RequestUtil;
import org.flowable.eventregistry.api.ChannelDefinitionQuery;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.ChannelDefinitionQueryProperty;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestApiInterceptor;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestResponseFactory;
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
@Api(tags = { "Channel Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class ChannelDefinitionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", ChannelDefinitionQueryProperty.ID);
        properties.put("key", ChannelDefinitionQueryProperty.KEY);
        properties.put("category", ChannelDefinitionQueryProperty.CATEGORY);
        properties.put("name", ChannelDefinitionQueryProperty.NAME);
        properties.put("deploymentId", ChannelDefinitionQueryProperty.DEPLOYMENT_ID);
        properties.put("createTime", ChannelDefinitionQueryProperty.CREATE_TIME);
        properties.put("tenantId", ChannelDefinitionQueryProperty.TENANT_ID);
    }

    @Autowired
    protected EventRegistryRestResponseFactory restResponseFactory;

    @Autowired
    protected EventRepositoryService repositoryService;
    
    @Autowired(required=false)
    protected EventRegistryRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List of channel definitions", tags = { "Channel Definitions" }, nickname = "listChannelDefinitions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "version", dataType = "integer", value = "Only return channel definitions with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return channel definitions with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return channel definitions with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return channel definitions with a name like the given name (case-insensitive).", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return channel definitions with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return channel definitions with a name like the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLikeIgnoreCase", dataType = "string", value = "Only return channel definitions with a name like the given key (case-insensitive).", paramType = "query"),
            @ApiImplicitParam(name = "createTime", dataType = "date-time", value = "Only return channel definitions with the given create time.", paramType = "query"),
            @ApiImplicitParam(name = "createTimeAfter", dataType = "date-time", value = "Only return channel definitions with a create time after the given date.", paramType = "query"),
            @ApiImplicitParam(name = "createTimeBefore", dataType = "date-time", value = "Only return channel definitions with a create time before the given date.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return channel definitions with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return channel definitions with a name like the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return channel definitions with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return channel definitions with a category like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return channel definitions which do not have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return channel definitions which are part of a deployment with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "parentDeploymentId", dataType = "string", value = "Only return channel definitions which are part of a deployment awith the given parent deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "Only return the latest channel definition versions. Can only be used together with key and keyLike parameters, using any other parameter will result in a 400-response.", paramType = "query"),
            @ApiImplicitParam(name = "onlyInbound", dataType = "boolean", value = "Only return the inbound channel definitions. Mutually exclusive with onlyOutbound", paramType = "query"),
            @ApiImplicitParam(name = "onlyOutbound", dataType = "boolean", value = "Only return the outbound channel definitions. Mutually exclusive with onlyInbound", paramType = "query"),
            @ApiImplicitParam(name = "implementation", dataType = "string", value = "Only return the channel definitions with the given implementation type.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "name,id,key,category,deploymentId,version", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the channel definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that latest is used with other parameters other than key and keyLike. The status-message contains additional information.")
    })
    @GetMapping(value = "/event-registry-repository/channel-definitions", produces = "application/json")
    public DataResponse<ChannelDefinitionResponse> getChannelDefinitions(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        ChannelDefinitionQuery channelDefinitionQuery = repositoryService.createChannelDefinitionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            channelDefinitionQuery.channelCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            channelDefinitionQuery.channelCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            channelDefinitionQuery.channelCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            channelDefinitionQuery.channelDefinitionKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            channelDefinitionQuery.channelDefinitionKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("keyLikeIgnoreCase")) {
            channelDefinitionQuery.channelDefinitionKeyLikeIgnoreCase(allRequestParams.get("keyLikeIgnoreCase"));
        }
        if (allRequestParams.containsKey("createTime")) {
            channelDefinitionQuery.channelCreateTime(RequestUtil.getDate(allRequestParams, "createTime"));
        }
        if (allRequestParams.containsKey("createTimeAfter")) {
            channelDefinitionQuery.channelCreateTimeAfter(RequestUtil.getDate(allRequestParams, "createTimeAfter"));
        }
        if (allRequestParams.containsKey("createTimeBefore")) {
            channelDefinitionQuery.channelCreateTimeBefore(RequestUtil.getDate(allRequestParams, "createTimeBefore"));
        }
        if (allRequestParams.containsKey("name")) {
            channelDefinitionQuery.channelDefinitionName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            channelDefinitionQuery.channelDefinitionNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("nameLikeIgnoreCase")) {
            channelDefinitionQuery.channelDefinitionNameLikeIgnoreCase(allRequestParams.get("nameLikeIgnoreCase"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            channelDefinitionQuery.channelDefinitionResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            channelDefinitionQuery.channelDefinitionResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            channelDefinitionQuery.channelVersion(Integer.valueOf(allRequestParams.get("version")));
        }
        if (allRequestParams.containsKey("latest")) {
            if (Boolean.parseBoolean(allRequestParams.get("latest"))) {
                channelDefinitionQuery.latestVersion();
            }
        }
        if (allRequestParams.containsKey("onlyInbound")) {
            if (Boolean.parseBoolean(allRequestParams.get("onlyInbound"))) {
                channelDefinitionQuery.onlyInbound();
            }
        }
        if (allRequestParams.containsKey("onlyOutbound")) {
            if (Boolean.parseBoolean(allRequestParams.get("onlyOutbound"))) {
                channelDefinitionQuery.onlyOutbound();
            }
        }
        if (allRequestParams.containsKey("implementation")) {
            channelDefinitionQuery.implementation(allRequestParams.get("implementation"));
        }
        if (allRequestParams.containsKey("deploymentId")) {
            channelDefinitionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("parentDeploymentId")) {
            channelDefinitionQuery.parentDeploymentId(allRequestParams.get("parentDeploymentId"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            channelDefinitionQuery.tenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            channelDefinitionQuery.tenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessChannelDefinitionsWithQuery(channelDefinitionQuery);
        }

        return paginateList(allRequestParams, channelDefinitionQuery, "name", properties, restResponseFactory::createChannelDefinitionResponseList);
    }
}
