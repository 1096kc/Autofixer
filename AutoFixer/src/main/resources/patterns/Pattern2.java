public class Pattern2{

    /*
    * unnessasary parenthesis
    * walkmod working
    * */
    int a;
    int b;

    public Pattern2(int a, int b){
        this.a = ((a + 1));
        this.b = (1);
    }

    public int compare(int x, int y){
        if (((x > y)) && (a > b)) {
            return 2;
        } else if ((x > y) && (a < b)) {
            return 1;
        } else {
            return 0;
        }
    }
}
