package patterns;

public class Pattern4{

    /*
    *
    * use one line for declaration
    * walkmod working
    * */

    class Test{
        int a;
        int b;
    }

    public int a = 1,b = 2,c = b,d;
    int e;


    public int calculate(){
        int sum1, sum2;
        int sum3;
        sum1 = a + b;
        sum2 = c + d;
        return sum1 - sum2;
    }

}
