package patterns;

public class Pattern3{

    /*
    *
    * use equals to compare two objects
    * walkmod not working
    * */

    Node nodeA;
    Node nodeB;

    public boolean compare(Node node1, Node node2, Node node3, int a, int b){
        if (a == b) {
          return true;
        } else if (node1 == node2) {
            return (node1 == node3);
        } else if(node1 != node3) {
            return true;
        } else {
            return false;
        }
    }
}

class Node{
    int val;
    public Node(int val){
        this.val = val;
    }
}