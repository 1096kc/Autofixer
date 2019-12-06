package patterns;

public class Pattern4 {

    /*
    *
    * use one line for declaration
    * walkmod working
    * */
    class Test {

        int a;

        int b;
    }

    public int a = 1;

    public int b = 2;

    public int c = b;

    public int d;

    int e;

    public int calculate() {
        int sum1;
        int sum2;
        int sum3;
        sum1 = a + b;
        sum2 = c + d;
        return sum1 - sum2;
    }
}
