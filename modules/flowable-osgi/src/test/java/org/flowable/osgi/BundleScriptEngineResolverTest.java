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
package org.flowable.osgi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.flowable.osgi.Extender.BundleScriptEngineResolver;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test processing of service provider configuration file.
 */
public class BundleScriptEngineResolverTest {

    @Test
    public void testResolveScriptEngine() throws Exception {
        ScriptEngineFactory factory = mock(ScriptEngineFactoryMock.class);
        Bundle bundle = mock(Bundle.class);
        when(bundle.loadClass(eq("org.flowable.ScriptEngineFactoryMock"))).thenAnswer(answer -> factory.getClass());
        URL configFile = getClass().getClassLoader().getResource("META-INF/services/javax.script.ScriptEngineFactory");
        BundleScriptEngineResolver resolver = new BundleScriptEngineResolver(bundle, configFile);
        ScriptEngine resolvedEngine = resolver.resolveScriptEngine("mockengine");
        assertNotNull(resolvedEngine);
        assertEquals("mockengine", resolvedEngine.get("name"));
    }

    /**
     * Script engine factory providing mock script engine.
     */
    static abstract class ScriptEngineFactoryMock implements ScriptEngineFactory {

        @Override
        public List<String> getNames() {
            return List.of("mockengine");
        }

        @Override
        public ScriptEngine getScriptEngine() {
            ScriptEngine engine = mock(ScriptEngine.class);
            when(engine.get(eq("name"))).thenReturn("mockengine");
            return engine;
        }

    }

}
