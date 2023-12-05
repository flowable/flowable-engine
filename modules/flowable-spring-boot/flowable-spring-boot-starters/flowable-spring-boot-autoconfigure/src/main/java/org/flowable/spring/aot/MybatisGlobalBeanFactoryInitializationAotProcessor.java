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
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Property;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.NullCacheKey;
import org.apache.ibatis.cache.decorators.BlockingCache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.decorators.TransactionalCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.javassist.util.proxy.RuntimeSupport;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdbc.BaseJdbcLogger;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.logging.jdbc.PreparedStatementLogger;
import org.apache.ibatis.logging.jdbc.ResultSetLogger;
import org.apache.ibatis.logging.jdbc.StatementLogger;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2AbstractLoggerImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.log4j2.Log4j2LoggerImpl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.slf4j.SLF4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Register invariant hints for Mybatis that are presumed the same for all applications
 *
 * @author Josh Long
 */
class MybatisGlobalBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final Logger log = LoggerFactory.getLogger(getClass());

	MybatisGlobalBeanFactoryInitializationAotProcessor() {
	}

	private void registerProxies(RuntimeHints hints) {
		var proxies = Set.of(Set.of(Connection.class.getName()), Set.of(SqlSession.class.getName()),
				Set.of(PreparedStatement.class.getName(), CallableStatement.class.getName()),
				Set.of(ParameterizedType.class.getName(),
						"org.springframework.core.SerializableTypeWrapper$SerializableTypeProxy",
						Serializable.class.getName()),
				Set.of(TypeVariable.class.getName(),
						"org.springframework.core.SerializableTypeWrapper$SerializableTypeProxy",
						Serializable.class.getName()),
				Set.of(WildcardType.class.getName(),
						"org.springframework.core.SerializableTypeWrapper$SerializableTypeProxy",
						Serializable.class.getName()));
		FlowableSpringAotUtils.debug("global proxies", proxies);
		for (var p : proxies) {
			var parts = p.stream().map(TypeReference::of).toArray(TypeReference[]::new);
			hints.proxies().registerJdkProxy(parts);
		}
	}

	private static Resource newResourceFor(Resource in) {
		try {
			var marker = "jar!";
			var p = in.getURL().toExternalForm();
			var rest = p.substring(p.lastIndexOf(marker) + marker.length());
			return new ClassPathResource(rest);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void registerResources(RuntimeHints hints) throws IOException {

		var resources = new HashSet<Resource>();
		var config = Stream
			.of("org/apache/ibatis/builder/xml/*.dtd", "org/apache/ibatis/builder/xml/*.xsd",
					"org/mybatis/spring/config/*.xsd")
			.map(p -> ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + p)
			.flatMap(p -> {
				try {
					return Stream.of(this.resourcePatternResolver.getResources(p));
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.map(MybatisGlobalBeanFactoryInitializationAotProcessor::newResourceFor)
			.filter(Resource::exists)
			.toList();

		resources.addAll(config);

		FlowableSpringAotUtils.debug("resources", resources);
		for (var r : resources)
			hints.resources().registerResource(r);
	}

	private void registerGlobalTypeHints(RuntimeHints hints) {

		var caches = Set.of(Cache.class, LruCache.class, BlockingCache.class, SerializedCache.class, FifoCache.class,
				NullCacheKey.class, PerpetualCache.class, CacheKey.class, WeakCache.class, TransactionalCache.class,
				SynchronizedCache.class, LoggingCache.class);

		var collections = Set.of(AbstractList.class, List.class, RandomAccess.class, Cloneable.class, Collection.class,
				TreeSet.class, SortedSet.class, Iterator.class, ArrayList.class, HashSet.class, Set.class, Map.class);

		var loggers = Set.of(Log4jImpl.class, Log4j2Impl.class, Log4j2LoggerImpl.class, Log4j2AbstractLoggerImpl.class,
				NoLoggingImpl.class, SLF4JLogger.class, StdOutImpl.class, BaseJdbcLogger.class, ConnectionLogger.class,
				PreparedStatementLogger.class, ResultSetLogger.class, StatementLogger.class, Jdk14LoggingImpl.class,
				JakartaCommonsLoggingImpl.class, Slf4jImpl.class);

		var annotations = Set.of(Select.class, Update.class, Insert.class, Delete.class, SelectProvider.class,
				UpdateProvider.class, InsertProvider.class, CacheNamespace.class, Flush.class, DeleteProvider.class,
				Options.class, Options.FlushCachePolicy.class, Many.class, Mapper.class, One.class, Property.class,
				Result.class, Results.class);

		var memberCategories = MemberCategory.values();

		var classesForReflection = new HashSet<Class<?>>();

		classesForReflection.addAll(caches);
		classesForReflection.addAll(annotations);
		classesForReflection.addAll(loggers);
		classesForReflection.addAll(collections);

		// Original version:
//		classesForReflection.addAll(Set.of(Serializable.class, SpringBootVFS.class, PerpetualCache.class, Cursor.class,
//				Optional.class, LruCache.class, MethodHandles.class, Date.class, HashMap.class, CacheRefResolver.class,
//				XNode.class, ResultFlag.class, ResultMapResolver.class, MapperScannerConfigurer.class,
//				MethodResolver.class, ProviderMethodResolver.class, ProviderContext.class,
//				MapperAnnotationBuilder.class, Logger.class, LogFactory.class, RuntimeSupport.class, Log.class,
//				SqlSessionTemplate.class, SqlSessionFactory.class, SqlSessionFactoryBean.class, ProxyFactory.class,
//				XMLLanguageDriver.class, RawLanguageDriver.class, Configuration.class, String.class, int.class,
//				Number.class, Integer.class, long.class, Long.class, short.class, Short.class, byte.class, Byte.class,
//				float.class, Float.class, boolean.class, Boolean.class, double.class, Double.class));

		classesForReflection.addAll(Set.of(Serializable.class, PerpetualCache.class, Cursor.class,
				Optional.class, LruCache.class, MethodHandles.class, Date.class, HashMap.class, CacheRefResolver.class,
				XNode.class, ResultFlag.class, ResultMapResolver.class,
				MethodResolver.class, ProviderMethodResolver.class, ProviderContext.class,
				MapperAnnotationBuilder.class, Logger.class, LogFactory.class, RuntimeSupport.class, Log.class,
				SqlSessionFactory.class, ProxyFactory.class,
				XMLLanguageDriver.class, RawLanguageDriver.class, Configuration.class, String.class, int.class,
				Number.class, Integer.class, long.class, Long.class, short.class, Short.class, byte.class, Byte.class,
				float.class, Float.class, boolean.class, Boolean.class, double.class, Double.class));

		FlowableSpringAotUtils.debug("global types for reflection", classesForReflection);

		for (var c : classesForReflection) {
			hints.reflection().registerType(c, memberCategories);
			if (FlowableSpringAotUtils.isSerializable(c)) {
				hints.serialization().registerType(TypeReference.of(c.getName()));
				if (log.isDebugEnabled())
					log.debug("the type " + c.getName() + " is serializable");
			}
		}
	}

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		return (generationContext, beanFactoryInitializationCode) -> {
			try {
				var hints = generationContext.getRuntimeHints();
				registerResources(hints);
				registerGlobalTypeHints(hints);
				registerProxies(hints);
			} //
			catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		};
	}

}
