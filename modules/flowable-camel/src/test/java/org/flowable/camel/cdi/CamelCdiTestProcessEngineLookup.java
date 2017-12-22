package org.flowable.camel.cdi;

import org.flowable.cdi.impl.LocalProcessEngineLookup;

public class CamelCdiTestProcessEngineLookup extends LocalProcessEngineLookup {

	public CamelCdiTestProcessEngineLookup() {
		processEngineName = "cdiCamelEngine";
	}
	@Override
	public int getPrecedence() {
		return 1000;
	}
	
	
}
