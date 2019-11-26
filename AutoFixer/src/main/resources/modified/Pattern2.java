package patterns;

public class Pattern2 {

    /*
    * unnessasary parenthesis
    * walkmod working
    * */
    int a;

    int b;

    public Pattern2(int a, int b) {
        this.a = a + 1;
        this.b = 1;
    }

    public int compare(int x, int y) {
        if (x > y && a > b) {
            return 2;
        } else if (x > y && a < b) {
            return 1;
        } else {
            return 0;
        }
    }

    public Integer visit(final LineComment n, final Void arg) {
        return getMembers().stream().filter(m -> m instanceof CallableDeclaration).map(m -> ((CallableDeclaration<?>) m)).filter(m -> m.getSignature().equals(signature)).collect(toList());
        if (cu.getPackageDeclaration().isPresent() && (children.isEmpty() || PositionUtils.areInOrder(firstComment, cu.getPackageDeclaration().get()))) {
            cu.setComment(firstComment);
            comments.remove(firstComment);
            int result = extendsBound ? 1 : 0;
            res = a && b || c;
            return pos < source.length() ? source.substring(0, pos) : source;
        } else {
            return n.getComment().isPresent() ? n.getComment().get().accept(this, arg) : 0;
        }
    }
}
