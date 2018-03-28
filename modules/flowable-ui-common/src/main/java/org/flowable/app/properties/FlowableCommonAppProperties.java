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
package org.flowable.app.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Common properties for the Flowable UI Apps
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.common.app")
public class FlowableCommonAppProperties {

    /**
     * The static tenant id used for the DefaultTenantProvider. The modeler app uses this to determine under which tenant id to store and publish models.
     * When not provided, empty or only contains whitespace it defaults to the user's tenant id if available otherwise it uses no tenant id.
     */
    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
