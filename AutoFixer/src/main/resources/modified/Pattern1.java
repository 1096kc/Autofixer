public class Pattern1 {

    public static void main(String[] args) {
        /*
         * comparing string literals with string objects
         * walkmod not working
         */
        String a = "b";
        if ("b".equals(a))
            System.out.println(1111);
        else if ("a".equals(a)) {
            System.out.println(222);
        }
    }
}
