package patterns;

/*
*
* missing braces
*
* */
public class Pattern6 {

    public void testIfElse(int a) {
        if (a > 10) {
            System.out.println(111);
        } else {
            System.out.println(2222);
        }
    }

    public void testFor(int a) {
        for (int i = 0; i < a; i++) {
            System.out.println(1111);
        }
    }

    public void testWhile(int a) {
        int i = 0;
        while (i++ < a) {
            System.out.println(1111);
        }
    }
}
