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

import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.impl.FormQueryProperty;
import org.flowable.rest.api.DataResponse;
import org.flowable.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
public class FormCollectionResource {

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

  @RequestMapping(value = "/form-repository/forms", method = RequestMethod.GET, produces = "application/json")
  public DataResponse getForms(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
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

    if (allRequestParams.containsKey("latest")) {
      Boolean latest = Boolean.valueOf(allRequestParams.get("latest"));
      if (latest != null && latest) {
        formDefinitionQuery.latestVersion();
      }
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

    return new FormPaginateList(formRestResponseFactory).paginateList(allRequestParams, formDefinitionQuery, "name", properties);
  }
}
