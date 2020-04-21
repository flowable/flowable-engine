---
id: oss-introduction
title: Introducing Flowable Open Source
sidebar_label: Open Source Details
---

## License

Flowable is distributed under [the Apache V2 license](http://www.apache.org/licenses/LICENSE-2.0.html).

## Download

[<http://www.flowable.org/downloads.html>](http://www.flowable.org/downloads.html)

## Sources

The distribution contains most of the sources as JAR files. The source code for Flowable can be found on
[<https://github.com/flowable/flowable-engine>](https://github.com/flowable/flowable-engine)

## Required software

### JDK 8+

Flowable runs on a JDK higher than or equal to version 8. Go to [Oracle Java SE downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and click on button "Download JDK". There are installation instructions on that page as well. To verify that your installation was successful, run java -version on the command line. That should print the installed version of your JDK.

### IDE

Flowable development can be done with the IDE of your choice. If you would like to use the Flowable Designer then you need Eclipse Mars or Neon.
Download the Eclipse distribution of your choice from [the Eclipse download page](http://www.eclipse.org/downloads/). Unzip the downloaded file and then you should be able to start it with the Eclipse file in the directory eclipse.
Further on in this guide, there is a section on [installing our eclipse designer plugin](bpmn/ch13-Designer.md#installation).

## Reporting problems

We expect developers to have read [How to ask questions the smart way](http://www.catb.org/~esr/faqs/smart-questions.html) before reporting or asking anything.

After you’ve done that you can post questions, comments or suggestions for enhancements on [the User forum](https://forum.flowable.org) and create issues for bugs in [our Github issue tracker](https://github.com/flowable/flowable-engine/issues).

## Experimental features

Sections marked with **\[EXPERIMENTAL\]** should not be considered stable.

All classes that have .impl. in the package name are internal implementation classes and cannot be considered stable or guaranteed in any way. However, if the User Guide mentions any classes as configuration values, they are supported and can be considered stable.

## Internal implementation classes

In the JAR files, all classes in packages that have .impl. (e.g. org.flowable.engine.impl.db) in their name are implementation classes and should be considered internal use only. No stability guarantees are given on classes or interfaces that are in implementation classes.

## Versioning Strategy

Versions are denoted using a standard triplet of integers: **MAJOR.MINOR.MICRO**. The intention is that **MAJOR** versions are for evolutions of the core engines. **MINOR** versions are for new features and new APIs. **MICRO** versions are for bug fixes and improvements.

In general, Flowable attempts to remain "source compatible" in **MINOR** and **MICRO** releases for all non internal implementation classes. We define "source compatible" to mean an application will continue to build without error and the semantics have remained unchanged. Flowable also attempts to remain "binary compatible" in **MINOR** and **MICRO** releases. We define "binary compatible" to mean that this new version of Flowable can be dropped as a jar replacement into a compiled application and continue to function properly.

In case an API change is introduced in a **MINOR** release, the strategy is that a backwards compatible version is kept in it, annotated with *@Deprecated*. Such deprecated APIs will then be removed two **MINOR** versions later.
