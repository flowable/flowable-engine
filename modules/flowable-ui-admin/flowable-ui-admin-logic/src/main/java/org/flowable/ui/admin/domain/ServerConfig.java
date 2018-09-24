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
package org.flowable.ui.admin.domain;

import java.io.Serializable;

public class ServerConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The id of the server config.
     */
    protected String id;

    /**
     * The name of the endpoint.
     */
    protected String name;

    /**
     * The description for the endpoint.
     */
    protected String description;

    /**
     * The server host for the endpoint.
     */
    protected String serverAddress;

    /**
     * The port for the endpoint.
     */
    protected Integer port;

    /**
     * The context root under which the application where the rest endpoint is running on.
     */
    protected String contextRoot;

    /**
     * The path for accessing the endpoint.
     */
    protected String restRoot;

    /**
     * The username that needs to be used when accessing the endpoint.
     */
    protected String userName;

    /**
     * The password that needs to be used when accessing the endpoint.
     */
    protected String password;

    /**
     * The code of the endpoint. Setting this property via configuration parameters has no effect.
     */
    protected Integer endpointType;

    /**
     * The tenant id of the endpoint.
     */
    protected String tenantId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public String getRestRoot() {
        return restRoot;
    }

    public void setRestRoot(String restRoot) {
        this.restRoot = restRoot;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(Integer endpointType) {
        this.endpointType = endpointType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServerConfig config = (ServerConfig) o;

        return id.equals(config.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ServerConfig [id=" + id + ", name=" + name + ", description=" + description + ", serverAddress="
                + serverAddress + ", port=" + port + ", contextRoot=" + contextRoot + ", restRoot=" + restRoot + ", userName="
                + userName + ", password=" + password + "]";
    }
}
