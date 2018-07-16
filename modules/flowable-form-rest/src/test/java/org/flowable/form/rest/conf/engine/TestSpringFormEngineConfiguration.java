package org.flowable.form.rest.conf.engine;

import org.flowable.common.engine.impl.EngineConfigurator;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.idm.engine.configurator.IdmEngineConfigurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestSpringFormEngineConfiguration extends SpringFormEngineConfiguration {

    protected boolean disableIdmEngine;

    public boolean isDisableIdmEngine() {
        return disableIdmEngine;
    }

    public FormEngineConfiguration setDisableIdmEngine(boolean disableIdmEngine) {
        this.disableIdmEngine = disableIdmEngine;
        return this;
    }

    @Override
    protected List<EngineConfigurator> getEngineSpecificEngineConfigurators() {
        if (!disableIdmEngine) {
            List<EngineConfigurator> specificConfigurators = new ArrayList<>();
            if (idmEngineConfigurator != null) {
                specificConfigurators.add(idmEngineConfigurator);
            } else {
                specificConfigurators.add(new IdmEngineConfigurator());
            }
            return specificConfigurators;
        }
        return Collections.emptyList();
    }
}
