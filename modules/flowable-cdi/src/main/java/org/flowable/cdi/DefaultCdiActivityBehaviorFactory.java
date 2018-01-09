package org.flowable.cdi;

import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;

public class DefaultCdiActivityBehaviorFactory extends DefaultActivityBehaviorFactory {

	@Override
	protected String getDefaultCamelBehaviorClassName() {
		return "org.flowable.camel.cdi.impl.CdiCamelBehaviorDefaultImpl";
	}

}
