package org.flowable.camel.cdi;

import org.flowable.cdi.spi.ProcessEngineLookup;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;

/**
 * Adapted from ProcessEngineLookupForTestsuite. Having this class here avoids dependency on flowable-cdi test jar. 
 * 
 * @author Zach Visagie
 */
public class CamelCdiTestProcessEngineLookup implements ProcessEngineLookup {

    public static ProcessEngine processEngine;

    @Override
    public int getPrecedence() {
        return 1000;
    }

    @Override
    public ProcessEngine getProcessEngine() {
        if (processEngine == null) {
            processEngine = ProcessEngines.getDefaultProcessEngine();
        }
        return processEngine;
    }

    @Override
    public void ungetProcessEngine() {
        // do nothing
    }
	
}
