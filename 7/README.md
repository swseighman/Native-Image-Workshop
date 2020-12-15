# 07 : GC options for GraalVM native image

<div class="inline-container">
<img src="../images/noun_Stopwatch_14262_100.png">
<strong>
  Estimated time: 15 minutes
</strong>
</div>

<div class="inline-container">
<img src="../images/noun_Book_3652476_100.png">
<strong>
References:
</strong>
</div>

- [Native Image : Memory Management at Image Runtime](https://www.graalvm.org/reference-manual/native-image/MemoryManagement/)

## Overview

In the previous parts of this workshop we've built a small microservice that can respond to HTTP traffic.

Let's add to this and see how we what options for GC GraalVM has.

## Update our Application

Locate the sample project we created:

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

If necessary, build the native image of it again:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./gradlew nativeImage
```

And locate the binary of the app at `./build/native-image/application`.

## Stressing Our Application

Let's run the application and apply some load to it so we can explore how it behaves under this. We'll use 
the `hey` load generation tool (https://github.com/rakyll/hey).

There are binaries on the github page, or you can install with `go get hey` (if you have go).

You can run the application as is on the host machine, for example:

Run your app (use `&` at the end to make it run in the background or use 2 terminals/connections to run 
commands in parallel):

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./build/native-image/application &
```

Run the load tool for 60s to get the measurements.

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
hey -z 60s http://localhost:8080/primes/random/100
```

## Stressing Our Dockerised Application

However it makes sense to limit the available resources to simulate a more cloud like deployment.

We'll use the docker image we've built before.

Run the following command to run the native image of our application:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker run -d --rm -p 8080:8080 --memory="256m" --memory-swap="256m" --cpus=1 primes-web:slim
```

Note we're restricting the memory to 256m, disable the swap and limit it to have 1 CPU. It's a pretty 
constrained environment. Now you can get the measurement, using the similar `hey` command:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
hey -z 60s http://localhost:8080/primes/random/100
```

## Making Use of G1GC

Edit the `build.gradle` file to include the following `nativeImage` section:

![User Input](../images/noun_Computer_3477192_100.png)
![File](../images/noun_File_3647224_100.png)
```groovy
nativeImage {
  args("--gc=G1")
}
```

In this new native image we have enabled the `G1GC` Garbage Collector.

---
![User Input](../images/noun_bulb_1912576_100.png)

The build now requires an **Enterprise Edition ONLY** feature. As the Enterprise Edition is licensed under the OTN
license, we are not able to redistribute it, so we can't build a docker image that contains Enterprise Edition and 
upload it to Dockerhub for public download. We will therefore need to build our own Enterprise Edition Docker image.
This is not very difficult and in my docker file fo rthe builds I will be referencing my own private docker image.

Please note that you will need to add your own base image ot the docker file to build this.

--- 

Run the build - we are using a 2-step docker file to do the build:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker build -f Dockerfile.slim -t primes-web:g1gc .
```

Run this new docker image:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker run -d --rm -p 8080:8080 --memory="512m" --memory-swap="512m" --cpus=1 primes-web:g1gc
```

And apply the load as before:  

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
hey -z 60s http://localhost:8080/primes/random/100
```

Explore the output.

\* Add the printGC / verboseGC flags we looked at in the previous chapter to the g1gc based image to look 
at the heap configuration. 

Next, we'll try to explore how to improve performance of the native images.

---
<a href="../8/">
    <img src="../images/noun_Next_511450_100.png"
        style="display: inline; height: 6em;" />
</a>
