package org.flowable.engine.test.db;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;

public class IdGeneratorDataSourceTest extends ResourceFlowableTestCase {

  public IdGeneratorDataSourceTest() {
    super("org/flowable/engine/test/db/IdGeneratorDataSourceTest.activiti.cfg.xml");
  }

  @Deployment
  public void testIdGeneratorDataSource() {
    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < 20; i++) {
      Thread thread = new Thread() {
        public void run() {
          for (int j = 0; j < 5; j++) {
            runtimeService.startProcessInstanceByKey("idGeneratorDataSource");
          }
        }
      };
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
