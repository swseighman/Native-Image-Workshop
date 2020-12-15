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
