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

import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Joram Barrez
 */
@RestController
@Api(tags = { "Properties" }, authorizations = { @Authorization(value = "basicAuth") })
public class EnginePropertiesResource {

    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @Autowired
    protected ManagementService managementService;

    protected void validateAccessToProperties() {
        if (restApiInterceptor != null) {
            restApiInterceptor.accessEngineProperties();
        }
    }
    
    @ApiOperation(value = "Get all engine properties", tags = { "EngineProperties" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that engine properties were found and returned."),
    })
    @GetMapping(value = "/management/engine-properties", produces = "application/json")
    public Map<String, String> getEngineProperties() {
        validateAccessToProperties();
        return managementService.getProperties();
    }

    @ApiOperation(value = "Delete an engine property", tags = { "EngineProperties" }, code = 204)
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Indicates the property was found and has been deleted. Response-body is intentionally empty."),
        @ApiResponse(code = 404, message = "Indicates the requested property was not found.")
    })
    @DeleteMapping(value = "/management/engine-properties/{engineProperty}", produces = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEngineProperty(@ApiParam(name = "engineProperty") @PathVariable String engineProperty) {
        validateAccessToProperties();

        validatePropertyExists(engineProperty);

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getPropertyEntityManager(commandContext).delete(engineProperty);
            return null;
        });
    }

    protected void validatePropertyExists(String engineProperty) {
        Map<String, String> properties = managementService.getProperties();
        if (!properties.containsKey(engineProperty)) {
            throw new FlowableObjectNotFoundException("Engine property " + engineProperty + " does not exist");
        }
    }

    @ApiOperation(value = "Create a new engine property", tags = { "EngineProperties" }, code = 201)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Indicates the property is created"),
        @ApiResponse(code = 409, message = "Indicates the property already exists")
    })
    @PostMapping(value = "/management/engine-properties", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public void createEngineProperty(@RequestBody PropertyRequestBody propertyRequestBody) {
        validateAccessToProperties();

        Map<String, String> properties = managementService.getProperties();
        String propertyName = propertyRequestBody.getName();
        if (properties.containsKey(propertyName)) {
            throw new FlowableConflictException("Engine property " + propertyName + " already exists");
        }

        managementService.executeCommand(commandContext -> {
            PropertyEntityManager propertyEntityManager = CommandContextUtil.getPropertyEntityManager(commandContext);
            PropertyEntity propertyEntity = propertyEntityManager.create();
            propertyEntity.setName(propertyName);
            propertyEntity.setValue(propertyRequestBody.getValue());
            propertyEntityManager.insert(propertyEntity);
            return null;
        });
    }

    @ApiOperation(value = "Update an engine property", tags = { "EngineProperties" })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indicates the property is updated"),
        @ApiResponse(code = 404, message = "Indicates the property is not found")
    })
    @PutMapping(value = "/management/engine-properties/{engineProperty}", produces = "application/json")
    public void updateEngineProperty(@ApiParam(name = "engineProperty") @PathVariable String engineProperty, @RequestBody PropertyRequestBody propertyRequestBody) {
        validateAccessToProperties();
        validatePropertyExists(engineProperty);

        managementService.executeCommand(commandContext -> {
            PropertyEntityManager propertyEntityManager = CommandContextUtil.getPropertyEntityManager(commandContext);
            PropertyEntity propertyEntity = propertyEntityManager.findById(engineProperty);
            propertyEntity.setValue(propertyRequestBody.getValue());
            propertyEntityManager.update(propertyEntity);
            return null;
        });
    }

    public static class PropertyRequestBody {

        protected String name;
        protected String value;

        public PropertyRequestBody() {
        }

        public PropertyRequestBody(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
    }

}
