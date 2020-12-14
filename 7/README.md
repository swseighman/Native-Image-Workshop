# GC options for GraalVM native image

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

In the previous parts of this workshop we've built a small microservice that can respond to HTTP traffic.

Let's add to this and see how we what options for GC GraalVM has.

<img src="../images/noun_Book_3652476_100.png"
     style="display: inline; height: 2.5em;">
<strong style="margin: 0;
  position: absolute;
  top: 50%;
  -ms-transform: translateY(-60%);
  transform: translateY(-60%);">
References:
</strong>

- [Native Image : Memory Management at Image Runtime](https://www.graalvm.org/reference-manual/native-image/MemoryManagement/)

## Update our Application

Locate the sample project we created:

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

If necessary, build the native image of it again:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
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
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./build/native-image/application &
```

Run the load tool for 60s to get the measurements.

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
hey -z 60s http://localhost:8080/primes/random/100
```

## Stressing Our Dockerised Application

However it makes sense to limit the available resources to simulate more cloud like deployment.

We'll use the docker image we've built before.

Run the following command to run the native image of our application:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
docker run --rm -p 8080:8080 --memory="256m" --memory-swap="256m" --cpus=1 primes-web:slim
```

Note we're restricting the memory to 256m, disable the swap and limit it to have 1 CPU. It's a pretty 
constrained environment. Now you can get the measurement, using the similar `hey` command:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
hey -z 60s http://localhost:8080/primes/random/100
```

## Making Use of G1GC

Edit the `build.gradle` file to include the following `nativeImage` section:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
nativeImage {
  args("--gc=G1")
}
```

Run the build:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./gradlew nativeImage
```

In this new native image we have enabled the `G1GC` Garbage Collector.
 
We can use the same `Dockerfile.slim` to build the image (the app on the host has changed):

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
docker build -f Dockerfile.slim -t primes-web:g1gc .
```

Run this new docker image:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
docker run --rm -p 8080:8080 --memory="256m" --memory-swap="256m" --cpus=1 primes-web:g1gc
```

And apply the load as before:  

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
hey -z 60s http://localhost:8080/primes/random/100
```

Explore the output.

\* Add the printGC / verboseGC flags we looked at in the previous chapter to the g1gc based image to look 
at the heap configuration. 

Next, we'll try to explore how to improve performance of the native images.

---
<a href="../8/README.md">
    <img src="../images/noun_Next_511450_100.png"
        style="display: inline; height: 6em;" />
</a>
