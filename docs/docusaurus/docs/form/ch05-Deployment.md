---
id: ch05-Deployment
title: Deployment
---

## Form Definitions

Form Definitions with .form extension can be deployed to the Form engine.

When the Form engine is plugged into the Process engine, the Form definitions can be packed into a business archive (BAR) together with other process related resources. The Process engine deployment service will take care of deploying the Form resources to the Form engine when the flowable-form-engine-configurator or flowable-form-spring-configurator modules are used.

### Form Definitions

A Form definition consists of one or more form field definitions.
When deploying a Form definition, it is inserted into the ACT\_FO\_FORM\_DEFINITION table.

### Deploying programmatically

Deploying a Form definition can be done like this:

    String formDefinition = "path/to/definition-one.form";
    ZipInputStream inputStream = new ZipInputStream(new FileInputStream(barFileName));

    formRepositoryService.createDeployment()
        .name("definition-one")
        .addClasspathResource(formDefinition)
        .deploy();

## Versioning of Form definitions

Versions of Form definitions are created during deployment, where Flowable will assign a version to the form definition before it is stored in the Flowable Form DB.

For each Form definition, the following steps are performed to initialize the properties key, version, name and id:

-   The form definition key attribute in the definition JSON file is used as the form definition key property.

-   The form definition name attribute in the JSON file is used as the form definition name property.

-   The first time a form definition with a particular key is deployed, version 1 is assigned. For all subsequent deployments of form definition with the same key, the version will be set 1 higher than the maximum currently deployed version. The key property is used to distinguish form definition.

-   The id property is a unique number to guarantee uniqueness of the form definition id for the form definition caches in a clustered environment.

Take, for example, the following form definition:

    {
        "key": "form1",
        "name": "My first form",
        "fields": [
            {
                "id": "input1",
                "name": "Input1",
                "type": "text",
                "required": false,
                "placeholder": "empty"
            }
        ]
    }

When deploying this form definition, the form definition table in the database will look like this:

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
<td><p>form1</p></td>
<td><p>My first form</p></td>
<td><p>1</p></td>
</tr>
</tbody>
</table>

Suppose we now deploy an updated version of the same form definition (for example, changing the text field), but the key of the form definition remains the same. The form definition table will now contain the following entries:

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
<td><p>form1</p></td>
<td><p>My first form</p></td>
<td><p>1</p></td>
</tr>
<tr class="even">
<td><p>e9c2a6c0-c085-11e6-9096-6ab56fad108a</p></td>
<td><p>form1</p></td>
<td><p>My first form</p></td>
<td><p>2</p></td>
</tr>
</tbody>
</table>

Should we create a second form definition, as defined below, and deploy this to Flowable Form Engine, a third row will be added to the table.

    {
        "key": "form2",
        "name": "My second form",
        "fields": [
            {
                "id": "input2",
                "name": "Input2",
                "type": "text",
                "required": false,
                "placeholder": "empty"
            }
        ]
    }

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
<td><p>form1</p></td>
<td><p>My first form</p></td>
<td><p>1</p></td>
</tr>
<tr class="even">
<td><p>e9c2a6c0-c085-11e6-9096-6ab56fad108a</p></td>
<td><p>form1</p></td>
<td><p>My first form</p></td>
<td><p>2</p></td>
</tr>
<tr class="odd">
<td><p>d317d3f7-e948-11e6-9ce6-b28c070b517d</p></td>
<td><p>form2</p></td>
<td><p>My second form</p></td>
<td><p>1</p></td>
</tr>
</tbody>
</table>

Note how the key for the new form definition is different from our first form definition. The Flowable Form engine only considers the key attribute when distinguishing form definitions. The new form definition is therefore deployed with version 1.

## Category

Both Form deployments and form definitions can have user-defined categories.
The deployment category can be specified in the API like this:

    formRepository
        .createDeployment()
        .category("yourCategory")
        ...
        .deploy();

The form definition category can be specified in the API like this:

    formRepository.setFormDefinitionCategory("e9c2a6c0-c085-11e6-9096-6ab56fad108a", "yourCategory");
