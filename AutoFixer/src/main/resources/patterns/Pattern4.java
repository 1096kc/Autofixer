public class Pattern4{

    /*
    *
    * use one line for declaration
    * walkmod working
    * */
    int a,b,c,d;
    public Pattern4(int a,int b, int c,int d){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public int calculate(){
        int sum1, sum2;
        sum1 = a + b;
        sum2 = c + d;
        return sum1 - sum2;
    }
}
