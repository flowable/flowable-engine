---
id: ch05-Deployment
title: Deployment
---

## DMN Definitions

DMN Definitions with .dmn extension can be deployed to the DMN engine.

When the DMN engine is plugged into the Process engine, the DMN definitions can be packed into a business archive (BAR) together with other process related resources. The Process engine deployment service will take care of deploying the DMN resources to the DMN engine.

> **Note**
>
> Java classes used for custom expression functions present in the business archive **will not be added to the classpath**. All custom classes used in expressions in decisions in the business archive should be present on the Flowable (DMN) Engine classpath in order to run the decisions.

### DMN Definitions, decisions and decision tables

A DMN definition consists, amongst other things, of decisions. A decision has one expression. The DMN spec describes several types of expressions. Currently in Flowable DMN we support the decision table type of expression.
When deploying a DMN definition, every decision, which can contain one **decision table**, is inserted separately into the ACT\_DMN\_DECISION table.

### Deploying programmatically

Deploying a DMN definition can be done like this:

    String dmnDefinition = "path/to/definition-one.dmn"; //Don't forget the .dmn extension!

    repositoryService.createDeployment()
        .name("Deployment of DMN definition-one")
        .addClasspathResource(dmnDefinition)
        .deploy();

You can use other methods to add the DMN definitions to the deployment like `addInputStream`. This is an example to deploy a DMN definition from an external file:

    File dmnFile = new File("/path/to/definition-two.dmn"); //Don't forget the .dmn extension!

    repositoryService.createDeployment()
        .name("Deployment of DMN definition-two")
        .addInputStream(dmnFile.getName(), new FileInputStream(dmnFile))
        .deploy();

The name of the deployment can be any text but the resource name must always contain a valid DMN resource name suffix (".dmn").

### Java classes

All classes that contain the custom expression functions that are used in your decisions should be present on the engine’s classpath when a decision is executed.

During deployment of a DMN definition, however, these classes don’t have to be present on the classpath.

When you are using the demo setup and you want to add your custom classes, you should add a JAR containing your classes to the flowable-ui-app or flowable-app-rest webapp lib. Don’t forget to include the dependencies of your custom classes (if any) as well. Alternatively, you can include your dependencies in the libraries directory of your Tomcat installation, ${tomcat.home}/lib.

### Creating a single app

Instead of making sure that all DMN engines have all the delegation classes on their classpath and use the right Spring configuration, you may consider including the flowable-rest webapp inside your own webapp so that there is only a single DmnEngine.

## Versioning of DMN decisions

DMN doesn’t have a notion of versioning. That is actually good, because the executable DMN definition file will probably live in a version control system repository (such as Subversion, Git or Mercurial) as part of your development project. Versions of DMN decisions are created during deployment. During deployment, Flowable will assign a version to the decision before it’s stored in the Flowable DB.

For each DMN decision in a DMN definition, the following steps are performed to initialize the properties key, version, name and id:

-   The decision id attribute in the definition XML file is used as the decision table key property.

-   The decision name attribute in the XML file is used as the decision table name property.

-   The first time a decision with a particular key is deployed, version 1 is assigned. For all subsequent deployments of decisions with the same key, the version will be set 1 higher than the maximum currently deployed version. The key property is used to distinguish decisions.

-   The id property is a unique number to guarantee uniqueness of the decision table identifier for the decision table caches in a clustered environment.

Take for example the following process:

    <definitions id="myDefinitions" >
      <decision id="myDecision" name="My important decision" >
        <decisionTable id="decisionTable1" hitPolicy="FIRST" >
        ...

When deploying this decision, the decision table in the database will look like this:

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
<td><p>e29d4126-ed4d-11e6-9e00-7282cbd6ce64</p></td>
<td><p>myDecision</p></td>
<td><p>My important decision</p></td>
<td><p>1</p></td>
</tr>
</tbody>
</table>

Suppose we now deploy an updated version of the same process (for example, changing some user tasks), but the id of the process definition remains the same. The process definition table will now contain the following entries:

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
<td><p>e29d4126-ed4d-11e6-9e00-7282cbd6ce64</p></td>
<td><p>myDecision</p></td>
<td><p>My important decision</p></td>
<td><p>1</p></td>
</tr>
<tr class="even">
<td><p>e9c2a6c0-c085-11e6-9096-6ab56fad108a</p></td>
<td><p>myDecision</p></td>
<td><p>My important decision</p></td>
<td><p>2</p></td>
</tr>
</tbody>
</table>

When the dmnRuleService.executeDecisionByKey("myDecision") is called, it will now use the decision definition with version 2, as this is the latest version of the decision definition.

If we create a second decision, as defined below and deploy this to Flowable DMN, a third row will be added to the table.

    <definitions id="myNewDefinitions" >
      <decision id="myNewDecision" name="My important decision" >
        <decisionTable id="decisionTable1" hitPolicy="FIRST" >
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
<td><p>e29d4126-ed4d-11e6-9e00-7282cbd6ce64</p></td>
<td><p>myDecision</p></td>
<td><p>My important decision</p></td>
<td><p>1</p></td>
</tr>
<tr class="even">
<td><p>e9c2a6c0-c085-11e6-9096-6ab56fad108a</p></td>
<td><p>myDecision</p></td>
<td><p>My important decision</p></td>
<td><p>2</p></td>
</tr>
<tr class="odd">
<td><p>d317d3f7-e948-11e6-9ce6-b28c070b517d</p></td>
<td><p>myNewDecision</p></td>
<td><p>My important decision</p></td>
<td><p>1</p></td>
</tr>
</tbody>
</table>

Note how the key for the new decision is different from our first decision. Even though the name is the same (we should probably have changed that too), Flowable DMN only considers the id attribute when distinguishing decisions. The new decision is therefore deployed with version 1.

## Category

Both DMN deployments and decision tables can have user defined categories.
The deployment category can be specified in the API like this:

    dmnRepository
        .createDeployment()
        .category("yourCategory")
        ...
        .deploy();

The decision table category can be specified in the API like this:

    dmnRepository.setDecisionTableCategory("e9c2a6c0-c085-11e6-9096-6ab56fad108a", "yourCategory");
