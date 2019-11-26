import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.printer.YamlPrinter;
import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.text.WordUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AutoFixer {

    private CompilationUnit compilationUnit;
    private String inputFile;
    private String outPutFile;

    public AutoFixer() {
    }

    public AutoFixer(String inputFile,String outPutFile) throws Exception {
        this.inputFile = inputFile;
        this.outPutFile = outPutFile;
        init();
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getOutPutFile() {
        return outPutFile;
    }

    public void setOutPutFile(String outPutFile) {
        this.outPutFile = outPutFile;
    }

    public void init() throws Exception{
        File file = new File(inputFile);
        compilationUnit = StaticJavaParser.parse(file);
    }

    public void setDefaultOutPutFile(){
        File file = new File(inputFile);
        this.outPutFile = file.getParent().concat("/../modified/").concat(file.getName());
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
                if (node instanceof EnclosedExpr && ((EnclosedExpr) node).getInner() instanceof LiteralExpr) {
                    node.getParentNode().get().replace(node,((EnclosedExpr) node).getInner());
                }
                if (node instanceof EnclosedExpr && node.getParentNode().isPresent() && node.getParentNode().get() instanceof AssignExpr) {
//                    System.out.println(node);
                    node.getParentNode().get().replace(node,((EnclosedExpr) node).getInner());
                }
                if (node instanceof EnclosedExpr && node.getParentNode().isPresent() && node.getParentNode().get() instanceof ReturnStmt) {
//                    if (((EnclosedExpr) node).getInner() instanceof ConditionalExpr && !(node.getParentNode().get() instanceof ReturnStmt)) {
//                        return;
//                    }
                    node.getParentNode().get().replace(node,((EnclosedExpr) node).getInner());
                }
                if (node.getParentNode().isPresent() && node.getParentNode().get() instanceof VariableDeclarator) {
                    ((VariableDeclarator) node.getParentNode().get()).setInitializer(((EnclosedExpr) node).getInner());
                }
                if (node.getParentNode().isPresent() && node.getParentNode().get() instanceof AssignExpr) {
                    ((AssignExpr) node.getParentNode().get()).setValue(((EnclosedExpr) node).getInner());
                }
            }
        });

        // remove parenthesis for binary expression
        compilationUnit.walk(node -> {
            if (node instanceof BinaryExpr) {
                Expression left = ((BinaryExpr) node).getLeft();
                Expression right = ((BinaryExpr) node).getRight();
                if (((BinaryExpr) node).getOperator().equals(BinaryExpr.Operator.AND) &&
                        (left instanceof EnclosedExpr && ((EnclosedExpr) left).getInner() instanceof BinaryExpr && ((BinaryExpr) ((EnclosedExpr) left).getInner()).getOperator().equals(BinaryExpr.Operator.OR))
                                || (right instanceof EnclosedExpr && ((EnclosedExpr) right).getInner() instanceof BinaryExpr && ((BinaryExpr) ((EnclosedExpr) right).getInner()).getOperator().equals(BinaryExpr.Operator.OR))) {
                    return;
                }

                if (left instanceof EnclosedExpr && ((EnclosedExpr) left).getInner() instanceof BinaryExpr && ((BinaryExpr) node).getOperator().compareTo(((BinaryExpr) ((EnclosedExpr) left).getInner()).getOperator()) > 0) {
                    return;
                }

                if (right instanceof EnclosedExpr && ((EnclosedExpr) right).getInner() instanceof BinaryExpr && ((BinaryExpr) node).getOperator().compareTo(((BinaryExpr) ((EnclosedExpr) right).getInner()).getOperator()) > 0) {
                    return;
                }

                if (left instanceof EnclosedExpr && !(((EnclosedExpr) left).getInner() instanceof ConditionalExpr)) {
                    ((BinaryExpr) node).setLeft(((EnclosedExpr) left).getInner());
                }
                if (right instanceof EnclosedExpr&& !(((EnclosedExpr) right).getInner() instanceof ConditionalExpr)) {
                    ((BinaryExpr) node).setRight(((EnclosedExpr) right).getInner());
                }
            }
        });

        compilationUnit.walk(node -> {
            if (node instanceof ConditionalExpr) {
                if (((ConditionalExpr) node).getCondition() instanceof EnclosedExpr) {
                    ((ConditionalExpr) node).setCondition(((EnclosedExpr) ((ConditionalExpr) node).getCondition()).getInner());
                }
            }
        });

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
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
//            List<FieldDeclaration> originalFields = new ArrayList<>();
            classOrInterfaceDeclaration.findAll(FieldDeclaration.class).forEach(fieldDeclaration -> {
                if (fieldDeclaration.getParentNode().get() != classOrInterfaceDeclaration) {
                    return;
                }
//                originalFields.add(fieldDeclaration);

                    List<FieldDeclaration> newFileds = new ArrayList<>();
                    NodeList<Modifier> modifier = fieldDeclaration.getModifiers();
                    if (fieldDeclaration.getVariables().size() == 1) {
                        return;
                    } else {
                        System.out.println(fieldDeclaration + "....." + fieldDeclaration.getVariables().size());
                        fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                            newFileds.add(new FieldDeclaration(modifier,variableDeclarator));
                        });
                        int i = classOrInterfaceDeclaration.getMembers().indexOf(fieldDeclaration);
                        for (FieldDeclaration fieldDeclaration1 : newFileds){
                            classOrInterfaceDeclaration.getMembers().add(i++,fieldDeclaration1);
                        }

                        classOrInterfaceDeclaration.remove(fieldDeclaration);
                    }
            });
//            List<FieldDeclaration> newFileds = new ArrayList<>();
//            originalFields.forEach(field -> {
//                NodeList<Modifier> modifier = field.getModifiers();
//                if (field.getVariables().size() == 1) {
//                    newFileds.add(field);
//                } else {
//                    field.getVariables().forEach(variableDeclarator -> {
//                        newFileds.add(new FieldDeclaration(modifier,variableDeclarator));
//                    });
//                }
//            });
//            newFileds.forEach(fieldDeclaration -> {
//                classOrInterfaceDeclaration.getMembers().add(newFileds.indexOf(fieldDeclaration),fieldDeclaration);
//            });
        });

//        compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
//           List<Statement> original = new ArrayList<>();
//           if (methodDeclaration.getBody().isPresent()) {
//               methodDeclaration.getBody().get().getStatements().forEach(statement -> {
//                   if (statement instanceof ExpressionStmt && ((ExpressionStmt) statement).getExpression() instanceof VariableDeclarationExpr) {
//                       original.add(statement);
//                   }
//               });
//           }
//            List<Statement> newList = new ArrayList<>();
//            original.forEach(statement -> {
//               methodDeclaration.getBody().get().remove(statement);
//               if (((VariableDeclarationExpr)((ExpressionStmt)statement).getExpression()).getVariables().size() == 1) {
//                   newList.add(statement);
//               } else {
//                   ((VariableDeclarationExpr)((ExpressionStmt)statement).getExpression()).getVariables().forEach(variable -> {
//                       newList.add(new ExpressionStmt(new VariableDeclarationExpr(variable)));
//                   });
//               }
//           });
//            newList.forEach(statement -> {
//                methodDeclaration.getBody().get().addStatement(newList.indexOf(statement),statement);
//            });
//        });

        compilationUnit.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
            Map<Statement,List<Statement>> map = new HashMap<>();
            if (methodDeclaration.getBody().isPresent()) {
                methodDeclaration.getBody().get().getStatements().forEach(statement -> {
                    if (statement instanceof ExpressionStmt && ((ExpressionStmt) statement).getExpression() instanceof VariableDeclarationExpr) {
                        List<Statement> newList = new ArrayList<>();
                            if (((VariableDeclarationExpr)((ExpressionStmt)statement).getExpression()).getVariables().size() == 1) {
                                return;
                            } else {
                                ((VariableDeclarationExpr)((ExpressionStmt)statement).getExpression()).getVariables().forEach(variable -> {
                                    newList.add(new ExpressionStmt(new VariableDeclarationExpr(variable)));
                                });
                                map.put(statement,newList);
                            }
                    }
                });
                for (Map.Entry<Statement,List<Statement>> entry : map.entrySet()) {
                    Statement statement = entry.getKey();
                    List<Statement> newList = entry.getValue();
                    int index = methodDeclaration.getBody().get().getStatements().indexOf(statement);
                    methodDeclaration.getBody().get().remove(statement);
                    for (Statement statement1 : newList){
                        methodDeclaration.getBody().get().addStatement(index++,statement1);
                    }
                }
            }

        });


    }

    /*
    *
    * For pattern5 and pattern9
    * check camel case for variables
    *
    *
    * */
    public void checkCamelCase() {
        compilationUnit.walk(node -> {
            if (node instanceof SimpleName) {
                if (node.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
                    return;
                }
                String s = ((SimpleName) node).getIdentifier();
                CaseFormat format = null;
                if (s.contains("_")) {
                    if (s.toUpperCase().equals(s))
                        format = CaseFormat.UPPER_UNDERSCORE;
                    if (s.toLowerCase().equals(s))
                        format = CaseFormat.LOWER_UNDERSCORE;
                } else if (s.contains("-")) {
                    if (s.toLowerCase().equals(s))
                        format = CaseFormat.LOWER_HYPHEN;
                } else {
                    if (Character.isLowerCase(s.charAt(0))) {
                        if (s.matches("([a-z]+[A-Z]+\\w+)+"))
                            format = CaseFormat.LOWER_CAMEL;
                    } else {
                        if (s.matches("([A-Z]+[a-z]+\\w+)+"))
                            format = CaseFormat.UPPER_CAMEL;
                    }
                }
                if (format != null) {
                    String modify = format.to(CaseFormat.LOWER_CAMEL,s);
                    ((SimpleName) node).setIdentifier(modify);
                }
            }
        });
    }

    /*
    *
    * For pattern6
    * check for statement must have braces
    *
    * */
    public void checkBraces(){

       compilationUnit.findAll(IfStmt.class).forEach(statement -> {
           Statement thenState = ((IfStmt) statement).getThenStmt();
           if (thenState != null && !(thenState instanceof BlockStmt)) {
               NodeList<Statement> temp = new NodeList<>();
               temp.add(thenState);
               thenState = new BlockStmt(temp);
               ((IfStmt) statement).setThenStmt(thenState);
           }
           Optional<Statement> optElseState = ((IfStmt) statement).getElseStmt();
           if (optElseState.isPresent() && !(optElseState.get() instanceof BlockStmt)) {
               NodeList<Statement> temp = new NodeList<>();
               temp.add(optElseState.get());
               Statement elseState = new BlockStmt(temp);
               ((IfStmt) statement).setElseStmt(elseState);
           }
       });

       compilationUnit.findAll(ForStmt.class).forEach(statement -> {
           Statement body = ((ForStmt) statement).getBody();
           if (!(body instanceof BlockStmt)) {
               NodeList<Statement> temp = new NodeList<>();
               temp.add(body);
               body = new BlockStmt(temp);
               ((ForStmt) statement).setBody(body);
           }
       });

       compilationUnit.findAll(WhileStmt.class).forEach(statement ->{
           Statement body = ((WhileStmt) statement).getBody();
           if (!(body instanceof BlockStmt)) {
               NodeList<Statement> temp = new NodeList<>();
               temp.add(body);
               body = new BlockStmt(temp);
               ((WhileStmt) statement).setBody(body);
           }
       });

       compilationUnit.findAll(ForEachStmt.class).forEach(statement -> {
           Statement body = ((ForEachStmt) statement).getBody();
           if (!(body instanceof BlockStmt)) {
               NodeList<Statement> temp = new NodeList<>();
               temp.add(body);
               body = new BlockStmt(temp);
               ((ForEachStmt) statement).setBody(body);
           }
       });
    }

    /*
    *
    * For pattern 7
    * comment out empty method body
    *
    * */
    public void checkEmptyMethodBody(){
        compilationUnit.findAll(MethodDeclaration.class).forEach(method -> {
            if (!method.getBody().isPresent()) {
                return;
            }
            Statement statement = method.getBody().get();
            if (statement.getChildNodes().size() == 0) {
                statement.addOrphanComment(new LineComment("this is an unsued methof"));
            }
        });
    }

    /*
    *
    * For pattern8
    * empty catch statement block
    *
    * */
    public void checkEmptyCatchClause(){
        compilationUnit.findAll(Statement.class).forEach(statement -> {
            if (statement instanceof TryStmt) {
                ((TryStmt) statement).getCatchClauses().forEach(catchClause -> {
                    if (catchClause.getBody().isEmpty()) {
                        BlockStmt blockStmt = new BlockStmt();
                        blockStmt.addOrphanComment(new LineComment("an exception happened"));
                        MethodCallExpr expression = new MethodCallExpr();
                        expression.setName("print");
                        FieldAccessExpr fieldAccessExpr = new FieldAccessExpr();
                        fieldAccessExpr.setName("out");
                        fieldAccessExpr.setScope(new NameExpr("System"));
                        expression.setScope(fieldAccessExpr);
                        NodeList<Expression> arguments = new NodeList<>();
                        arguments.add(new StringLiteralExpr(""));
                        expression.setArguments(arguments);
                        blockStmt.addStatement(expression);
                        catchClause.setBody(blockStmt);
                    }
                });
            }
        });
    }

    /*
    *
    * For pattern10
    * check assignment in operands
    *
    * */
    public void checkAssignmentInOperands(){
        compilationUnit.findAll(BinaryExpr.class).forEach(binaryExpr -> {
            Expression left = binaryExpr.getLeft();
            Expression right = binaryExpr.getRight();
            if (left instanceof EnclosedExpr && ((EnclosedExpr) left).getInner() instanceof AssignExpr) {
                ExpressionStmt stmt = new ExpressionStmt(((EnclosedExpr) left).getInner());
                Expression newLeft = ((AssignExpr) ((EnclosedExpr) left).getInner()).getTarget();
                BlockStmt parent = (BlockStmt) binaryExpr.getParentNode().get().getParentNode().get();
                parent.addStatement(parent.getStatements().indexOf(binaryExpr.getParentNode().get()),stmt);
                left.getParentNode().get().replace(left,newLeft);
            }
            if (right instanceof EnclosedExpr && ((EnclosedExpr) right).getInner() instanceof AssignExpr) {
                ExpressionStmt stmt = new ExpressionStmt(((EnclosedExpr) right).getInner());
                Expression newRight = ((AssignExpr) ((EnclosedExpr) right).getInner()).getTarget();
                BlockStmt parent = (BlockStmt) binaryExpr.getParentNode().get().getParentNode().get();
                parent.addStatement(parent.getStatements().indexOf(binaryExpr.getParentNode().get()),stmt);
                right.getParentNode().get().replace(right,newRight);
            }
        });
    }

    public void doRefactor(){
        checkSingleLineDeclaration();
        checkAssignmentInOperands();
        checkStringEquals();
//        checkForClassEquals();
        checkUselessParenthesis();
        checkBraces();
//        checkCamelCase();
        checkEmptyCatchClause();
        checkEmptyMethodBody();
        write();
    }

    /*
    *
    * Analysis target artifatcs
    *
    *
    * */
    public void analyze(String inputFile) throws Exception {
        File file = new File(inputFile);
        for (File temp : file.listFiles()) {
            String outputFile = temp.getAbsolutePath().replaceAll("original","modified");
            File outFile = new File(outputFile);
            if (temp.isDirectory()) {
                outFile.mkdir();
                analyze(temp.getAbsolutePath());
            } else {
//                System.out.println(temp.getAbsolutePath());
                if (temp.getAbsolutePath().endsWith(".java")) {
                    this.inputFile = temp.getAbsolutePath();
                    this.outPutFile = outputFile;
                    init();
                    doRefactor();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

        AutoFixer autoFixer = new AutoFixer();

        // code for 10 patterns
        File file = new File("./AutoFixer/src/main/resources/patterns");
        for (File temp : file.listFiles()) {
            if (temp.getName().endsWith("6.java")) {
                autoFixer.setInputFile(temp.getAbsolutePath());
                autoFixer.setDefaultOutPutFile();
                autoFixer.init();
                autoFixer.print();
                autoFixer.doRefactor();
            }
        }

        // code for analyze javaparser
//        autoFixer.analyze("./AutoFixer/src/main/resources/javaparser/original");

        // code for analyze pmd
//        autoFixer.analyze("./AutoFixer/src/main/resources/pmd/original");

    }

}
