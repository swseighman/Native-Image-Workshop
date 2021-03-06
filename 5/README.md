# 05 : Deployment Options for GraalVM Native Images

<div class="inline-container">
<img src="../images/noun_Stopwatch_14262_100.png">
<strong>
  Estimated time: 15 minutes
</strong>
</div>

## Overview

In the previous part we've build a small microservice that can respond to the HTTP traffic.
Let's continue using it and try to explore some options how we can deploy it differently.

---
![Note](../images/noun_bulb_1912576_100.png)

## A Note on Building on OSX

If you use a Mac you will need to build your native images inside a linux Docker container if you want to deploy
them inside Docker containers. If you stop and think about it for a second, that makes perfect sense, right? You 
build on a mac, you get a mac executable.

You will, from time to time, forget this and then you will see the following error when you deploy your app into a docker
container:

![Note](../images/noun_bulb_1912576_100.png)
![Error Message](../images/noun_protest_sign_2029359_100.png)
```text
standard_init_linux.go:211: exec user process caused "exec format error"
```
---

## Building Our Web Micro-Service

Locate the sample project we created - there's a copy of this in the current folder:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
cd primes-web
```

If you haven't done it yet, build the app:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./gradlew build
```

Build the native image of it again (let's not use the upxed version, if you used the `-k` option to keep the backup, the 
executable should be in `build/native-image/application.~`).

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./gradlew nativeImage
```

And locate the binary of the app (if needed move the upx backup file: `mv build/native-image/application.~ build/native-image/application`):

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./build/native-image/application
```

Explore the output of `ldd` to check which libraries it's linked against:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
ldd ./build/native-image/application
```

## Packing in a Docker Container

Let's package it into a docker image. One of the benefits of the native image is that it offers smaller deployment 
sizes, so we'll use a `slim` image.

**NOTE**: We will use a docker based build, as on a mac the binary built will not work when added to the linux
docker container - see the note earlier. We will use a 2-step docker build. The first will build the binary and the
second will copy the binary from the image built in step 1.

Create a `Dockerfile.slim` file with the following:

![User Input](../images/noun_Computer_3477192_100.png)
![Docker](../images/noun_Cloud_Docker_676618_100.png)
```dockerfile
FROM oracle/graalvm-ce:20.3.0-java11 AS builder

# Here we are using GraalVM CE - but you don't have to.
# There is not yet a public GraalVM EE docker image that you can download,
# but you can make your own easily

RUN gu install native-image
RUN mkdir app
COPY . /app
WORKDIR app

# Build the Jar
RUN ./gradlew build

# Build the Native Image
RUN ./gradlew nativeImage

# Step 2 : build the image that will run

FROM oraclelinux:8-slim
COPY --from=builder app/build/native-image/application app
ENTRYPOINT ["/app"]
```

Build the image:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker build -f Dockerfile.slim -t primes-web:slim .
```

Install the `dive` utility to inspect the images: https://github.com/wagoodman/dive#installation

Run it on the new image and explore the output:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
dive primes-web:slim
```

The image doesn't have much except our application. We can still run it and access the app on port 8080.

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker run --rm -p 8080:8080 primes-web:slim

# visit the following URL in your browser
# http://localhost:8080/primes/random/200
```

## Tiny Docker Containers

However, we can do even better. One way is to link all necessary libraries statically, except `libc`. Which we'll 
depend on from the environment.

Edit the `build.gradle` file to configure the native-image use through the Micronaut's Gradle plugin. Add the 
following section:

![User Input](../images/noun_Computer_3477192_100.png)
![File](../images/noun_File_3647224_100.png)
```groovy
nativeImage {
  args("-H:+StaticExecutableWithDynamicLibC")
}
```

We will use Distroless as our base. Distroless is a very minimal container distro. We can now package our app into an 
even smaller docker image. Let's call this `Dockerfile.distroless`

![User Input](../images/noun_Computer_3477192_100.png)
![Docker](../images/noun_Cloud_Docker_676618_100.png)
```dockerfile
FROM oracle/graalvm-ce:20.3.0-java11 AS builder

# Here we are using GraalVM CE - but you don't have to.
# There is not yet a public GraalVM EE docker image that you can download,
# but you can make your own easily

RUN gu install native-image
RUN mkdir app
COPY . /app
WORKDIR app

# Build the Jar
RUN ./gradlew build
# Build the Native Image
RUN ./gradlew nativeImage

FROM gcr.io/distroless/base
COPY --from=builder app/build/native-image/application app
ENTRYPOINT ["/app"]
```

Build the image and explore it with `dive`:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker build -f Dockerfile.distroless -t primes-web:distroless .
```

Look at the image efficiency score!

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
dive primes-web:distroless
```

Running it is just as simple:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker run --rm -p 8080:8080 primes-web:distroless
```

Use the app a few times, explore the docker container stats in `docker stats` or some monitoring service like `cadvisor`.

## Bonus Points!

As a bonus exercise you can build a statically linked native image which is completely self-contained and can be used 
in the empty docker image: `FROM scratch`. This is possible on Linux only, and requires some prerequisites described in 
[the docs](https://www.graalvm.org/reference-manual/native-image/StaticImages/).

### TLDR;

In a nutshell you can pass `--static --libc=musl` and get a statically linked binary.

Please take your time to explore it.

Next, we'll try to explore various options for configuring the runtime for native images.

---
<a href="../6/">
    <img src="../images/noun_Next_511450_100.png"
        style="display: inline; height: 6em;" />
</a>
