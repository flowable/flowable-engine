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
package org.flowable.cmmn.engine;

/**
 * @author Harsha Teja Kanna
 */
public class HttpClientConfig {

    // request settings
    protected int connectTimeout = 5000;
    protected int socketTimeout = 5000;
    protected int connectionRequestTimeout = 5000;
    protected int requestRetryLimit = 3;
    // https settings
    protected boolean disableCertVerify;

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
    }
}
