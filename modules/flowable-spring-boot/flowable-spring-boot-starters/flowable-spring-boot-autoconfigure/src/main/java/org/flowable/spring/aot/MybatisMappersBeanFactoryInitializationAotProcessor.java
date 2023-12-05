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
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Discovers any {@literal  mappings.xml} and reads them in to then register the
 * referenced {@literal .xml} files as resource hints.
 *
 * @author Josh Long
 */
class MybatisMappersBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	MybatisMappersBeanFactoryInitializationAotProcessor() {
	}

	private Set<Resource> persistenceResources(String rootPackage) throws Exception {
		var folderFromPackage = FlowableSpringAotUtils.packageToPath(rootPackage);
		var patterns = Stream//
			.of(folderFromPackage + "/**/mappings.xml")//
			.map(path -> ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + path)//
			.flatMap(p -> {
				try {
					return Stream.of(this.resolver.getResources(p));
				} //
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			})//
			.map(FlowableSpringAotUtils::newResourceFor)
			.toList();

		var resources = new HashSet<Resource>();
		for (var p : patterns) {
			var mappers = mappers(p);
			resources.add(p);
			resources.addAll(mappers);
		}
		return resources.stream().filter(Resource::exists).collect(Collectors.toSet());
	}

	protected List<String> getPackagesToScan (BeanFactory b){
		return  AutoConfigurationPackages.get(b) ;
	}

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		try {
			var packages =  getPackagesToScan(beanFactory);
			var resources = new HashSet<Resource>();
			for (var pkg : packages) {
				resources.addAll(persistenceResources(pkg));
			}
			return (generationContext, beanFactoryInitializationCode) -> {
				for (var r : resources)
					if (r.exists())
						generationContext.getRuntimeHints().resources().registerResource(r);
			};
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Set<Resource> mappers(Resource mapping) throws Exception {
		var resources = new HashSet<Resource>();
		try (var in = new InputStreamReader(mapping.getInputStream())) {
			var xml = FileCopyUtils.copyToString(in);
			resources.addAll(mapperResources(xml));
		}
		resources.add(mapping);
		return resources;

	}

	private Set<Resource> mapperResources(String xml) {
		try {
			var set = new HashSet<Resource>();
			var dbf = DocumentBuilderFactory.newInstance();
			var db = dbf.newDocumentBuilder();
			var is = new InputSource(new StringReader(xml));
			var doc = db.parse(is);
			var mappersElement = (Element) doc.getElementsByTagName("mappers").item(0);
			var mapperList = mappersElement.getElementsByTagName("mapper");
			for (var i = 0; i < mapperList.getLength(); i++) {
				var mapperElement = (Element) mapperList.item(i);
				var resourceValue = mapperElement.getAttribute("resource");
				set.add(new ClassPathResource(resourceValue));
			}
			return set;
		} //
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}

	}

}
