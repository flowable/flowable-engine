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
package org.flowable.spring.aot;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.type.TypeHandler;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.api.query.Query;
import org.flowable.common.engine.impl.db.ListQueryParameterObject;
import org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl;
import org.flowable.common.engine.impl.persistence.cache.EntityCacheImpl;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.common.engine.impl.persistence.entity.TablePageQueryImpl;
import org.flowable.eventregistry.impl.db.SetChannelDefinitionTypeAndImplementationCustomChange;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.InternalVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.QueryVariableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Josh Long
 * @author Joram Barrez
 */
class FlowableBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private final Logger log = LoggerFactory.getLogger(getClass());

    FlowableBeanFactoryInitializationAotProcessor() {
    }


    private Set<Resource> processResources() {
        return resources("processes/**/*.bpmn20.xml");
    }

    private Set<Resource> flowablePersistenceResources() throws Exception {

        var resources = new HashSet<Resource>();
        resources.addAll(resources("org/flowable/**/*.sql", "org/flowable/**/*.xml", "org/flowable/**/*.txt", "org/flowable/**/*.xsd", "org/flowable/**/*.properties"));
        resources.addAll(processResources());

        for (var e : "xml,yaml,yml".split(","))
            resources.add(new ClassPathResource("flowable-default." + e));

        resources.addAll(from(this.resolver.getResources("META-INF/services/org.flowable.common.engine.impl.EngineConfigurator")));
        resources.addAll(from(this.resolver.getResources("org/flowable/common/engine/impl/de/odysseus/el/misc/LocalStrings")));
        return resources.stream()
                .filter(Resource::exists)
                .collect(Collectors.toSet());
    }

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        return (generationContext, beanFactoryInitializationCode) -> {
            var hints = generationContext.getRuntimeHints();
            beanFactory.getBeanClassLoader();
            try {

                var memberCategories = MemberCategory.values();


                for (var c : Set.of(ProxyFactory.class, XMLLanguageDriver.class,
                        org.apache.ibatis.logging.slf4j.Slf4jImpl.class, EntityCacheImpl.class,
                        RawLanguageDriver.class, org.apache.ibatis.session.Configuration.class, HashSet.class))
                    hints.reflection().registerType(c, memberCategories);

                var types = new Class[]{
                        TypeHandler.class,
                        EntityManager.class,
                        Entity.class,
                        Query.class,
                        VariableType.class,
                        ListQueryParameterObject.class,
                        TablePageQueryImpl.class,
                        SetChannelDefinitionTypeAndImplementationCustomChange.class,
                        ByteArrayRef.class,
                        InternalVariableInstanceQueryImpl.class,
                        QueryVariableValue.class,
                        ExpressionFactoryImpl.class
                };

                var packagesSet = new HashSet<String>();
                packagesSet.add("org.apache.ibatis");
                packagesSet.add("org.flowable");
                packagesSet.addAll(AutoConfigurationPackages.get(beanFactory));
                var packages = packagesSet.toArray(new String[0]);

                for (var t : types) {
                    hints.reflection().registerType(t, memberCategories);
                    var subTypes = FlowableSpringAotUtils.getSubTypesOf(t, packages);
                    for (var s : subTypes) {
                        if (StringUtils.hasText(s)) {
                            hints.reflection().registerType(TypeReference.of(s), memberCategories);
                        }
                    }
                }

                var resources = new HashSet<Resource>();
                resources.addAll(flowablePersistenceResources());
                resources.addAll("""
                        flowable-default.properties
                        flowable-default.xml
                        flowable-default.yaml
                        flowable-default.yml
                           """
                        .stripIndent()
                        .stripLeading()
                        .trim()
                        .lines()
                        .map(l -> l.strip().trim())
                        .filter(l -> !l.isEmpty())
                        .map(ClassPathResource::new)
                        .toList());


                for (var resource : resources) {
                    if (resource.exists()) {
                        hints.resources().registerResource(resource);
                    }
                }


                // here lay dragons; we're going to attempt to proactively register aot hints for beans referenced within a process definition
                var processDefinitions = this.processResources();
                for (var processDefinitionXmlResource : processDefinitions) {
                    Assert.state(processDefinitionXmlResource.exists(), "the process definition file [" + processDefinitionXmlResource.getFilename() +
                            "] does not exist");

                    hints.resources().registerResource(processDefinitionXmlResource);
                    try (var in = processDefinitionXmlResource.getInputStream()) {

                        var bpmnXMLConverter = new BpmnXMLConverter();
                        var bpmnModel = bpmnXMLConverter.convertToBpmnModel(() -> in, false, false);
                        var serviceTasks = bpmnModel.getMainProcess().findFlowElementsOfType(ServiceTask.class);
                        for (var st : serviceTasks) {
                            if (st.getImplementationType().equals(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION)) {
                                var expression = st.getImplementation();
                                var expressionWithoutDelimiters = expression.substring(2);
                                expressionWithoutDelimiters = expressionWithoutDelimiters.substring(0, expressionWithoutDelimiters.length() - 1);
                                var beanName = expressionWithoutDelimiters;
                                try {
                                    var beanDefinition = beanFactory.getBeanDefinition(beanName);
                                    hints.reflection().registerType(TypeReference.of(beanDefinition.getBeanClassName()), MemberCategory.values());

                                    log.debug("registering hint for bean name [" + beanName + "]");
                                }//
                                catch (Throwable throwable) {
                                    log.error("couldn't find bean named [" + beanName + "]");
                                }

                            }
                        }
                    }
                }


            }//
            catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
    }


    private static <T> Set<T> from(T[] t) {
        return new HashSet<>(Arrays.asList(t));
    }

    private static Resource newResourceFor(Resource in) {
        try {
            var marker = "jar!";
            var externalFormOfUrl = in.getURL().toExternalForm();
            if (externalFormOfUrl.contains(marker)) {
                var rest = externalFormOfUrl.substring(externalFormOfUrl.lastIndexOf(marker) + marker.length());
                return new ClassPathResource(rest);
            }//
            else {
                // ugh i think this only works for maven? what about gradle?
                var classesSubstring = "classes/";
                var locationOfClassesInUrl = externalFormOfUrl.indexOf(classesSubstring);
                if (locationOfClassesInUrl != -1) {
                    return new ClassPathResource(externalFormOfUrl.substring(locationOfClassesInUrl + classesSubstring.length()));
                }

            }

            return in;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    Set<Resource> resources(String... patterns) {
        return Stream
                .of(patterns)
                .map(path -> ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + path)
                .flatMap(p -> {
                    try {
                        return Stream.of(this.resolver.getResources(p));
                    }//
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(FlowableBeanFactoryInitializationAotProcessor::newResourceFor)
                .filter(Resource::exists)
                .collect(Collectors.toSet());
    }


}
