---
id: ch05-Deployment
title: Deployment
---

## External resources

Case definitions live in the Flowable database. These case definitions can reference delegation classes when using Service Tasks or Spring beans from the Flowable configuration file. These classes and the Spring configuration file have to be available to all CMMN engines that may execute the case definitions.

### Java classes

All custom classes that are used in your case definition (for example, PlanItemJavaDelegates used in Service Tasks) should be present on the engine’s classpath when an instance of the case is started.

During deployment of a case definition however, those classes don’t have to be present on the classpath. This means that your delegation classes don’t have to be on the classpath when deploying a new case definition, for example.

When you are using the demo setup and you want to add your custom classes, you should add a JAR containing your classes to the flowable-task or flowable-rest webapp lib. Don’t forget to include the dependencies of your custom classes (if any) as well. Alternatively, you can include your dependencies in the libraries directory of your Tomcat installation, ${tomcat.home}/lib (or similar place for other web containers).

### Using Spring beans from a case instance

When expressions or scripts use Spring beans, those beans have to be available to the engine when executing the case definition. If you are building your own webapp and you configure your CMMN engine in your context as described in [the spring integration section](cmmn/ch04-Spring.md#spring-integration), that is straightforward. But bear in mind that you also should update the Flowable task and rest webapps with that context if you use it.

## Versioning of case definitions

CMMN doesn’t have a notion of versioning. That is actually good, because the executable CMMN file will probably live in a version control system repository (such as Subversion, Git or Mercurial) as part of your development project. However, versions of case definitions are created in the engine as part of deployment. During deployment, Flowable will assign a version to the CaseDefinition before it is stored in the Flowable DB.

For each case definition in a deployment, the following steps are performed to initialize the properties key, version, name and id:

-   The case definition id attribute in the XML file is used as the case definition key property.

-   The case definition name attribute in the XML file is used as the case definition name property. If the name attribute is not specified, then the id attribute is used as the name.

-   The first time a case with a particular key is deployed, version 1 is assigned. For all subsequent deployments of case definitions with the same key, the version will be set 1 higher than the highest currently deployed version. The key property is used to distinguish case definitions.

Take for example the following case

    <definitions id="myDefinitions" >
      <case id="myCase" name="My important case" >
        ...

When deploying this case definition, the case definition in the database will look like this:

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>id</th>
<th>key</th>
<th>name</th>
<th>version</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>676</p></td>
<td><p>myCase</p></td>
<td><p>My important case</p></td>
<td><p>1</p></td>
</tr>
</tbody>
</table>

Suppose we now deploy an updated version of the same case (for example, changing some human tasks), but the id of the case definition remains the same. The case definition table will now contain the following entries:

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>id</th>
<th>key</th>
<th>name</th>
<th>version</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>676</p></td>
<td><p>myCase</p></td>
<td><p>My important case</p></td>
<td><p>1</p></td>
</tr>
<tr class="even">
<td><p>870</p></td>
<td><p>myCase</p></td>
<td><p>My important case</p></td>
<td><p>2</p></td>
</tr>
</tbody>
</table>

When the runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start() is called, it will now use the case definition with version 2, as this is the latest version of the case definition.

Should we create a second case, as defined below and deploy this to Flowable, a third row will be added to the table.

    <definitions id="myNewDefinitions" >
      <case id="myNewCase" name="My important case" >
        ...

The table will look like this:

<table>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>id</th>
<th>key</th>
<th>name</th>
<th>version</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>676</p></td>
<td><p>myCase</p></td>
<td><p>My important case</p></td>
<td><p>1</p></td>
</tr>
<tr class="even">
<td><p>870</p></td>
<td><p>myCase</p></td>
<td><p>My important case</p></td>
<td><p>2</p></td>
</tr>
<tr class="odd">
<td><p>1033</p></td>
<td><p>myNewCase</p></td>
<td><p>My important case</p></td>
<td><p>1</p></td>
</tr>
</tbody>
</table>

Note how the key for the new case is different from our first case. Even though the name is the same (we should probably have changed that too), Flowable only considers the id attribute when distinguishing cases. The new case is therefore deployed with version 1.

## Category

Both deployments and case definitions have user-defined categories. The case definition category is initialized with the value of the targetNamespace attribute in the CMMN XML: &lt;definitions ... targetNamespace="yourCategory" ...

The deployment category can also be specified in the API like this:

    repositoryService
        .createDeployment()
        .category("yourCategory")
        ...
        .deploy();
