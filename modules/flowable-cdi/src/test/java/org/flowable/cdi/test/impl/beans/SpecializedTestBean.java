package org.flowable.cdi.test.impl.beans;

import javax.enterprise.inject.Specializes;

import org.flowable.cdi.test.impl.util.ProgrammaticBeanLookupTest.TestBean;

@Specializes
public class SpecializedTestBean extends TestBean {

}
