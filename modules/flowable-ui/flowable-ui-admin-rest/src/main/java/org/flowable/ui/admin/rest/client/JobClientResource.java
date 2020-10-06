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

import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.JobService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * REST controller for managing the current user's account.
 */
@RestController
public class JobClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobClientResource.class);

    @Autowired
    protected JobService clientService;

    /**
     * GET /rest/admin/jobs/{jobId} -> return job data
     */
    @GetMapping(value = "/rest/admin/jobs/{jobId}", produces = "application/json")
    public JsonNode getJob(@PathVariable String jobId, HttpServletRequest request) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        String jobType = request.getParameter("jobType");
        try {
            return clientService.getJob(serverConfig, jobId, jobType);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting job {}", jobId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * DELETE /rest/admin/jobs/{jobId} -> delete job
     */
    @DeleteMapping(value = "/rest/admin/jobs/{jobId}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteJob(@PathVariable String jobId, HttpServletRequest request) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        String jobType = request.getParameter("jobType");
        try {
            clientService.deleteJob(serverConfig, jobId, jobType);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error deleting job {}", jobId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * POST /rest/admin/jobs/{jobId} -> execute job
     */
    @PostMapping(value = "/rest/admin/jobs/{jobId}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public void executeJob(@PathVariable String jobId) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.executeJob(serverConfig, jobId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error executing job {}", jobId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * POST /rest/admin/move-jobs/{jobId} -> move job
     */
    @PostMapping(value = "/rest/admin/move-jobs/{jobId}", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public void moveJob(@PathVariable String jobId, HttpServletRequest request) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        String jobType = request.getParameter("jobType");
        try {
            clientService.moveJob(serverConfig, jobId, jobType);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error executing job {}", jobId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * GET /rest/admin/jobs/{jobId}/exception-stacktrace -> return job stacktrace
     */
    @GetMapping(value = "/rest/admin/jobs/{jobId}/stacktrace", produces = "text/plain")
    public String getJobStacktrace(@PathVariable String jobId, HttpServletRequest request) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        String jobType = request.getParameter("jobType");
        try {
            String trace = clientService.getJobStacktrace(serverConfig, jobId, jobType);
            if (trace != null) {
                trace = StringUtils.trim(trace);
            }
            return trace;
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting job stacktrace {}", jobId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
