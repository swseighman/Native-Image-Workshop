# GraalVM Native Image Workshop

## Overview 

A GraalVM Workshop that will introduce Native Image & demonstrate some of the things that you can do with it,

The workshop is divided into a number of sub-pages, each largely self-contained, that cover one aspect of GraalVM
Native Image. Some of these workshops are equally applicable to the Community Edition & the Enterprise Edition, but some
focus on functionality that is only available within the Enterprise Edition, such as Profile Guided Optimisations.

## Credits

This workshop is a fork from [shelajev/workshop](https://github.com/shelajev/workshop), so full credit must go to him.

## Install GraalVM 

The instructions to install GraalVM can be found online 
[here](https://docs.oracle.com/en/graalvm/enterprise/20/docs/getting-started/installation-linux/).

The prerequsites for getting the native image component working are described in the 
[docs](https://docs.oracle.com/en/graalvm/enterprise/20/docs/reference-manual/enterprise-native-image/).

This workshop assumes that you are using either Linux or Mac, but is equally applicable to Windows. You might just have
to adpat it, for example the `bash` shell script sections etc may need to be updated.

## Table of Contents

* [What is Native Image](./0/)
* [Building a simple GraalVM native image](./1/)
* [Assisted configuration for GraalVM native image](./2/)
* [Class initialization strategy for GraalVM native image](./3/)
* [Smaller deployment options for GraalVM native image](./4/)
* [Deployment options for GraalVM native images](./5/)
* [Configuring memory used by GraalVM native images](./6/)
* [GC options for GraalVM native image](./7/)
* [Profile guided optimizations for GraalVM native image](./8/)

Extra sessions on GraalVM EE JIT Performance & Polyglot

* [For best peak performance use GraalVM with the JIT](./9/)
* [Exploring the GraalVM JIT](./95/)
* [Exploring the polyglot runtime of GraalVM](./97/)
