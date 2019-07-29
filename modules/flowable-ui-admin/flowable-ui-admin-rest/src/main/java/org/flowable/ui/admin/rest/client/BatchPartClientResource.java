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
package org.flowable.ui.admin.rest.client;

import javax.servlet.http.HttpServletRequest;

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.BatchService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * REST controller for managing the batch.
 */
@RestController
@RequestMapping("/app")
public class BatchPartClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPartClientResource.class);

    @Autowired
    protected BatchService clientService;

    /**
     * GET /rest/admin/batch-parts/{batchPartId} -> return batch part data
     */
    @RequestMapping(value = "/rest/admin/batch-parts/{batchPartId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getBatchPart(@PathVariable String batchPartId, HttpServletRequest request) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getBatchPart(serverConfig, batchPartId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting batch part {}", batchPartId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * GET /rest/admin/batch-parts/{batchPartId}/batch-part-document
     */
    @RequestMapping(value = "/rest/admin/batch-parts/{batchPartId}/batch-part-document", method = RequestMethod.GET, produces = "text/plain")
    public String getBatchPartDocument(@PathVariable String batchPartId, HttpServletRequest request) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getBatchPartDocument(serverConfig, batchPartId);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting batch part document {}", batchPartId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
