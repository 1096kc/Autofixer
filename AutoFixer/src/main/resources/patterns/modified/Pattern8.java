package patterns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Pattern8 {

    public void test() {
        try {
            FileInputStream fin = new FileInputStream(new File("./a.txt"));
        } catch (FileNotFoundException e) {
            // an exception happened
            System.out.print("");
        }
    }

    public void test1() {
        try {
            FileInputStream fin = new FileInputStream(new File("./a.txt"));
        } catch (FileNotFoundException e) {
            System.out.println(1111);
        }
    }
}
