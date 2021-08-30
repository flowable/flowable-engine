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
package org.flowable.cdi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

import org.flowable.cdi.impl.util.FlowableCmmnServices;
import org.flowable.cdi.spi.CmmnEngineLookup;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.common.engine.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * starts / stops the {@link CmmnEngine}.
 *
 * @author Andy Verberne
 * @since 6.6.1
 */
public class FlowableCmmnExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCmmnExtension.class);

    private CmmnEngineLookup cmmnEngineLookup;

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        try {
            LOGGER.info("Initializing flowable-cmmn-cdi.");
            // initialize the cmmn engine
            lookupCmmnEngine(beanManager);
        } catch (Exception e) {
            // interpret cmmn engine initialization problems as definition errors
            event.addDeploymentProblem(e);
        }
    }

    protected CmmnEngine lookupCmmnEngine(BeanManager beanManager) {
        ServiceLoader<CmmnEngineLookup> cmmnEngineServiceLoader = ServiceLoader.load(CmmnEngineLookup.class);
        Iterator<CmmnEngineLookup> serviceIterator = cmmnEngineServiceLoader.iterator();
        List<CmmnEngineLookup> discoveredLookups = new ArrayList<>();
        while (serviceIterator.hasNext()) {
            CmmnEngineLookup serviceInstance = serviceIterator.next();
            discoveredLookups.add(serviceInstance);
        }

        Collections.sort(discoveredLookups, new Comparator<CmmnEngineLookup>() {
            @Override
            public int compare(CmmnEngineLookup o1, CmmnEngineLookup o2) {
                return (-1) * ((Integer) o1.getPrecedence()).compareTo(o2.getPrecedence());
            }
        });

        CmmnEngine cmmnEngine = null;

        for (CmmnEngineLookup cmmnEngineLookup : discoveredLookups) {
            cmmnEngine = cmmnEngineLookup.getCmmnEngine();
            if (cmmnEngine != null) {
                this.cmmnEngineLookup = cmmnEngineLookup;
                LOGGER.debug("CmmnEngineLookup service {} returned cmmn engine.", cmmnEngineLookup.getClass());
                break;
            } else {
                LOGGER.debug("CmmnEngineLookup service {} returned 'null' value.", cmmnEngineLookup.getClass());
            }
        }

        if (cmmnEngineLookup == null) {
            throw new FlowableException(
                    "Could not find an implementation of the " + CmmnEngineLookup.class.getName() + " service returning a non-null cmmnEngine. Giving up.");
        }

        Bean<FlowableCmmnServices> flowableCmmnServicesBean = (Bean<FlowableCmmnServices>) beanManager.getBeans(FlowableCmmnServices.class).stream()
                .findAny()
                .orElseThrow(
                        () -> new IllegalStateException("CDI BeanManager cannot find an instance of requested type " + FlowableCmmnServices.class.getName()));
        FlowableCmmnServices services = (FlowableCmmnServices) beanManager
                .getReference(flowableCmmnServicesBean, FlowableCmmnServices.class, beanManager.createCreationalContext(flowableCmmnServicesBean));
        services.setCmmnEngine(cmmnEngine);

        return cmmnEngine;
    }

    public void beforeShutdown(@Observes BeforeShutdown event) {
        if (cmmnEngineLookup != null) {
            cmmnEngineLookup.ungetCmmnEngine();
            cmmnEngineLookup = null;
        }
        LOGGER.info("Shutting down flowable-cmmn-cdi");
    }
}
