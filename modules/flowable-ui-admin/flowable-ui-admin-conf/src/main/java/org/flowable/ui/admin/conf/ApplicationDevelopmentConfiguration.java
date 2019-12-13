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
package org.flowable.ui.admin.conf;

import javax.annotation.PostConstruct;

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.properties.FlowableAdminAppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Development @Profile specific datasource override
 *
 * @author Yvo Swillens
 */
@Profile({"dev"})
@Configuration(proxyBeanMethods = false)
public class ApplicationDevelopmentConfiguration {

    protected static final Integer FLOWABLE_ADMIN_APP_SERVER_CONFIG_PROCESS_PORT = 9999;
    protected static final Integer FLOWABLE_ADMIN_APP_SERVER_CONFIG_CMMN_PORT = 9999;
    protected static final Integer FLOWABLE_ADMIN_APP_SERVER_CONFIG_APP_PORT = 9999;
    protected static final Integer FLOWABLE_ADMIN_APP_SERVER_CONFIG_DMN_PORT = 9999;
    protected static final Integer FLOWABLE_ADMIN_APP_SERVER_CONFIG_FORM_PORT = 9999;
    protected static final Integer FLOWABLE_ADMIN_APP_SERVER_CONFIG_CONTENT_PORT = 9999;

    @Autowired
    private FlowableAdminAppProperties flowableAdminAppProperties;

    @PostConstruct
    public void postConstruct() {
        if (flowableAdminAppProperties.getServerConfig() == null) {
            return;
        }
        if (flowableAdminAppProperties.getServerConfig().get(EndpointType.PROCESS) != null) {
            flowableAdminAppProperties.getServerConfig().get(EndpointType.PROCESS).setPort(FLOWABLE_ADMIN_APP_SERVER_CONFIG_PROCESS_PORT);
        }
        if (flowableAdminAppProperties.getServerConfig().get(EndpointType.CMMN) != null) {
            flowableAdminAppProperties.getServerConfig().get(EndpointType.CMMN).setPort(FLOWABLE_ADMIN_APP_SERVER_CONFIG_CMMN_PORT);
        }
        if (flowableAdminAppProperties.getServerConfig().get(EndpointType.APP) != null) {
            flowableAdminAppProperties.getServerConfig().get(EndpointType.APP).setPort(FLOWABLE_ADMIN_APP_SERVER_CONFIG_APP_PORT);
        }
        if (flowableAdminAppProperties.getServerConfig().get(EndpointType.DMN) != null) {
            flowableAdminAppProperties.getServerConfig().get(EndpointType.DMN).setPort(FLOWABLE_ADMIN_APP_SERVER_CONFIG_DMN_PORT);
        }
        if (flowableAdminAppProperties.getServerConfig().get(EndpointType.FORM) != null) {
            flowableAdminAppProperties.getServerConfig().get(EndpointType.FORM).setPort(FLOWABLE_ADMIN_APP_SERVER_CONFIG_FORM_PORT);
        }
        if (flowableAdminAppProperties.getServerConfig().get(EndpointType.CONTENT) != null) {
            flowableAdminAppProperties.getServerConfig().get(EndpointType.CONTENT).setPort(FLOWABLE_ADMIN_APP_SERVER_CONFIG_CONTENT_PORT);
        }
    }
}