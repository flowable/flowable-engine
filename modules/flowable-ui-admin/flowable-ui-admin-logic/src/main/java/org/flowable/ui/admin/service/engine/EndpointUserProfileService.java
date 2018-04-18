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

import org.apache.http.client.methods.HttpGet;
import org.flowable.ui.admin.properties.FlowableAdminAppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class EndpointUserProfileService extends AbstractEncryptingService {

    @Autowired
    protected FlowableClientService clientUtil;

    public EndpointUserProfileService(FlowableAdminAppProperties properties) {
        super(properties);
    }

    public String getEndpointUserTenantIdUsingEncryptedPassword(String contextRoot, String restRoot,
            String serverAddress, Integer port,
            String userName, String encryptedPassword) {
        String decryptedPassword = decrypt(encryptedPassword);
        return getEndpointUserTenantId(contextRoot, restRoot, serverAddress, port, userName, decryptedPassword);
    }

    public String getEndpointUserTenantId(String contextRoot, String restRoot,
            String serverAddress, Integer port,
            String userName, String password) {
        JsonNode jsonNode = getEndpointUserProfile(contextRoot, restRoot, serverAddress, port, userName, password);
        if (jsonNode.has("tenantId") && !jsonNode.get("tenantId").isNull()) {
            JsonNode tenantIdNode = jsonNode.get("tenantId");
            return tenantIdNode.asText();
        }
        return null;
    }

    public JsonNode getEndpointUserProfile(String contextRoot, String restRoot,
            String serverAddress, Integer port,
            String userName, String password) {

        HttpGet get = new HttpGet(clientUtil.getServerUrl(contextRoot, restRoot, serverAddress, port, "enterprise/profile"));
        return clientUtil.executeRequest(get, userName, password);
    }

}
