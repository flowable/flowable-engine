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
package org.flowable.app.rest.runtime;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.app.model.runtime.ProcessInstanceRepresentation;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.app.service.runtime.FlowableProcessInstanceService;
import org.flowable.form.model.FormModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * REST controller for managing a process instance.
 */
@RestController
public class ProcessInstanceResource {

    @Autowired
    protected FlowableProcessInstanceService processInstanceService;

    @Autowired
    protected Environment environment;

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}", method = RequestMethod.GET, produces = "application/json")
    public ProcessInstanceRepresentation getProcessInstance(@PathVariable String processInstanceId, HttpServletResponse response) {
        return processInstanceService.getProcessInstance(processInstanceId, response);
    }

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}/start-form", method = RequestMethod.GET, produces = "application/json")
    public FormModel getProcessInstanceStartForm(@PathVariable String processInstanceId, HttpServletResponse response) {
        return processInstanceService.getProcessInstanceStartForm(processInstanceId, response);
    }

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteProcessInstance(@PathVariable String processInstanceId) {
        processInstanceService.deleteProcessInstance(processInstanceId);
    }

    @RequestMapping(value = "/rest/models", method = RequestMethod.POST)
    public HttpResponse createModel(@RequestParam String skeleton, @RequestBody String data) {
        String deployApiUrl = environment.getRequiredProperty("modeler.api.url");
        String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
        String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");

        if (!deployApiUrl.endsWith("/")) {
            deployApiUrl = deployApiUrl.concat("/");
        }
        deployApiUrl = deployApiUrl.concat("app/rest/models?skeleton=" + skeleton);

        HttpPost httpPost = new HttpPost(deployApiUrl);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
                Base64.encodeBase64((basicAuthUser + ":" + basicAuthPassword).getBytes(Charset.forName("UTF-8")))));

        StringEntity dataEntity = new StringEntity(data, ContentType.create("application/json", "UTF-8"));
        httpPost.setEntity(dataEntity);

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            clientBuilder.setSSLSocketFactory(sslsf);
        } catch (Exception e) {
//            log.error("Could not configure SSL for http client", e);
            throw new InternalServerErrorException("Could not configure SSL for http client", e);
        }

        CloseableHttpClient client = clientBuilder.build();

        try {
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == org.apache.http.HttpStatus.SC_CREATED) {
                return response;
            } else {
//                logger.error("Invalid deploy result code: {}", response.getStatusLine());
                throw new InternalServerErrorException("Invalid create model result code: " + response.getStatusLine());
            }
        } catch (IOException ioe) {
//            logger.error("Error calling deploy endpoint", ioe);
            throw new InternalServerErrorException("Error calling deploy endpoint: " + ioe.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
//                    logger.warn("Exception while closing http client", e);
                }
            }
        }
    }

}
