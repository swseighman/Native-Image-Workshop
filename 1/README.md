# Building a Simple GraalVM Native Image

<img src="../images/noun_Stopwatch_14262.png"
     style="display: inline; height: 2.5em;">
<strong style="margin: 0;
  position: relative;
  top: 10px;
  -ms-transform: translateY(-60%);
  transform: translateY(-60%);">
  Estimated time: 15 minutes
</strong>


## Overview
GraalVM native image can process your application compiling it ahead of time into a standalone executable.

Some of the benefits you get from it are:

* **Small** standalone distribution, not requiring a JDK
* **Instant Startup**
* **Lower memory** footprint

Let's take a look...

<img src="../images/noun_Book_3652476_100.png"
     style="display: inline; height: 2.5em;">
<strong style="margin: 0;
  position: absolute;
  top: 50%;
  -ms-transform: translateY(-60%);
  transform: translateY(-60%);">
References:
</strong>

- [Native Image : Build a Native Image](https://www.graalvm.org/reference-manual/native-image/#build-a-native-image)

## Creating a Micronaut Application

We'll create a Micronaut application to compute sequences of prime numbers & then we will see how we can easily create 
a fast starting Native Image form this Java application.

First, let's make sure that we have (and they are the latest versions) the tools we need:

- Micronaut ([Install](https://micronaut.io/download.html) Micronaut using SDKMan)
- GraalVM EE, 20.3.0

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
# Check Micronaut version
mn --version
Micronaut Version: 2.2.0
```

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
# Check Java version
java --version
java 11.0.9 2020-10-20 LTS
Java(TM) SE Runtime Environment GraalVM EE 20.3.0 (build 11.0.9+7-LTS-jvmci-20.3-b06)
Java HotSpot(TM) 64-Bit Server VM GraalVM EE 20.3.0 (build 11.0.9+7-LTS-jvmci-20.3-b06, mixed mode, sharing)
```

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
# Check that we have Native Image installed as well
native-image --version
GraalVM Version 20.3.0 EE (Java Version 11.0.9+7-LTS-jvmci-20.3-b06)
```

Now, create the application using Micronaut!

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
mn create-cli-app primes; cd primes
```

## Adding Functionality to Our App

Create and edit the `src/main/java/primes/PrimesComputer.java` file:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_java_825609_100.png)
```Java
package primes;

import javax.inject.Singleton;
import java.util.stream.*;
import java.util.*;

@Singleton
public class PrimesComputer {
    private Random r = new Random(41);

    public List<Long> random(int upperbound) {
        int to = 2 + r.nextInt(upperbound - 2);
        int from = 1 + r.nextInt(to - 1);
        return primeSequence(from, to);
    }

    public static List<Long> primeSequence(long min, long max) {
        return LongStream.range(min, max)
            .filter(PrimesComputer::isPrime)
            .boxed()
            .collect(Collectors.toList());
    }

    public static boolean isPrime(long n) {
        return LongStream.rangeClosed(2, (long) Math.sqrt(n))
                .allMatch(i -> n % i != 0);
    }
}
```

Edit the `src/main/java/primes/PrimesCommand.java` file:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_java_825609_100.png)
```Java
package primes;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import javax.inject.*;
import java.util.*;

@Command(name = "primes", description = "...",
        mixinStandardHelpOptions = true)
public class PrimesCommand implements Runnable {
    @Option(names = {"-n", "--n-iterations"}, description = "How many iterations to run")
    int n;

    @Option(names = {"-l", "--limit"}, description = "Upper limit for the sequence")
    int l;

    @Inject
    PrimesComputer primesComputer;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(PrimesCommand.class, args);
    }

    public void run() {
        for(int i =0; i < n; i++) {
            List<Long> result = primesComputer.random(l);
            System.out.println(result);
        }
    }
}
```

Remove the tests because we changed the functionality of the main command:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
rm src/test/java/primes/PrimesCommandTest.java
```

## Build the Java Application

Now we can build this Micronaut project to get the jar file with our functionality:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./gradlew build
```

Test the application that it prints the prime numbers:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
java -jar build/libs/primes-0.1-all.jar -n 1 -l 100
[53, 59, 61, 67, 71, 73]
```

## Build a Native Image

Now you can build the native image too:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
./gradlew nativeImage
```

Micronaut includes a Gradle plugin to invoke the `native-image` utility and configure its execution.

You can find the resulting executable in `build/native-image/application`.

### Let's take a look at the Native App

Inspect it with the `ldd` utility and check that it's linked to the OS libraries.

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
# On linux
ldd build/native-image/application
```
If you are on a mac, then you will need to use the `otool`, as below:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
# On OsX
otool -l build/native-image/application
```
Take a look at its file type:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
file build/native-image/application
```

## Comparing the Startup Time & Performance of JIT & Native Image

If you have GNU time utility (`brew install gnu-time` on Macos), you can time the execution and the memory usage of the process. Compare the following:

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
/usr/bin/time -v java -jar build/libs/primes-0.1-all.jar -n 1 -l 100
```
vs.

![User Input](../images/noun_Computer_3477192_100.png)
![User Input](../images/noun_SH_File_272740_100.png)
```SH
/usr/bin/time -v build/native-image/application -n 1 -l 100
```

Next, we'll try to explore some more options how to configure the build process for native images.

---
<a href="../2/README.md">
    <img src="../images/noun_Next_511450_100.png"
        style="display: inline; height: 6em;" />
</a>
