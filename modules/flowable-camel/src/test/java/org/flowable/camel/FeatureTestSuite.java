package org.flowable.camel;

import org.flowable.camel.cdi.named.CdiCustomContextTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  AsyncPingTest.class,
  CdiCustomContextTest.class,
})

public class FeatureTestSuite {
  // the class remains empty,
  // used only as a holder for the above annotations
}