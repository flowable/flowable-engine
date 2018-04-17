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
package org.flowable.form.rest.service.api.repository;

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
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.impl.FormQueryProperty;
import org.flowable.form.rest.FormRestResponseFactory;
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
@Api(tags = { "Form Definitions" }, description = "Manage Form Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class FormDefinitionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", FormQueryProperty.FORM_ID);
        properties.put("key", FormQueryProperty.FORM_DEFINITION_KEY);
        properties.put("category", FormQueryProperty.FORM_CATEGORY);
        properties.put("name", FormQueryProperty.FORM_NAME);
        properties.put("version", FormQueryProperty.FORM_VERSION);
        properties.put("deploymentId", FormQueryProperty.DEPLOYMENT_ID);
        properties.put("tenantId", FormQueryProperty.FORM_TENANT_ID);
    }

    @Autowired
    protected FormRestResponseFactory formRestResponseFactory;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @ApiOperation(value = "List of form definitions",  nickname = "listFormDefinitions", tags = { "Form Definitions" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return form definitions with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return form definitions with a category like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return form definitions not with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return form definitions with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return form definitions with a key like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return form definitions with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return form definitions with a name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return form definitions with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return form definitions a resource name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "version", dataType = "string", value = "Only return form definitions with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "versionGreaterThan", dataType = "string", value = "Only return form definitions with a version greater than the given value.", paramType = "query"),
            @ApiImplicitParam(name = "versionGreaterThanOrEquals", dataType = "string", value = "Only return form definitions with a version greater than or equal to the given value.", paramType = "query"),
            @ApiImplicitParam(name = "versionLowerThan", dataType = "string", value = "Only return form definitions with a version lower than the given value.", paramType = "query"),
            @ApiImplicitParam(name = "versionLowerThanOrEquals", dataType = "string", value = "Only return form definitions with a version lower than or equal to the given value.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return form definitions with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return form definitions with the given tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return form definitions with a tenant id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "string", value = "Only return form definitions without a tenant id.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "If true; only the latest versions will be returned.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "key,category,id,version,name,deploymentId,tenantId", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the form definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format . The status-message contains additional information.")
    })
    @GetMapping(value = "/form-repository/form-definitions", produces = "application/json")
    public DataResponse<FormDefinitionResponse> getForms(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        FormDefinitionQuery formDefinitionQuery = formRepositoryService.createFormDefinitionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            formDefinitionQuery.formCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            formDefinitionQuery.formCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            formDefinitionQuery.formCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            formDefinitionQuery.formDefinitionKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            formDefinitionQuery.formDefinitionKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("name")) {
            formDefinitionQuery.formName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            formDefinitionQuery.formNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            formDefinitionQuery.formResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            formDefinitionQuery.formResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            formDefinitionQuery.formVersion(Integer.valueOf(allRequestParams.get("version")));
        }
        if (allRequestParams.containsKey("versionGreaterThan")) {
            formDefinitionQuery.formVersionGreaterThan(Integer.valueOf(allRequestParams.get("versionGreaterThan")));
        }
        if (allRequestParams.containsKey("versionGreaterThanOrEquals")) {
            formDefinitionQuery.formVersionGreaterThanOrEquals(Integer.valueOf(allRequestParams.get("versionGreaterThanOrEquals")));
        }
        if (allRequestParams.containsKey("versionLowerThan")) {
            formDefinitionQuery.formVersionLowerThan(Integer.valueOf(allRequestParams.get("versionLowerThan")));
        }
        if (allRequestParams.containsKey("versionLowerThanOrEquals")) {
            formDefinitionQuery.formVersionLowerThanOrEquals(Integer.valueOf(allRequestParams.get("versionLowerThanOrEquals")));
        }
        if (allRequestParams.containsKey("deploymentId")) {
            formDefinitionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            formDefinitionQuery.formTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            formDefinitionQuery.formTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        if (allRequestParams.containsKey("withoutTenantId")) {
            Boolean withoutTenantId = Boolean.valueOf(allRequestParams.get("withoutTenantId"));
            if (withoutTenantId) {
                formDefinitionQuery.formWithoutTenantId();
            }
        }
        if (allRequestParams.containsKey("latest")) {
            Boolean latest = Boolean.valueOf(allRequestParams.get("latest"));
            if (latest) {
                formDefinitionQuery.latestVersion();
            }
        }

        return new FormPaginateList(formRestResponseFactory).paginateList(allRequestParams, formDefinitionQuery, "name", properties);
    }
}
