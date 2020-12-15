# Class Initialization Strategy for GraalVM Native Image

<div class="inline-container">
<img src="../images/noun_Stopwatch_14262_100.png">
<strong>
  Estimated time: 15 minutes
</strong>
</div>

## Overview

One of the most misunderstood features of native image is the class initialization strategy.
Here we'll try to explain it a little.

* Classes need to be initialized before they can be used
* The lifecycle of native image is split into 
two parts:
  * Build time
  * Run time.
* Known **safe** classes are initialised at Build Time
* By default other classes are initialized at runtime
* But you can initialize at runtime - if you want to

We will look into this in more detail in the following.

## An Example

Let's explore an example application that consists of a few classes to better understand the implications of initialisation at
`run-time` or `build-time` and how we can configure the initialisation strategy.

Here's our program - you can create these files to follow along:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_java_825609_100.png)
```Java
import java.nio.charset.*;


public class Main0 {

  public static void main(String[] args) {
    A.b.doit();

  }
}


class A {

  public static B b = new B();

}

class B {

  private static final Charset UTF_32_LE = Charset.forName("UTF-32LE");

  public void doit() {
    System.out.println(UTF_32_LE);
  }
}
```

It consists of 3 classes: Main, calling `A.b.doit()`; A, holding a reference to a B instance in a static field; and B, 
holding a reference to a Charset - `UTF-32LE`.

Run it:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
# Compile it
javac Main0.java

# And run it
java Main0
```

Now build a native image of it, run it, and take a look at what happens:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
native-image -cp . Main0
./main0
```

It breaks, with the following exception, because the Charset `UTF_32_LE` is not by default included in the native image  and is thus not found at runtime. **Note** Not all Charsets are added to native images, adding them is a deliberate step and this helps to reduce image size:

![User Input](../images/noun_protest_sign_2029359_100.png)
```Java
Exception in thread "main" java.lang.ExceptionInInitializerError
	at com.oracle.svm.core.classinitialization.ClassInitializationInfo.initialize(ClassInitializationInfo.java:291)
	at A.<clinit>(Main.java:15)
	at com.oracle.svm.core.classinitialization.ClassInitializationInfo.invokeClassInitializer(ClassInitializationInfo.java:351)
	at com.oracle.svm.core.classinitialization.ClassInitializationInfo.initialize(ClassInitializationInfo.java:271)
	at Main.main(Main.java:7)
Caused by: java.nio.charset.UnsupportedCharsetException: UTF-32LE
	at java.nio.charset.Charset.forName(Charset.java:529)
	at B.<clinit>(Main.java:21)
	at com.oracle.svm.core.classinitialization.ClassInitializationInfo.invokeClassInitializer(ClassInitializationInfo.java:351)
	at com.oracle.svm.core.classinitialization.ClassInitializationInfo.initialize(ClassInitializationInfo.java:271)
	... 4 more
```

One way to resolve this issue is to include all charsets:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
native-image -H:+AddAllCharsets -cp . Main0
```

## Can We See What Gets Initialised at Build Time?

We're more interested in the class init details right now and adding all of the charsets may not be the best way 
to solve our problem. Let's use the `-H:+PrintClassInitialization` to check how the classes are initialized:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
native-image -H:+PrintClassInitialization -cp . Main0
```

Check the output:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
cat reports/run_time_classes_*
```

You can for example see classes such as:

![User Input](../images/noun_File_3647224_100.png)
```SH
com.oracle.svm.core.containers.cgroupv1.CgroupV1Subsystem
com.oracle.svm.core.containers.cgroupv2.CgroupV2Subsystem
```

Which are used for determining the V1/V2 cgroup resources availability when running in containers.

And also our classes `A` and `B`. Initializing the class means running it's `<clinit>` so it tries to load the charset and it breaks at runtime.

## Moving to Build Time Initialisation

What if we move the initialization of these classes to build time? This will succeed because build time is a Java process and it'll load the charset without any problems - all of the charsets are avilable to the Java process that is doing the native image build.

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
native-image --initialize-at-build-time=A,B -cp . Main0
```

The classes are initialized at build time, the Chatset instance is written out to the image heap and can be used at runtime.

Run the native image application again, in order to confirm that this now does work as expected:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./main0
```

## When You Can't Initialise at Runtime

Sometimes objects instantiated during the build class initialization cannot be initialised ar `build-time` and written to the image heap.

When classes contain any of the following, they can't be written to the image heap:

* _Opened files_
* _Running threads_
* _Opened network sockets_
* _Random instances_

If the analysis sees them in the image heap - it'll notify you and ask you to initalize the classes holding them at 
runtime, but be aware that there may be a chain of dependencies that caused that class to be initialised at `build-time`.

For example, if we modify the code to be as below, what will happen when we build?

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_java_825609_100.png)
```Java
import java.nio.charset.*;


public class Main1 {

  public static void main(String[] args) {
    A.b.doit();
    System.out.println(A.t);
  }
}


class A {

  public static B b = new B();

  public static Thread t;

  static {
    // Oh no! We added a Thread
    // This is something that can't go onto the Image Heap
    t = new Thread(()-> {
      try {
        Thread.sleep(30_000);
      } catch (Exception e){}
    });
    t.start();
  }

}

class B {

  private static final Charset UTF_32_LE = Charset.forName("UTF-32LE");

  public void doit() {
    System.out.println(UTF_32_LE);
  }
}
```

Building the native image like before will now fail, but please notice the build error:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
native-image --no-fallback --initialize-at-build-time=A,B -cp . Main1
```

This is the error you will see:

![User Input](../images/noun_protest_sign_2029359_100.png)
```Java
Error: Detected a started Thread in the image heap. Threads running in the image generator are no longer running at image run time.  To see how this object got instantiated use -H:+TraceClassInitialization. The object was probably created by a class initializer and is reachable from a static field. You can request class initialization at image run time by using the option --initialize-at-run-time=<class-name>. Or you can write your own initialization methods and call them explicitly from your main entry point.
Detailed message:
Trace: Object was reached by
	reading field A.t

Error: Use -H:+ReportExceptionStackTraces to print stacktrace of underlying exception
Error: Image build request failed with exit status 1```
```

## The Solution, in This Case

Balancing initialization can be a bit tricky, so by default GraalVM initializes classes at runtime. So for this example 
it's good to have initialize only `B` at build time.

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
native-image --no-fallback --initialize-at-build-time=B -cp . Main1
```
This now builds and we can run it. Running it will take 30 seconds now because of the added `Thread.sleep`

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./main1
UTF-32LE
Thread[Thread-0,5,main]
~/init-strategy
```

Correspondingly, you can use the `--initialize-at-run-time=package.C1` option to make classes init at runtime. 

Next, we'll try to explore various deployment options for native images.

---
<a href="../4/">
    <img src="../images/noun_Next_511450_100.png"
        style="display: inline; height: 6em;" />
</a>

