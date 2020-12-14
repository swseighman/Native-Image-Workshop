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