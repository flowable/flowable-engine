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
package org.flowable.app.conf;

import org.flowable.app.properties.FlowableTaskAppProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlowableTaskAppProperties.class)
@ComponentScan(basePackages = {
        "org.flowable.app.conf",
        "org.flowable.app.repository",
        "org.flowable.app.service",
        "org.flowable.app.filter",
        "org.flowable.app.security",
        "org.flowable.app.model.component" })
public class ApplicationConfiguration {

}
