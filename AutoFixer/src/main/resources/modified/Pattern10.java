package patterns;

public class Pattern10 {

    public void test() {
        int x = 2;
        int y = 4;
        x = getX();
        y = getY();
        if (x == y) {
            System.out.println("3!");
        }
    }

    public int getX() {
        return 3;
    }

    public int getY() {
        return 4;
    }
}
