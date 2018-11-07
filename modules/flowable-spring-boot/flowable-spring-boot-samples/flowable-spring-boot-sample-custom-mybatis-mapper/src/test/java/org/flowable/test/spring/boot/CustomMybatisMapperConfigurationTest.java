package org.flowable.test.spring.boot;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import flowable.Application;
import flowable.mappers.CustomMybatisMapper;

/**
 * @author Dominik Bartos
 */
public class CustomMybatisMapperConfigurationTest {

    @Test
    public void executeCustomMybatisMapperQuery() throws Exception {
        AnnotationConfigApplicationContext applicationContext = this.context(Application.class);
        ManagementService managementService = applicationContext.getBean(ManagementService.class);
        String processDefinitionId = managementService.executeCustomSql(new AbstractCustomSqlExecution<CustomMybatisMapper, String>(CustomMybatisMapper.class) {
            @Override
            public String execute(CustomMybatisMapper customMybatisMapper) {
                return customMybatisMapper.loadProcessDefinitionIdByKey("waiter");
            }
        });
        Assert.assertNotNull("the processDefinitionId should not be null!", processDefinitionId);
    }

    @Test
    public void executeCustomMybatisXmlQuery() throws Exception {
        AnnotationConfigApplicationContext applicationContext = this.context(Application.class);
        ManagementService managementService = applicationContext.getBean(ManagementService.class);
        String processDefinitionDeploymentId = managementService.executeCommand(new Command<String>() {
            @Override
            public String execute(CommandContext commandContext) {
                return (String) CommandContextUtil.getDbSqlSession(commandContext).selectOne("selectProcessDefinitionDeploymentIdByKey", "waiter");
            }
        });
        Assert.assertNotNull("the processDefinitionDeploymentId should not be null!", processDefinitionDeploymentId);
    }

    private AnnotationConfigApplicationContext context(Class<?>... clzz) throws Exception {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clzz);

        URL propertiesUrl = this.getClass().getClassLoader().getResource("config/application.properties");
        File springBootPropertiesFile = new File(propertiesUrl.toURI());
        Properties springBootProperties = new Properties();
        springBootProperties.load(new FileInputStream(springBootPropertiesFile));

        annotationConfigApplicationContext
                .getEnvironment()
                .getPropertySources()
                .addFirst(new PropertiesPropertySource("testProperties", springBootProperties));

        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }
}
