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

    static Optional<Type> calculateMaximumCommonType(List<Type> types) {
        // we use a local class because we cannot use an helper static method in an interface
        class Helper {

            // Conceptually: given a type we start from the Element Type and get as many array levels as indicated
            // From the implementation point of view we start from the actual type and we remove how many array
            // levels as needed to get the target level of arrays
            // It returns null if the type has less array levels then the desired target
            private Optional<Type> toArrayLevel(Type type, int level) {
                if (level > type.getArrayLevel()) {
                    return Optional.empty();
                }
                for (int i = type.getArrayLevel(); i > level; i--) {
                    if (!(type instanceof ArrayType)) {
                        return Optional.empty();
                    }
                    type = ((ArrayType) type).getComponentType();
                }
                return Optional.of(type);
            }
        }
        Helper helper = new Helper();
        int level = 0;
        boolean keepGoing = true;
        // In practice we want to check for how many levels of arrays all the variables have the same type,
        // including also the annotations
        while (keepGoing) {
            final int currentLevel = level;
            // Now, given that equality on nodes consider the position the simplest way is to compare
            // the pretty-printed string got for a node. We just check all them are the same and if they
            // are we just just is not null
            Object[] values = types.stream().map(v -> {
                Optional<Type> t = helper.toArrayLevel(v, currentLevel);
                return t.map(Node::toString).orElse(null);
            }).distinct().toArray();
            if (values.length == 1 && values[0] != null) {
                level++;
            } else {
                keepGoing = false;
            }
        }
        return helper.toArrayLevel(types.get(0), --level);
    }
}
