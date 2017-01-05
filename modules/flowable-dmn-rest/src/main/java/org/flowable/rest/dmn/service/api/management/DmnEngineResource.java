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
package org.flowable.rest.dmn.service.api.management;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.engine.common.EngineInfo;
import org.flowable.engine.common.api.FlowableException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Engine" }, description = "Manage DMN Engine")
public class DmnEngineResource {

  @ApiOperation(value = "Get DMN engine info", tags = {"Engine"})
  @ApiResponses(value = {
      @ApiResponse(code = 200, message =  "Indicates the engine info is returned."),
  })
  @RequestMapping(value = "/dmn-management/engine", method = RequestMethod.GET, produces = "application/json")
  public DmnEngineInfoResponse getEngineInfo() {
    DmnEngineInfoResponse response = new DmnEngineInfoResponse();

    try {
      EngineInfo dmnEngineInfo = DmnEngines.getDmnEngineInfo(DmnEngines.getDefaultDmnEngine().getName());
      if (dmnEngineInfo != null) {
        response.setName(dmnEngineInfo.getName());
        response.setResourceUrl(dmnEngineInfo.getResourceUrl());
        response.setException(dmnEngineInfo.getException());
      }
      
    } catch (Exception e) {
      throw new FlowableException("Error retrieving DMN engine info", e);
    }

    response.setVersion(DmnEngine.VERSION);

    return response;
  }

}
