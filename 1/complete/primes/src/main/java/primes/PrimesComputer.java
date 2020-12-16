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

    /**
     * If n is not a prime, then n = a * b (some a & b)
     * Both a and b can't be larger than sqrt(n), or the product of them would be greater than n
     * One of the factors has to be smaller than sqrt(n).
     * So, if we don't find any factors by the time we get to sqrt(n), then n must be prime
     */
    public static boolean isPrime(long n) {
        return LongStream.rangeClosed(2, (long) Math.sqrt(n))
            .allMatch(i -> n % i != 0);
    }
}
