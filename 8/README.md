# 08 : Profile Guided Optimizations for GraalVM Native Image

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

- [Native Image Profile Guided Optimisations](https://www.graalvm.org/reference-manual/native-image/PGO/)

## Overview

One of the great benefits of the JVM is the JIT (Just-In-Time) compiler. This allows a JVM to profile the code that is
being run, adapt to the data flowing through an application and to improve and tweak the performance. 
We shall continue to experiment on the example project we created before and look at how we can bring this profiling 
to Native Images. This is an **Enterprise Edition ONLY** feature.

## Our Application
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

## Profile Guide Optimisations 

One very interesting feature of the GraalVM native images is the ability to use profile guided optimizations to improve 
the performance of the native executables produced with native image.

In a similar fashion to the JIT compiler gathering the profile, we can build an instrumented image, apply the desired 
load to it (your load tests, benchmark suite, or a slice of real workload), collect the profile, and use it to build a 
more optimized executable for the particular workloads.

Here's how you do it:

1. Instrument your native image application so that it profiles the running code. These profiles are saved to the file system
2. Run a number of representative workloads against the running native image of the application. This will help to generate all of the profiling data
3. Use the saved profile data to build your next native image of the application

## Step 1 : Instrument Our Native Image of the Application

We need to supply the `--pgo-instrument` option to the native image build, then we build native image using the `--pgo` option.

For simplicity and consistency we'll edit the `build.gradle` manually as before, but the separated nature of the build (2 stages) nicely maps into the separate jobs in CI or scripting.

Edit the `build.gradle` to include the following:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_File_3647224_100.png)
```groovy
nativeImage {
  args("--pgo-instrument")
  // G1GC is currently only supported on Linux. 
  // For this example, as I don't want to do a docker based build, I will exclude it
  //args("--gc=G1")
}
```

Build the image:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./gradlew nativeImage
```

## Step 2 : Profile

Now run it and apply the load - this will allow the code to be profiled:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
build/native-image/application
```

The load can be shorter because we just need the profile:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
hey -z 30s http://localhost:8080/primes/random/100
```

Note: with instrumentation added, the app will run slower.

Stop the application with `Ctrl+C`, look at the profile file:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
ls -la default.iprof
```

## Step 3 : Build the PGO Native Image of the Application

We'll use it now to build the final image with the profile guided optimizations:

Edit the `build.gradle` (note you can have several profile files comma separated there):

Also note the `../..` it's because the building happens in the `build/native-image` directory.

![User Input](../images/noun_Computer_3477192_100.png)
![File](../images/noun_File_3647224_100.png)
```groovy
nativeImage {
  args("--pgo=default.iprof")
  //args("--gc=G1")
}
```

Build the image:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./gradlew nativeImage
```

Test it works:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
./build/native-image/application
```

## Packing as a Docker Image (on Linux)

Package it into the docker image. We can use the same `Dockerfile.slim` to build the image (the app on the host has 
changed):

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker build -f Dockerfile.slim -t primes-web:pgo .
```

Run this new docker image:

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
docker run --rm -p 8080:8080 --memory="256m" --memory-swap="256m" --cpus=1 primes-web:pgo
```

And apply the load as before:  

![User Input](../images/noun_Computer_3477192_100.png)
![Shell Script](../images/noun_SH_File_272740_100.png)
```bash
hey -z 60s http://localhost:8080/primes/random/100
```

Explore the output.

---
