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
package org.flowable.spring.boot;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Flowable http properties for use in http tasks
 *
 * @author Valentin Rentschler
 */
@ConfigurationProperties(prefix = "flowable.http")
public class FlowableHttpProperties {

    /**
     * Whether to use system properties (e.g. http.proxyPort).
     */
    protected boolean useSystemProperties = false;

    /**
     * Connect timeout for the http client
     */
    protected Duration connectTimeout = Duration.ofMillis(5000);
    /**
     * Socket timeout for the http client
     */
    protected Duration socketTimeout = Duration.ofMillis(5000);
    /**
     * Connection Request Timeout for the http client
     */
    protected Duration connectionRequestTimeout = Duration.ofMillis(5000);
    /**
     * Request retry limit for the http client
     */
    protected int requestRetryLimit = 3;
    /**
     * Whether to disable certificate validation for the http client
     */
    protected boolean disableCertVerify = false;

    public boolean isUseSystemProperties() {
        return useSystemProperties;
    }

    public void setUseSystemProperties(boolean useSystemProperties) {
        this.useSystemProperties = useSystemProperties;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Duration getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
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
}
