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
package org.flowable.admin.app.rest.client;

import org.apache.commons.lang3.StringUtils;
import org.flowable.admin.domain.EndpointType;
import org.flowable.admin.domain.ServerConfig;
import org.flowable.admin.service.engine.JobService;
import org.flowable.admin.service.engine.exception.FlowableServiceException;
import org.flowable.app.service.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * REST controller for managing the current user's account.
 */
@RestController
public class JobClientResource extends AbstractClientResource {

	@Autowired
	protected JobService clientService;

	/**
	 * GET /rest/activiti/jobs/{jobId} -> return job data
	 */
	@RequestMapping(value = "/rest/activiti/jobs/{jobId}", method = RequestMethod.GET, produces = "application/json")
	public JsonNode getJob(@PathVariable String jobId) throws BadRequestException {

		ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
		try {
			return clientService.getJob(serverConfig, jobId);
		} catch (FlowableServiceException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	/**
	 * DELETE /rest/activiti/jobs/{jobId} -> delete job
	 */
	@RequestMapping(value = "/rest/activiti/jobs/{jobId}", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteJob(@PathVariable String jobId) throws BadRequestException {

		ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
		try {
			clientService.deleteJob(serverConfig, jobId);
		} catch (FlowableServiceException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	/**
	 * POST /rest/activiti/jobs/{jobId} -> execute job
	 */
	@RequestMapping(value = "/rest/activiti/jobs/{jobId}", method = RequestMethod.POST, produces = "application/json")
	@ResponseStatus(value = HttpStatus.OK)
	public void executeJob(@PathVariable String jobId) throws BadRequestException {

		ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
		try {
			 clientService.executeJob(serverConfig, jobId);
		} catch (FlowableServiceException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	/**
	 * GET /rest/activiti/jobs/{jobId}/exception-stracktrace -> return job stacktrace
	 */
	@RequestMapping(value = "/rest/activiti/jobs/{jobId}/stacktrace", method = RequestMethod.GET, produces = "text/plain")
	public String getJobStacktrace(@PathVariable String jobId) throws BadRequestException {

		ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
		try {
			String trace =  clientService.getJobStacktrace(serverConfig, jobId);
			if(trace != null) {
				trace = StringUtils.trim(trace);
			}
			return trace;
		} catch (FlowableServiceException e) {
			throw new BadRequestException(e.getMessage());
		}
	}
}
