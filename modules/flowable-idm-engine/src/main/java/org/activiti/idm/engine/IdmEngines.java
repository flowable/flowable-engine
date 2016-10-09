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
package org.activiti.idm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IdmEngines {

  private static Logger log = LoggerFactory.getLogger(IdmEngines.class);

  public static final String NAME_DEFAULT = "default";

  protected static boolean isInitialized;
  protected static Map<String, IdmEngine> idmEngines = new HashMap<String, IdmEngine>();
  protected static Map<String, IdmEngineInfo> idmEngineInfosByName = new HashMap<String, IdmEngineInfo>();
  protected static Map<String, IdmEngineInfo> idmEngineInfosByResourceUrl = new HashMap<String, IdmEngineInfo>();
  protected static List<IdmEngineInfo> idmEngineInfos = new ArrayList<IdmEngineInfo>();

  /**
   * Initializes all dmn engines that can be found on the classpath for resources <code>activiti.dmn.cfg.xml</code> and for resources <code>activiti-dmn-context.xml</code> (Spring style
   * configuration).
   */
  public synchronized static void init() {
    if (!isInitialized()) {
      if (idmEngines == null) {
        // Create new map to store dmn engines if current map is null
        idmEngines = new HashMap<String, IdmEngine>();
      }
      ClassLoader classLoader = IdmEngines.class.getClassLoader();
      Enumeration<URL> resources = null;
      try {
        resources = classLoader.getResources("activiti.idm.cfg.xml");
      } catch (IOException e) {
        throw new ActivitiIdmException("problem retrieving activiti.idm.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
      }

      // Remove duplicated configuration URL's using set. Some
      // classloaders may return identical URL's twice, causing duplicate startups
      Set<URL> configUrls = new HashSet<URL>();
      while (resources.hasMoreElements()) {
        configUrls.add(resources.nextElement());
      }
      for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext();) {
        URL resource = iterator.next();
        log.info("Initializing idm engine using configuration '{}'", resource.toString());
        initFormEngineFromResource(resource);
      }

      /*
       * try { resources = classLoader.getResources("activiti-form-context.xml"); } catch (IOException e) { throw new ActivitiDmnException(
       * "problem retrieving activiti-dmn-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e); } while (resources.hasMoreElements()) { URL resource =
       * resources.nextElement(); log.info("Initializing dmn engine using Spring configuration '{}'", resource.toString()); initDmnEngineFromSpringResource(resource); }
       */

      setInitialized(true);
    } else {
      log.info("Idm engines already initialized");
    }
  }

  /**
   * Registers the given dmn engine. No {@link IdmEngineInfo} will be available for this dmn engine. An engine that is registered will be closed when the {@link IdmEngines#destroy()} is called.
   */
  public static void registerIdmEngine(IdmEngine idmEngine) {
    idmEngines.put(idmEngine.getName(), idmEngine);
  }

  /**
   * Unregisters the given dmn engine.
   */
  public static void unregister(IdmEngine idmEngine) {
    idmEngines.remove(idmEngine.getName());
  }

  private static IdmEngineInfo initFormEngineFromResource(URL resourceUrl) {
    IdmEngineInfo idmEngineInfo = idmEngineInfosByResourceUrl.get(resourceUrl.toString());
    // if there is an existing dmn engine info
    if (idmEngineInfo != null) {
      // remove that dmn engine from the member fields
      idmEngineInfos.remove(idmEngineInfo);
      if (idmEngineInfo.getException() == null) {
        String idmEngineName = idmEngineInfo.getName();
        idmEngines.remove(idmEngineName);
        idmEngineInfosByName.remove(idmEngineName);
      }
      idmEngineInfosByResourceUrl.remove(idmEngineInfo.getResourceUrl());
    }

    String resourceUrlString = resourceUrl.toString();
    try {
      log.info("initializing idm engine for resource {}", resourceUrl);
      IdmEngine idmEngine = buildIdmEngine(resourceUrl);
      String idmEngineName = idmEngine.getName();
      log.info("initialised idm engine {}", idmEngineName);
      idmEngineInfo = new IdmEngineInfo(idmEngineName, resourceUrlString, null);
      idmEngines.put(idmEngineName, idmEngine);
      idmEngineInfosByName.put(idmEngineName, idmEngineInfo);
    } catch (Throwable e) {
      log.error("Exception while initializing idm engine: {}", e.getMessage(), e);
      idmEngineInfo = new IdmEngineInfo(null, resourceUrlString, getExceptionString(e));
    }
    idmEngineInfosByResourceUrl.put(resourceUrlString, idmEngineInfo);
    idmEngineInfos.add(idmEngineInfo);
    return idmEngineInfo;
  }

  private static String getExceptionString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  protected static IdmEngine buildIdmEngine(URL resource) {
    InputStream inputStream = null;
    try {
      inputStream = resource.openStream();
      IdmEngineConfiguration idmEngineConfiguration = IdmEngineConfiguration.createIdmEngineConfigurationFromInputStream(inputStream);
      return idmEngineConfiguration.buildIdmEngine();

    } catch (IOException e) {
      throw new ActivitiIdmException("couldn't open resource stream: " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  /** Get initialization results. */
  public static List<IdmEngineInfo> getIdmEngineInfos() {
    return idmEngineInfos;
  }

  /**
   * Get initialization results. Only info will we available for form engines which were added in the {@link IdmEngines#init()}. No {@link IdmEngineInfo} is available for engines which were
   * registered programmatically.
   */
  public static IdmEngineInfo getIdmEngineInfo(String idmEngineName) {
    return idmEngineInfosByName.get(idmEngineName);
  }

  public static IdmEngine getDefaultIdmEngine() {
    return getIdmEngine(NAME_DEFAULT);
  }

  /**
   * obtain a dmn engine by name.
   * 
   * @param dmnEngineName
   *          is the name of the dmn engine or null for the default dmn engine.
   */
  public static IdmEngine getIdmEngine(String idmEngineName) {
    if (!isInitialized()) {
      init();
    }
    return idmEngines.get(idmEngineName);
  }

  /**
   * retries to initialize a dmn engine that previously failed.
   */
  public static IdmEngineInfo retry(String resourceUrl) {
    log.debug("retying initializing of resource {}", resourceUrl);
    try {
      return initFormEngineFromResource(new URL(resourceUrl));
    } catch (MalformedURLException e) {
      throw new ActivitiIdmException("invalid url: " + resourceUrl, e);
    }
  }

  /**
   * provides access to dmn engine to application clients in a managed server environment.
   */
  public static Map<String, IdmEngine> getIdmEngines() {
    return idmEngines;
  }

  /**
   * closes all dmn engines. This method should be called when the server shuts down.
   */
  public synchronized static void destroy() {
    if (isInitialized()) {
      Map<String, IdmEngine> engines = new HashMap<String, IdmEngine>(idmEngines);
      idmEngines = new HashMap<String, IdmEngine>();

      for (String idmEngineName : engines.keySet()) {
        IdmEngine idmEngine = engines.get(idmEngineName);
        try {
          idmEngine.close();
        } catch (Exception e) {
          log.error("exception while closing {}", (idmEngineName == null ? "the default idm engine" : "idm engine " + idmEngineName), e);
        }
      }

      idmEngineInfosByName.clear();
      idmEngineInfosByResourceUrl.clear();
      idmEngineInfos.clear();

      setInitialized(false);
    }
  }

  public static boolean isInitialized() {
    return isInitialized;
  }

  public static void setInitialized(boolean isInitialized) {
    IdmEngines.isInitialized = isInitialized;
  }
}
