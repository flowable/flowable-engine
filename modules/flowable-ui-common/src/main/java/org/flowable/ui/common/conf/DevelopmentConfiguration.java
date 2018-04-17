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
package org.flowable.ui.common.conf;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Development @Profile specific datasource override
 *
 * @author Yvo Swillens
 */
@Configuration
@Profile({"dev"})
public class DevelopmentConfiguration {

    protected static final String DATASOURCE_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    protected static final String DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/flowable?characterEncoding=UTF-8";
    protected static final String DATASOURCE_USERNAME = "flowable";
    protected static final String DATASOURCE_PASSWORD = "flowable";

    @Bean
    @Primary
    public DataSource developmentDataSource() {
        return DataSourceBuilder
            .create()
            .driverClassName(DATASOURCE_DRIVER_CLASS_NAME)
            .url(DATASOURCE_URL)
            .username(DATASOURCE_USERNAME)
            .password(DATASOURCE_PASSWORD)
            .build();
    }

}