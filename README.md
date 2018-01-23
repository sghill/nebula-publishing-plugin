# Nebula Publishing Plugin

[![Build Status](https://travis-ci.org/nebula-plugins/nebula-publishing-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/nebula-publishing-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/nebula-publishing-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/nebula-publishing-plugin?branch=master)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/nebula-publishing-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/nebula-publishing-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Usage

To apply this plugin if using Gradle 2.1 or newer

    plugins {
      id 'nebula.<publishing plugin of your choice>' version '5.1.0'
    }

If using an older version of Gradle

    buildscript {
      repositories { jcenter() }
      dependencies {
        classpath 'com.netflix.nebula:nebula-publishing-plugin:5.1.0'
      }
    }

    apply plugin: 'nebula.<publishing plugin of your choice>'


Provides publishing related plugins to reduce boiler plate and add functionality to maven-publish/ivy-publish.

## Maven Related Publishing Plugins

### nebula.maven-base-publish

Create a maven publication named nebula. This named container will be used to generate task names as described in 
[the Maven Publish documentation on gradle.org](https://docs.gradle.org/current/userguide/publishing_maven.html). Examples 
include `publishNebulaPublicationToMavenLocalRepository` and `publishNebulaPublicationToMyArtifactoryRepository`. 

All of the maven based publishing plugins will interact with this publication. All of the other maven based plugins will
also automatically apply this so most users will not need to.

Eliminates this boilerplate:

    apply plugin: 'maven-publish'
                   
    publishing {
      publications {
        nebula(MavenPublication) {
        }
      }
    }

### nebula.maven-dependencies

We detect if the war plugin is applied and publish the web component, it will default to publishing the java component.

Applying this plugin would be the same as:

if war detected

    publishing {
      publications {
        nebula(MavenPublication) {
          from components.web
          // code to append dependencies since they are useful information
        }
      }
    }

if war not detected

    publishing {
      publications {
        nebula(MavenPublication) {
          from components.java
        }
      }
    }
    
Note that nebula.maven-dependencies is based on the Gradle maven-publish plugin, which places first order compile dependencies in the runtime scope in the POM.  If you wish your compile dependencies to be compile scope dependencies in the POM, you can add a withXml block like so:

    publishing {
        publications {
            nebula(MavenPublication) {
                pom.withXml {
                    configurations.compile.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
                        asNode().dependencies[0].dependency.find {
                            it.artifactId[0].text() == dep.moduleName &&
                            it.groupId[0].text() == dep.moduleGroup
                        }?.scope[0]?.value = 'compile'
                    }
                }
            }
        }
    }

### nebula.maven-publish

Link all the other maven plugins together.

### nebula.maven-manifest

Adds a properties block to the pom. Copying in data from our [gradle-info-plugin](https://github.com/nebula-plugins/gradle-info-plugin).

### nebula.maven-resolved-dependencies

Walk through all project dependencies and replace all dynamic dependencies with what those jars currently resolve to. Necessary if [nebula-dependency-recommender-plugin](https://github.com/nebula-plugins/nebula-dependency-recommender-plugin) is used to include version numbers in POM files generated by Gradle's `maven-publish`.

### nebula.maven-excludes

Place excludes from single dependencies into the pom.

### nebula.maven-scm

Adds scm block to the pom. Tries to use the the info-scm plugin from [gradle-info-plugin](https://github.com/nebula-plugins/gradle-info-plugin) 

## Ivy Related Publishing Plugins

### nebula.ivy-base-publish

Create an ivy publication named nebulaIvy. This named container will be used to generate task names as described in 
[the Ivy Publish documentation on gradle.org](https://docs.gradle.org/current/userguide/publishing_ivy.html). Examples 
include `publishNebulaIvyPublicationToMyLocalIvyRepository` and `publishNebulaIvyPublicationToMyArtifactoryRepository`. 

All of the ivy based publishing plugins will interact with this publication. All of the other ivy based plugins will
also automatically apply this so most users will not need to.

Eliminates this boilerplate:

    apply plugin: 'ivy-publish'
                   
    publishing {
      publications {
        nebulaIvy(IvyPublication) {
        }
      }
    }
    
### nebula.ivy-dependencies

We detect if the war plugin is applied and publish the web component, it will default to publishing the java component.

Applying this plugin would be the same as:

if war detected

    publishing {
      publications {
        nebulaIvy(IvyPublication) {
          from components.web
          // code to append dependencies since they are useful information
        }
      }
    }

if war not detected

    publishing {
      publications {
        nebulaIvy(IvyPublication) {
          from components.java
        }
      }
    }

### nebula.ivy-publish

Link all the other ivy plugins together.

### nebula.ivy-manifest

Adds a properties block to the ivy.xml. Copying in data from our [gradle-info-plugin](https://github.com/nebula-plugins/gradle-info-plugin).

### nebula.ivy-resolved

Walk through all project dependencies and replace all dynamic dependencies with what those jars currently resolve to.

### nebula.ivy-excludes

Place excludes from single dependencies into the ivy.xml

## Extra Publication Plugins

### nebula.compile-api

Add a `compileApi` dependency scope that will place those dependencies in the generated ivy or pom files compile conf/scope

### nebula.javadoc-jar

Creates a javadocJar task to package up javadoc and add it to publications so it is published.

Eliminates this boilerplate:

    tasks.create('javadocJar', Jar) {
      dependsOn tasks.javadoc
      from tasks.javadoc.destinationDir
      classifier 'javadoc'
      extension 'jar'
      group 'build'
    }
    publishing {
      publications {
        nebula(MavenPublication) { // if maven-publish is applied
          artifact tasks.javadocJar
        }
        nebulaIvy(IvyPublication) { // if ivy-publish is applied
          artifact tasks.javadocJar
        }
      }
    }

### nebula.source-jar

Creates a sourceJar tasks to package up the the source code of your project and add it to publications so it is published.

Eliminates this boilerplate:

    tasks.create('sourceJar', Jar) {
        dependsOn tasks.classes
        from sourceSets.main.allSource
        classifier 'sources'
        extension 'jar'
        group 'build'
    }
    publishing {
      publications {
        nebula(MavenPublication) { // if maven-publish is applied
          artifact tasks.sourceJar
        }
        nebulaIvy(IvyPublication) { // if ivy-publish is applied
          artifact tasks.sourceJar
        }
      }
    }
    
### nebula.publish-verification

Creates a task which runs before actual publication into repositories. It verifies that any of your direct dependencies don't have
a lower status then your project. E.g. your project has a status `release` but one of your direct dependencies is `SNAPSHOT`.
The task will prevent the publication until you will depend only on final releases. Your test dependencies are NOT verified.

This plugin is automatically applied with `nebula.ivy-publish` or `nebula.maven-publish`. The task itself is a dependence of
tasks with type `PublishToIvyRepository` or `PublishToMavenRepository`. The task will also get hooked to tasks named 
`artifactoryPublish` and `artifactoryDeploy` coming from `com.jfrog.artifactory` plugin. If you need any other integration
you have to manually configure relationship in your build file.

#### How to ignore selected dependencies.

It might happen that you need to create a release which have to depend on a library which have a lower status. E.g. it
contains critical bug fix or you are early adopter of release candidates. You can exclude those libraries from the 
checking process.

    dependencies {
      compile 'group:this_will_be_checked:1.0'
      compile nebulaPublishVerification.ignore('foo:bar:1.0-SNAPSHOT')
      compile nebulaPublishVerification.ignore(group: 'baz', name: 'bax', version: '1.0-SNAPSHOT')
    }
 
    
Gradle Compatibility Tested
---------------------------

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version    | Works |
| :---------------: | :---: |
| < 4.0-milestone-2 | no    |
| 4.0-milestone-2   | yes   |

LICENSE
=======

Copyright 2014-2017 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
