package org.flowable.common.engine.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class ServiceConfiguratorTest {

    @Test
    public void testNoServiceConfigurators() {
        AtomicBoolean beforeInit = new AtomicBoolean(false);
        AtomicBoolean afterInit = new AtomicBoolean(false);

        TestServiceConfiguration serviceConfig = new TestServiceConfiguration("test");
        serviceConfig.init();

        assertThat(beforeInit.get()).isFalse();
        assertThat(afterInit.get()).isFalse();
    }

    @Test
    public void testSetServiceConfigurators() {
        AtomicBoolean beforeInit = new AtomicBoolean(false);
        AtomicBoolean afterInit = new AtomicBoolean(false);

        TestServiceConfiguration serviceConfig = new TestServiceConfiguration("test");
        serviceConfig.setConfigurators(List.of(new ServiceConfigurator<>() {

            @Override
            public void beforeInit(TestServiceConfiguration service) {
                beforeInit.set(true);
            }

            @Override
            public void afterInit(TestServiceConfiguration service) {
                afterInit.set(true);
            }
        }));

        serviceConfig.init();

        assertThat(beforeInit.get()).isTrue();
        assertThat(afterInit.get()).isTrue();
    }

    @Test
    public void testAddServiceConfigurators() {
        AtomicBoolean beforeInit = new AtomicBoolean(false);
        AtomicBoolean afterInit = new AtomicBoolean(false);

        TestServiceConfiguration serviceConfig = new TestServiceConfiguration("test");
        serviceConfig.addConfigurator(new ServiceConfigurator<>() {

            @Override
            public void beforeInit(TestServiceConfiguration service) {
                beforeInit.set(true);
            }

            @Override
            public void afterInit(TestServiceConfiguration service) {
                afterInit.set(true);
            }
        });

        serviceConfig.init();
        assertThat(beforeInit.get()).isTrue();
        assertThat(afterInit.get()).isTrue();
    }

    @Test
    public void testOrderServiceConfigurators() {

        AtomicBoolean beforeInit = new AtomicBoolean(false);
        AtomicBoolean afterInit = new AtomicBoolean(false);

        TestServiceConfiguration serviceConfig = new TestServiceConfiguration("test");

        serviceConfig.addConfigurator(new ServiceConfigurator<>() {

            @Override
            public void beforeInit(TestServiceConfiguration service) {
                beforeInit.set(true);
            }

            @Override
            public void afterInit(TestServiceConfiguration service) {
                afterInit.set(true);
            }

            @Override
            public int getPriority() {
                return 2;
            }
        });

        serviceConfig.addConfigurator(new ServiceConfigurator<>() {

            @Override
            public void beforeInit(TestServiceConfiguration service) {
                beforeInit.set(false);
            }

            @Override
            public void afterInit(TestServiceConfiguration service) {
                afterInit.set(false);
            }

            @Override
            public int getPriority() {
                return 1;
            }
        });

        serviceConfig.init();
        assertThat(beforeInit.get()).isTrue();
        assertThat(afterInit.get()).isTrue();
    }

    private static class TestServiceConfiguration extends AbstractServiceConfiguration<TestServiceConfiguration> {

        public TestServiceConfiguration(String engineName) {
            super(engineName);
        }

        @Override
        protected TestServiceConfiguration getService() {
            return this;
        }

        public void init() {
            configuratorsBeforeInit();
            configuratorsAfterInit();
        }

    }
}
