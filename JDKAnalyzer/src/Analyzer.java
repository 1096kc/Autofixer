import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;

public class Analyzer {

    public static void main(String[] args) {
        FileInputStream fin = null;
        Map<String,Integer> map = new HashMap<>();
        try {

            File file = new File("./JDKAnalyzer/resource/PMD Result/");

            for (File resultFile : file.listFiles()) {
                if (!resultFile.getName().endsWith(".txt")) {
                    continue;
                }
                fin = new FileInputStream(resultFile);
                Scanner scanner = new Scanner(fin);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    line = line.replaceAll(" \\'.*\\'", " ");
                    line = line.substring(line.indexOf(":") + 1);
                    line = line.substring(line.indexOf(":") + 1);
//                    System.out.println(resultFile.getName() + " " + line);
                    map.put(line, map.getOrDefault(line, 0) + 1);
                }
            }

            PriorityQueue<Violation> pq = new PriorityQueue<>((a, b) -> (b.count - a.count));
            for (Map.Entry<String,Integer> entry : map.entrySet()) {
                pq.add(new Violation(entry.getKey(),entry.getValue()));
            }
            while (!pq.isEmpty()) {
                System.out.println(pq.poll());
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Violation{
    String description;
    int count;
    public Violation(String description, int count) {
        this.description = description;
        this.count = count;
    }

    @Override
    public String toString() {
        return "Violation{" +
                "description='" + description + '\'' +
                ", count=" + count +
                '}';
    }
}