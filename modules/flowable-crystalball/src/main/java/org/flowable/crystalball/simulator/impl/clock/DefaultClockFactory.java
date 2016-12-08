package org.flowable.crystalball.simulator.impl.clock;

import org.flowable.engine.common.impl.util.DefaultClockImpl;
import org.flowable.engine.common.runtime.Clock;
import org.springframework.beans.factory.FactoryBean;

/**
 * This class provides factory for default clocks
 * 
 * @author martin.grofcik
 */
public class DefaultClockFactory implements FactoryBean<Clock> {
  @Override
  public Clock getObject() throws RuntimeException {
    return new DefaultClockImpl();
  }

  @Override
  public Class<?> getObjectType() {
    return DefaultClockImpl.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
