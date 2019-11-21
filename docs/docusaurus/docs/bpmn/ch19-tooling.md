---
id: ch19-tooling
title: Tooling
---

## JMX

### Introduction

It is possible to connect to Flowable engine using standard Java Management Extensions (JMX) technology in order to get information or to change its behavior. Any standard JMX client can be used for that purpose. Enabling and disabling Job Executor, deploy new process definition files and deleting them are just samples of what could be done using JMX without writing a single line of code.

### Quick Start

By default JMX is not enabled. To enable JMX in its default configuration, it is enough to add the flowable-jmx jar file to your classpath, using Maven or by any other means. In case you are using Maven, you can add proper dependency by adding the following lines in your pom.xml:

    <dependency>
      <groupId>org.flowable</groupId>
      <artifactId>flowable-jmx</artifactId>
      <version>latest.version</version>
    </dependency>

After adding the dependency and building process engine, the JMX connection is ready to be used. Just run jconsole available in a standard JDK distribution. In the list of local processes, you will see the JVM containing Flowable. If for any reason the proper JVM is not listed in "local processes" section, try connecting to it using this URL in "Remote Process" section:

    service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi/flowable

You can find the exact local URL in your log files. After connecting, you can see the standard JVM statistics and MBeans. You can view Flowable specific MBeans by selecting MBeans tab and select "org.flowable.jmx.Mbeans" on the right panel. By selecting any MBean, you can query information or change configuration. This snapshot shows how the jconsole looks like:

Any JMX client not limited to jconsole can be used to access MBeans. Most of data center monitoring tools have some connectors which enables them to connect to JMX MBeans.

### Attributes and operations

Here is a list available attributes and operations at this moment. This list may extend in future releases based on needs.

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>MBean</th>
<th>Type</th>
<th>Name</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>ProcessDefinitionsMBean</p></td>
<td><p>Attribute</p></td>
<td><p>processDefinitions</p></td>
<td><p>Id, Name, Version, IsSuspended properties of deployed process definitions as a list of list of Strings</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>Attribute</p></td>
<td><p>deployments</p></td>
<td><p>Id, Name, TenantId properties of current deployments</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>method</p></td>
<td><p>getProcessDefinitionById(String id)</p></td>
<td><p>Id, Name, Version and IsSuspended properties of the process definition with given id</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>method</p></td>
<td><p>deleteDeployment(String id)</p></td>
<td><p>Deletes deployment with the given Id</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>method</p></td>
<td><p>suspendProcessDefinitionById(String id)</p></td>
<td><p>Suspends the process definition with the given Id</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>method</p></td>
<td><p>activatedProcessDefinitionById(String id)</p></td>
<td><p>Activates the process definition with the given Id</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>method</p></td>
<td><p>suspendProcessDefinitionByKey(String id)</p></td>
<td><p>Suspends the process definition with the given key</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>method</p></td>
<td><p>activatedProcessDefinitionByKey(String id)</p></td>
<td><p>Activates the process definition with the given key</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>method</p></td>
<td><p>deployProcessDefinition(String resourceName, String processDefinitionFile)</p></td>
<td><p>Deploys the process definition file</p></td>
</tr>
<tr class="even">
<td><p>JobExecutorMBean</p></td>
<td><p>attribute</p></td>
<td><p>isJobExecutorActivated</p></td>
<td><p>Returns true if the async job executor is activated, false otherwise</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>method</p></td>
<td><p>setJobExecutorActivate(Boolean active)</p></td>
<td><p>Activates and Deactivates the async job executor based on the given boolean</p></td>
</tr>
</tbody>
</table>

### Configuration

JMX uses default configuration to make it easy to deploy with the most used configuration. However it is easy to change the default configuration. You can do it programmatically or via configuration file. The following code excerpt shows how this could be done in the configuration file:

    <bean id="processEngineConfiguration" class="...SomeProcessEngineConfigurationClass">
      ...
      <property name="configurators">
        <list>
          <bean class="org.flowable.management.jmx.JMXConfigurator">

            <property name="connectorPort" value="1912" />
            <property name="serviceUrlPath" value="/jmxrmi/flowable" />

            ...
          </bean>
        </list>
      </property>
    </bean>

This table shows what parameters you can configure along with their default values:

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Name</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>disabled</p></td>
<td><p>false</p></td>
<td><p>if set, JMX will not be started even in presence of the dependency</p></td>
</tr>
<tr class="even">
<td><p>domain</p></td>
<td><p>org.flowable.jmx.Mbeans</p></td>
<td><p>Domain of MBean</p></td>
</tr>
<tr class="odd">
<td><p>createConnector</p></td>
<td><p>true</p></td>
<td><p>if true, creates a connector for the started MbeanServer</p></td>
</tr>
<tr class="even">
<td><p>MBeanDomain</p></td>
<td><p>DefaultDomain</p></td>
<td><p>domain of MBean server</p></td>
</tr>
<tr class="odd">
<td><p>registryPort</p></td>
<td><p>1099</p></td>
<td><p>appears in the service URL as registry port</p></td>
</tr>
<tr class="even">
<td><p>serviceUrlPath</p></td>
<td><p>/jmxrmi/flowable</p></td>
<td><p>appears in the service URL</p></td>
</tr>
<tr class="odd">
<td><p>connectorPort</p></td>
<td><p>-1</p></td>
<td><p>if greater than zero, will appear in service URL as connector port</p></td>
</tr>
</tbody>
</table>

### JMX Service URL

The JMX service URL has the following format:

    service:jmx:rmi://<hostName>:<connectorPort>/jndi/rmi://<hostName>:<registryPort>/<serviceUrlPath>

hostName will be automatically set to the network name of the machine.
connectorPort, registryPort and serviceUrlPath can be configured.

If connectionPort is less than zero, the corresponding part of service URL will be dropped and it will be simplified to:

    service:jmx:rmi:///jndi/rmi://:<hostname>:<registryPort>/<serviceUrlPath>

## Maven archetypes

### Create Test Case

During development, sometimes it is helpful to create a small test case to test an idea or a feature,
before implementing it in the real application.
This helps to isolate the subject under test. JUnit test cases are also the preferred tools for communicating
bug reports and feature requests.
Having a test case attached to a bug report or feature request jira issue, considerably reduces its fixing time.

To facilitate creation of a test case, a maven archetype is available. By use of this archetype, one can rapidly create a standard test case.
The archetype should be already available in the standard repository. If not, you can easily install it in your local maven repository folder by just typing
**mvn install** in **tooling/archtypes** folder.

The following command creates the unit test project:

    mvn archetype:generate \
    -DarchetypeGroupId=org.flowable \
    -DarchetypeArtifactId=flowable-archetype-unittest \
    -DarchetypeVersion=<current version> \
    -DgroupId=org.myGroup \
    -DartifactId=myArtifact

The effect of each parameter is explained in the following table:

<table>
<caption>Unittest Generation archetype parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<tbody>
<tr class="odd">
<td><p>Row</p></td>
<td><p>Parameter</p></td>
<td><p>Explanation</p></td>
</tr>
<tr class="even">
<td><p>1</p></td>
<td><p>archetypeGroupId</p></td>
<td><p>Group id of the archetype. should be <strong>org.flowable</strong></p></td>
</tr>
<tr class="odd">
<td><p>2</p></td>
<td><p>archetypeArtifactId</p></td>
<td><p>Artifact if of the archetype. should be <strong>flowable-archetype-unittest</strong></p></td>
</tr>
<tr class="even">
<td><p>3</p></td>
<td><p>archetypeVersion</p></td>
<td><p>Flowable version used in the generated test project</p></td>
</tr>
<tr class="odd">
<td><p>4</p></td>
<td><p>groupId</p></td>
<td><p>Group id of the generated test project</p></td>
</tr>
<tr class="even">
<td><p>5</p></td>
<td><p>artifactId</p></td>
<td><p>Artifact id of the generated test project</p></td>
</tr>
</tbody>
</table>

The directory structure of the generated project would be like this:

    .
    ├── pom.xml
    └── src
        └── test
            ├── java
            │   └── org
            │       └── myGroup
            │           └── MyUnitTest.java
            └── resources
                ├── flowable.cfg.xml
                ├── log4j.properties
                └── org
                    └── myGroup
                        └── my-process.bpmn20.xml

You can modify the Java unit test case and its corresponding process model, or add new test cases and process models.
If you are using the project to articulate a bug or a feature, test case should fail initially. It should then pass
after the desired bug is fixed or the desired feature is implemented.
Please make sure to clean the project by typing **mvn clean** before sending it.
