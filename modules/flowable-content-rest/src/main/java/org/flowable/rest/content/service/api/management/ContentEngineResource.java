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
package org.flowable.rest.content.service.api.management;

import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngines;
import org.flowable.engine.common.EngineInfo;
import org.flowable.engine.common.api.FlowableException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
public class ContentEngineResource {

  @RequestMapping(value = "/content-management/engine", method = RequestMethod.GET, produces = "application/json")
  public ContentEngineInfoResponse getEngineInfo() {
    ContentEngineInfoResponse response = new ContentEngineInfoResponse();

    try {
      ContentEngine contentEngine = ContentEngines.getDefaultContentEngine();
      EngineInfo contentEngineInfo = ContentEngines.getContentEngineInfo(contentEngine.getName());
      if (contentEngineInfo != null) {
        response.setName(contentEngineInfo.getName());
        response.setResourceUrl(contentEngineInfo.getResourceUrl());
        response.setException(contentEngineInfo.getException());
        
      } else {
        response.setName(contentEngine.getName());
      }
      
    } catch (Exception e) {
        throw new FlowableException("Error retrieving content engine info", e);
    }

    response.setVersion(ContentEngine.VERSION);

    return response;
  }
}
