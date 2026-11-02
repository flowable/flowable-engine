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
package org.flowable.common.engine.impl.aot;

import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.javassist.util.proxy.RuntimeSupport;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * @author Filip Hrisafov
 */
public class FlowableMyBatisRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // These hints are coming from https://github.com/mybatis/spring-boot-starter/wiki/MyBatisNativeConfiguration.java
        MemberCategory[] memberCategories = MemberCategory.values();
        ReflectionHints reflectionHints = hints.reflection();
        reflectionHints.registerType(Configuration.class, memberCategories);
        reflectionHints.registerType(RawLanguageDriver.class, memberCategories);
        reflectionHints.registerType(XMLLanguageDriver.class, memberCategories);
        reflectionHints.registerType(RuntimeSupport.class, memberCategories);
        reflectionHints.registerType(ProxyFactory.class, memberCategories);
        reflectionHints.registerType(Slf4jImpl.class, memberCategories);
        reflectionHints.registerType(Log.class, memberCategories);
        reflectionHints.registerType(JakartaCommonsLoggingImpl.class, memberCategories);
        reflectionHints.registerType(Log4j2Impl.class, memberCategories);
        reflectionHints.registerType(Jdk14LoggingImpl.class, memberCategories);
        reflectionHints.registerType(StdOutImpl.class, memberCategories);
        reflectionHints.registerType(NoLoggingImpl.class, memberCategories);
        reflectionHints.registerType(SqlSessionFactory.class, memberCategories);
        reflectionHints.registerType(PerpetualCache.class, memberCategories);
        reflectionHints.registerType(FifoCache.class, memberCategories);
        reflectionHints.registerType(LruCache.class, memberCategories);
        reflectionHints.registerType(SoftCache.class, memberCategories);
        reflectionHints.registerType(WeakCache.class, memberCategories);

        ResourceHints resourceHints = hints.resources();
        resourceHints.registerPattern("org/apache/ibatis/builder/xml/*.dtd");
        resourceHints.registerPattern("org/apache/ibatis/builder/xml/*.xsd");

    }
}
