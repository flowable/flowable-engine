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
package org.flowable.ui.modeler.rest.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/app")
public class StencilSetResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(StencilSetResource.class);

    @Autowired
    protected ObjectMapper objectMapper;

    @RequestMapping(value = "/rest/stencil-sets/editor", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getStencilSetForEditor(String i18n) {
        try {
            String path = "i18n" + File.separator + "stencilset" + File.separator + "bpmn" + File.separator;
            return getJsonNode(i18n, path);
        } catch (Exception e) {
            LOGGER.error("Error reading bpmn stencil set json", e);
            throw new InternalServerErrorException("Error reading bpmn stencil set json");
        }
    }

    @RequestMapping(value = "/rest/stencil-sets/cmmneditor", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getCmmnStencilSetForEditor(String i18n) {
        try {
            String path = "i18n" + File.separator + "stencilset" + File.separator + "cmmn" + File.separator;
            return getJsonNode(i18n, path);
        } catch (Exception e) {
            LOGGER.error("Error reading bpmn stencil set json", e);
            throw new InternalServerErrorException("Error reading bpmn stencil set json");
        }
    }

    private JsonNode getJsonNode(String i18n, String path) throws IOException {
        JsonNode stencilNode;
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(
                path + StringUtils.trimToEmpty(i18n) + ".json");
        if (in != null) {
            stencilNode = objectMapper.readTree(in);
        } else {
            stencilNode = objectMapper.readTree(this.getClass().getClassLoader().getResourceAsStream(
                    path + "en.json"));
        }
        return stencilNode;
    }
}
