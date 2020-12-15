# Deployment Options for GraalVM Native Images

<img src="../images/noun_Stopwatch_14262.png"
     style="display: inline; height: 2.5em;">
<strong style="margin: 0;
  position: absolute;
  top: 50%;
  -ms-transform: translateY(-60%);
  transform: translateY(-60%);">
  Estimated time: 15 minutes
</strong>


## Overview

In the previous part we've build a small microservice that can respond to the HTTP traffic.
Let's continue using it and try to explore some options how we can deploy it differently.

---
![User Input](../images/noun_bulb_1912576_100.png)

## A Note on Building on OSX

If you use a Mac you will need to build your native images inside a linux Docker container if you want to deploy
them inside Docker containers. If you stop and think about it for a second, that makes perfect sense, right? You 
build on a mac, you get a mac executable.

You will, from time to time, forget this and then you will see the following error when you deploy your app into a docker
container:

![User Input](../images/noun_bulb_1912576_100.png)
![User Input](../images/noun_protest_sign_2029359_100.png)
```text
standard_init_linux.go:211: exec user process caused "exec format error"
```
---

## Building Our Web Micro-Service

Locate the sample project we created - there's a copy of this in the current folder:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
cd primes-web
```

If you haven't done it yet, build the app:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./gradlew build
```

Build the native image of it again (let's not use the upxed version, if you used the `-k` option to keep the backup, the 
executable should be in `build/native-image/application.~`).

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./gradlew nativeImage
```

And locate the binary of the app (if needed move the upx backup file: `mv build/native-image/application.~ build/native-image/application`):

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./build/native-image/application
```

Explore the output of `ldd` to check which libraries it's linked against:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
ldd ./build/native-image/application
```

## Packing in a Docker Container

Let's package it into a docker image. One of the benefits of the native image is that it offers smaller deployment 
sizes, so we'll use a `slim` image.

Create a `Dockerfile.slim` file with the following:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_Cloud_Docker_676618_100.png)
```dockerfile
FROM oraclelinux:8-slim
COPY build/native-image/application app
ENTRYPOINT ["/app"]
```

Build the image:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
docker build -f Dockerfile.slim -t primes-web:slim .
```

Install the `dive` utility to inspect the images: https://github.com/wagoodman/dive#installation

Run it on the new image and explore the output:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
dive primes-web:slim
```

The image doesn't have much except our application. We can still run it and access the app on port 8080.

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
docker run --rm -p 8080:8080 primes-web:slim
```

## Tiny Docker Containers

However, we can do even better. One way is to link all necessary libraries statically, except `libc`. Which we'll 
depend on from the environment.

Edit the `build.gradle` file to configure the native-image use through the Micronaut's Gradle plugin. Add the 
following section:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_File_3647224_100.png)
```groovy
nativeImage {
  args("-H:+StaticExecutableWithDynamicLibC")
}
```

Build it again and explore the output of `ldd`:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./gradlew nativeImage
```

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SHldd ./build/native-image/application
```

We will use Distroless as our base. Distroless is a very minimal container distro. We can now package our app into an 
even smaller docker image. Let's call this `Dockerfile.distroless`

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_Cloud_Docker_676618_100.png)
```dockerfile
FROM gcr.io/distroless/base
COPY build/native-image/application app
ENTRYPOINT ["/app"]
```

Build the image and explore it with `dive`:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
docker build -f Dockerfile.distroless -t primes-web:distroless .
```

Look at the image efficiency score!

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
dive primes-web:distroless
```

Running it is just as simple:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SHdocker run --rm -p 8080:8080 primes-web:distroless
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
