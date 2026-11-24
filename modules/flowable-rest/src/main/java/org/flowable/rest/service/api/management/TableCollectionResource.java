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

package org.flowable.rest.service.api.management;

import java.util.List;

import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Frederik Heremans
 */
@RestController
@Conditional(TableManagementEnabledCondition.class)
@Api(tags = { "Database tables" }, authorizations = { @Authorization(value = "basicAuth") })
public class TableCollectionResource implements InitializingBean {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TableCollectionResource.class);

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @Override
    public void afterPropertiesSet() {
        LOGGER.warn("The database table REST resources (management/tables) are enabled through '{}'. "
                + "They expose the raw content of the Flowable database tables and should only be enabled when needed, with access restricted to administrators.",
                TableManagementEnabledCondition.PROPERTY_NAME);
    }

    @ApiOperation(value = " List tables", tags = { "Database tables" }, nickname = "listTables")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the request was successful.")
    })
    @GetMapping(value = "/management/tables", produces = "application/json")
    public List<TableResponse> getTables() {
        if (restApiInterceptor != null) {
            restApiInterceptor.accessTableInfo();
        }
        return restResponseFactory.createTableResponseList(managementService.getTableCount());
    }
}
