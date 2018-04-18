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
@SpringBootApplication
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

    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }

}
