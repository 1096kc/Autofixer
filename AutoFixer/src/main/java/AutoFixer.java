import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.printer.YamlPrinter;
import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.text.WordUtils;

import java.io.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AutoFixer {

    private CompilationUnit compilationUnit;
    private String inputFile;
    private String outPutFile;

    public AutoFixer(String fileName) throws FileNotFoundException {
        File file = new File(fileName);
        inputFile = fileName;
        outPutFile = file.getParent().concat("/../modified/").concat(file.getName());
        compilationUnit = StaticJavaParser.parse(file);
    }

    public void print(){
        YamlPrinter printer = new YamlPrinter(true);
        System.out.println(printer.output(compilationUnit));
    }

    public void write(){
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(outPutFile));
            fileOutputStream.write((compilationUnit + "").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    *
    * For pattern1
    * Place string literals ahead
    *
    * */
    public void checkStringEquals(){
        compilationUnit.findAll(MethodDeclaration.class).forEach(method -> method.walk(node -> {
            if (node instanceof MethodCallExpr && ((MethodCallExpr) node).getName().getIdentifier().equals("equals")) {
                AtomicBoolean caller = new AtomicBoolean();
                AtomicReference<Optional<Expression>> callerString = new AtomicReference<>();
                caller.setOpaque(false);
                ((MethodCallExpr) node).getScope().get().getChildNodes().forEach(
                        child -> {
                            if (child instanceof SimpleName) {
                                caller.setOpaque(true);
                                callerString.setOpaque(((MethodCallExpr) node).getScope());
                            }
                        }
                );
                AtomicBoolean callee = new AtomicBoolean();
                AtomicReference<Expression> calleeString = new AtomicReference<>();
                callee.setOpaque(false);
                ((MethodCallExpr) node).getArguments().forEach(child -> {
                    if (child instanceof StringLiteralExpr) {
                        callee.setOpaque(true);
                        calleeString.setOpaque(((MethodCallExpr) node).getArgument(0));

                    }
                });
                if (caller.getOpaque() && callee.getOpaque()) {
                    ((MethodCallExpr) node).setScope(calleeString.get());
                    ((MethodCallExpr) node).setArgument(0,callerString.get().get());
                }

            }
        }));
    }

    /*
    *
    * For pattern2
    * useless parenthesis
    *
    * */
    public void checkUselessParenthesis(){
        // remove duplicated parenthesis
        compilationUnit.walk(node -> {
            if (node instanceof EnclosedExpr) {
                while (((EnclosedExpr) node).getInner() instanceof EnclosedExpr) {
                    ((EnclosedExpr) node).setInner(((EnclosedExpr) ((EnclosedExpr) node).getInner()).getInner());
                }
            }
        });

        // remove parenthesis for binary expression
        compilationUnit.walk(node -> {
            if (node instanceof BinaryExpr) {
                Expression left = ((BinaryExpr) node).getLeft();
                Expression right = ((BinaryExpr) node).getLeft();
                if (left instanceof EnclosedExpr) {
                    ((BinaryExpr) node).setLeft(((EnclosedExpr) left).getInner());
                }
                if (right instanceof EnclosedExpr) {
                    ((BinaryExpr) node).setRight(((EnclosedExpr) right).getInner());
                }
            }
        });

        System.out.println(compilationUnit);
    }

    /*
    *
    * For pattern3
    * use equals for object comparision
    *
    * */
    private void checkForClassEquals() {

        // Get all class definition
        Set<String> classDeclaration = new HashSet<>();
        compilationUnit.walk(node -> {
            if (node instanceof ClassOrInterfaceDeclaration) {
                classDeclaration.add(((ClassOrInterfaceDeclaration) node).getName().getIdentifier());
            }
        });

        // Get all the filed instances
        Set<String> filedInstanceSet = new HashSet<>();
        compilationUnit.findAll(FieldDeclaration.class).forEach(fieldDeclaration -> {
            fieldDeclaration.getVariables().forEach(variable -> {
                if (classDeclaration.contains(variable.getTypeAsString())) {
                    filedInstanceSet.add(variable.getName().getIdentifier());
                }
            });
        });

        // check in every method, if there's a violation
        compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
            Set<String> methodInstanceSet = new HashSet<>();
            methodDeclaration.getParameters().forEach(param -> {
                if (classDeclaration.contains(param.getTypeAsString())) {
                    methodInstanceSet.add(param.getName().getIdentifier());
                }
            });
            methodDeclaration.walk(child -> {
                // change == to equals
                if (child instanceof BinaryExpr && ((BinaryExpr) child).getOperator().equals(BinaryExpr.Operator.EQUALS)) {
                    Expression left = ((BinaryExpr) child).getLeft();
                    Expression right = ((BinaryExpr) child).getRight();
                    if ((left instanceof NameExpr && (filedInstanceSet.contains(((NameExpr)left).getName().getIdentifier()) || methodInstanceSet.contains(((NameExpr)left).getName().getIdentifier())))
                            || (right instanceof NameExpr && (filedInstanceSet.contains(((NameExpr)right).getName().getIdentifier()) || methodInstanceSet.contains(((NameExpr)right).getName().getIdentifier())))) {
                        NodeList<Expression> arguments = new NodeList<>();
                        arguments.add(right);
                        Node temp = new MethodCallExpr(left,"equals",arguments);
                        child.getParentNode().get().replace(child,temp);
                    }
                }

                // change != to !equals()
                if (child instanceof BinaryExpr && ((BinaryExpr) child).getOperator().equals(BinaryExpr.Operator.NOT_EQUALS)) {
                    Expression left = ((BinaryExpr) child).getLeft();
                    Expression right = ((BinaryExpr) child).getRight();
                    if ((left instanceof NameExpr && (filedInstanceSet.contains(((NameExpr)left).getName().getIdentifier()) || methodInstanceSet.contains(((NameExpr)left).getName().getIdentifier())))
                            || (right instanceof NameExpr && (filedInstanceSet.contains(((NameExpr)right).getName().getIdentifier()) || methodInstanceSet.contains(((NameExpr)right).getName().getIdentifier())))) {
                        NodeList<Expression> arguments = new NodeList<>();
                        arguments.add(right);
                        Node temp = new MethodCallExpr(left, "equals", arguments);
                        Node node = new UnaryExpr((Expression) temp, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
                        child.getParentNode().get().replace(child, node);
                    }
                }
            });
        });
    }

    /*
    *
    * For pattern4
    * one line for one declaration
    *
    * */
    public void checkSingleLineDeclaration() {

    }

    /*
    *
    * For pattern5
    * check camel case
    *
    *
    * */
    public void checkCamelCase() {
        compilationUnit.walk(node -> {
            if (node instanceof SimpleName) {
                String origin = ((SimpleName) node).getIdentifier();
                String modify = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,origin);
                ((SimpleName) node).setIdentifier(modify);
            }
        });
    }

    public static void main(String[] args) throws FileNotFoundException {
        AutoFixer fixer = new AutoFixer("./AutoFixer/src/main/resources/patterns/Pattern1.java");
        fixer.print();
        fixer.checkStringEquals();
        fixer.write();
    }

}
