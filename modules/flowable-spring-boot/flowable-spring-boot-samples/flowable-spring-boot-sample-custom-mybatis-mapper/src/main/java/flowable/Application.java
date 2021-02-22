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
package flowable;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import flowable.mappers.CustomMybatisMapper;

/**
 * @author Dominik Bartos
 */
@SpringBootApplication(proxyBeanMethods = false)
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @Bean
    CommandLineRunner customMybatisMapper(final ManagementService managementService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                String processDefinitionId = managementService.executeCustomSql(new AbstractCustomSqlExecution<CustomMybatisMapper, String>(CustomMybatisMapper.class) {
                    @Override
                    public String execute(CustomMybatisMapper customMybatisMapper) {
                        return customMybatisMapper.loadProcessDefinitionIdByKey("waiter");
                    }
                });

                LOGGER.info("Process definition id = {}", processDefinitionId);
            }
        };
    }

    @Bean
    CommandLineRunner customMybatisXmlMapper(final ManagementService managementService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                String processDefinitionDeploymentId = managementService.executeCommand(new Command<String>() {
                    @Override
                    public String execute(CommandContext commandContext) {
                        return (String) CommandContextUtil.getDbSqlSession()
                                .selectOne("selectProcessDefinitionDeploymentIdByKey", "waiter");
                    }
                });

                LOGGER.info("Process definition deployment id = {}", processDefinitionDeploymentId);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
