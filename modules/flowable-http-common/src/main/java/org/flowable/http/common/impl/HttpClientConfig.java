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
package org.flowable.http.common.impl;

import java.time.Duration;
import java.util.Objects;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.impl.apache.ApacheHttpComponentsFlowableHttpClient;
import org.flowable.http.common.impl.spring.reactive.SpringWebClientFlowableHttpClient;

/**
 * @author Harsha Teja Kanna
 */
public class HttpClientConfig {

    protected static final boolean isApacheHttpComponentsPresent;
    protected static final boolean isSpringWebClientPresent;
    protected static final boolean isReactorHttpClientPresent;

    static {
        ClassLoader loader = HttpClientConfig.class.getClassLoader();
        boolean httpClientBuilderPresent = false;
        try {
            Class.forName("org.apache.http.impl.client.HttpClientBuilder", false, loader);
            httpClientBuilderPresent = true;
        } catch (ClassNotFoundException e) {
        }

        isApacheHttpComponentsPresent = httpClientBuilderPresent;

        boolean springWebClientBuilderPresent = false;
        try {
            Class.forName("org.springframework.web.reactive.function.client.WebClient", false, loader);
            springWebClientBuilderPresent = true;
        } catch (ClassNotFoundException e) {
        }
        isSpringWebClientPresent = springWebClientBuilderPresent;

        boolean reactorHttpClientPresent = false;
        try {
            Class.forName("reactor.netty.http.client.HttpClient", false, loader);
            reactorHttpClientPresent = true;
        } catch (ClassNotFoundException e) {
        }

        isReactorHttpClientPresent = reactorHttpClientPresent;
    }

    // request settings
    /**
     * The maximum time to wait for connecting to a server.
     * This occurs only when starting the TCP connection.
     * This usually happens if the remote machine does not answer.
     * This means that the server has been shut down, you used the wrong IP/DNS name, wrong port or the network connection to the server is down.
     */
    protected int connectTimeout = 5000;

    /**
     * The maximum time the connection is idle (i.e. no data is received).
     * A socket timeout is dedicated to monitor the continuous incoming data flow.
     * If the data flow is interrupted for the specified timeout the connection is regarded as stalled/broken.
     * Of course this only works with connections where data is received all the time.
     */
    protected int socketTimeout = 5000;
    protected int connectionRequestTimeout = 5000;
    protected int requestRetryLimit = 3;
    // https settings
    protected boolean disableCertVerify;

    protected boolean useSystemProperties = false;

    protected FlowableHttpClient httpClient;

    /**
     * How the Http Task should perform the HTTP requests in case no parallelInSameTransaction is defined in the XML.
     */
    protected boolean defaultParallelInSameTransaction = false;

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getRequestRetryLimit() {
        return requestRetryLimit;
    }

    public void setRequestRetryLimit(int requestRetryLimit) {
        this.requestRetryLimit = requestRetryLimit;
    }

    public boolean isDisableCertVerify() {
        return disableCertVerify;
    }

    public void setDisableCertVerify(boolean disableCertVerify) {
        this.disableCertVerify = disableCertVerify;
    }

    public void setUseSystemProperties(boolean useSystemProperties) {
        this.useSystemProperties = useSystemProperties;
    }

    public boolean isUseSystemProperties() {
        return useSystemProperties;
    }

    public void merge(HttpClientConfig other) {
        if (this.connectTimeout != other.getConnectTimeout()) {
            setConnectTimeout(other.getConnectTimeout());
        }

        if (this.socketTimeout != other.getSocketTimeout()) {
            setSocketTimeout(other.getSocketTimeout());
        }

        if (this.connectionRequestTimeout != other.getConnectionRequestTimeout()) {
            setConnectionRequestTimeout(other.getConnectionRequestTimeout());
        }

        if (this.requestRetryLimit != other.getRequestRetryLimit()) {
            setRequestRetryLimit(other.getRequestRetryLimit());
        }

        if (this.disableCertVerify != other.isDisableCertVerify()) {
            setDisableCertVerify(other.isDisableCertVerify());
        }

        if (this.useSystemProperties != other.isUseSystemProperties()) {
            setUseSystemProperties(other.isUseSystemProperties());
        }

        if (!Objects.equals(this.httpClient, other.getHttpClient())) {
            setHttpClient(other.getHttpClient());
        }

        if (this.defaultParallelInSameTransaction != other.isDefaultParallelInSameTransaction()) {
            setDefaultParallelInSameTransaction(other.isDefaultParallelInSameTransaction());
        }
    }

    public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
        setConnectionRequestTimeout(Math.toIntExact(connectionRequestTimeout.toMillis()));
    }

    public void setConnectTimeout(Duration connectTimeout) {
        setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
    }

    public void setSocketTimeout(Duration socketTimeout) {
        setSocketTimeout(Math.toIntExact(socketTimeout.toMillis()));
    }

    public FlowableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(FlowableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public FlowableHttpClient determineHttpClient() {
        if (httpClient != null) {
            return httpClient;
        } else if (isApacheHttpComponentsPresent) {
            // Backwards compatibility, if apache HTTP Components is present then it has priority
            this.httpClient = new ApacheHttpComponentsFlowableHttpClient(this);
            return this.httpClient;
        } else if (isSpringWebClientPresent && isReactorHttpClientPresent) {
            this.httpClient = new SpringWebClientFlowableHttpClient(this);
            return httpClient;
        } else {
            throw new FlowableException("Failed to determine FlowableHttpClient");
        }
    }

    public boolean isDefaultParallelInSameTransaction() {
        return defaultParallelInSameTransaction;
    }

    public void setDefaultParallelInSameTransaction(boolean defaultParallelInSameTransaction) {
        this.defaultParallelInSameTransaction = defaultParallelInSameTransaction;
    }
}
