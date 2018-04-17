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
package org.flowable.ui.admin.service.engine;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class CmmnJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnJobService.class);

    @Autowired
    protected FlowableClientService clientUtil;

    public JsonNode listJobs(ServerConfig serverConfig, Map<String, String[]> parameterMap) {
        String jobType = null;
        String[] jobTypeParameter = parameterMap.get("jobType");
        if (jobTypeParameter != null && jobTypeParameter.length > 0) {
            jobType = jobTypeParameter[0];
        }

        String jobUrl = getJobUrl(jobType);
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(jobUrl);

        } catch (Exception e) {
            LOGGER.error("Error building uri", e);
            throw new FlowableServiceException("Error building uri", e);
        }

        for (String name : parameterMap.keySet()) {
            if (!"jobType".equals(name)) {
                builder.addParameter(name, parameterMap.get(name)[0]);
            }
        }
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder.toString()));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getJob(ServerConfig serverConfig, String jobId, String jobType) {
        String jobUrl = getJobUrl(jobType);
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, jobUrl + jobId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public String getJobStacktrace(ServerConfig serverConfig, String jobId, String jobType) {
        String jobUrl = getJobUrl(jobType);
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, jobUrl + jobId + "/exception-stacktrace"));
        return clientUtil.executeRequestAsString(get, serverConfig, HttpStatus.SC_OK);
    }

    public void executeJob(ServerConfig serverConfig, String jobId) {
        HttpPost post = clientUtil.createPost("management/jobs/" + jobId, serverConfig);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("action", "execute");
        post.setEntity(clientUtil.createStringEntity(node));

        clientUtil.executeRequestNoResponseBody(post, serverConfig, HttpStatus.SC_NO_CONTENT);
    }

    public void moveJob(ServerConfig serverConfig, String jobId, String jobType) {
        String jobUrl = getJobUrl(jobType);
        HttpPost post = clientUtil.createPost(jobUrl + jobId, serverConfig);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("action", "move");
        post.setEntity(clientUtil.createStringEntity(node));

        clientUtil.executeRequestNoResponseBody(post, serverConfig, HttpStatus.SC_NO_CONTENT);
    }

    public void deleteJob(ServerConfig serverConfig, String jobId, String jobType) {
        String jobUrl = getJobUrl(jobType);
        HttpDelete post = new HttpDelete(clientUtil.getServerUrl(serverConfig, jobUrl + jobId));
        clientUtil.executeRequestNoResponseBody(post, serverConfig, HttpStatus.SC_NO_CONTENT);
    }

    protected String getJobUrl(String jobType) {
        String jobUrl = null;
        if ("timerJob".equals(jobType)) {
            jobUrl = "cmmn-management/timer-jobs/";
        } else if ("suspendedJob".equals(jobType)) {
            jobUrl = "cmmn-management/suspended-jobs/";
        } else if ("deadletterJob".equals(jobType)) {
            jobUrl = "cmmn-management/deadletter-jobs/";
        } else {
            jobUrl = "cmmn-management/jobs/";
        }

        return jobUrl;
    }
}
