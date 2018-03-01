package org.flowable.test.spring.boot;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.FormEngineAutoConfiguration;
import org.flowable.spring.boot.FormEngineServicesAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

public class FormEngineAutoConfigurationTest {

    @Test
    public void standaloneFormEngineWithBasicDataSource() {
        AnnotationConfigApplicationContext context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FormEngineServicesAutoConfiguration.class,
            FormEngineAutoConfiguration.class
        );

        FormEngine formEngine = context.getBean(FormEngine.class);
        assertThat(formEngine).as("Form engine").isNotNull();
        assertThat(context.getBean(FormService.class)).as("Form service")
            .isEqualTo(formEngine.getFormService());

        assertThat(context.getBean(FormRepositoryService.class)).as("Form repository service")
            .isEqualTo(formEngine.getFormRepositoryService());

        assertThat(context.getBean(FormManagementService.class)).as("Form management service")
            .isEqualTo(formEngine.getFormManagementService());

        assertThat(context.getBean(FormEngineConfiguration.class)).as("Form engine configuration")
            .isEqualTo(formEngine.getFormEngineConfiguration());
    }

    @Test
    public void formEngineWithBasicDataSourceAndProcessEngine() {
        AnnotationConfigApplicationContext context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FormEngineAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            FormEngineServicesAutoConfiguration.class
        );

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        assertThat(processEngine).as("Process engine").isNotNull();
        FormEngineConfigurationApi formProcessConfigurationApi = formEngine(processEngine);

        FormEngineConfigurationApi formEngine = context.getBean(FormEngineConfigurationApi.class);
        assertThat(formEngine).isEqualTo(formProcessConfigurationApi);
        assertThat(formEngine).as("Form engine").isNotNull();
        assertThat(context.getBean(FormService.class)).as("Form service")
            .isEqualTo(formEngine.getFormService());

        FormRepositoryService formRepositoryService = context.getBean(FormRepositoryService.class);
        assertThat(formRepositoryService).as("Form repository service")
            .isEqualTo(formEngine.getFormRepositoryService());

        assertThat(context.getBean(FormManagementService.class)).as("Form management service")
            .isEqualTo(formEngine.getFormManagementService());
    }


    private AnnotationConfigApplicationContext context(Class<?>... clazz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clazz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }

    private static FormEngineConfigurationApi formEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getFormEngineConfiguration(processEngineConfiguration);
    }
}
