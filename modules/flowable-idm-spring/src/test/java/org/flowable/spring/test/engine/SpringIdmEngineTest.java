package org.flowable.spring.test.engine;

import static org.junit.Assert.assertNotNull;

import org.flowable.idm.engine.IdmEngines;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Spring process engine base test
 * 
 * @author Henry Yan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:org/flowable/spring/test/engine/springIdmEngine-context.xml")
public class SpringIdmEngineTest {

    @Test
    public void testGetEngineFromCache() {
        assertNotNull(IdmEngines.getDefaultIdmEngine());
        assertNotNull(IdmEngines.getIdmEngine("default"));
    }

}
