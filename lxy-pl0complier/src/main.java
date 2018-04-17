import complier.GramSemaAnalysis;
import complier.LexicalAnalysis;
import interpreter.Interpreter;

import java.io.*;

public class main {
    public static void main(String[]args){
        /*LexicalAnalysis s=new LexicalAnalysis("src/input.txt");
        s.next();

        try {
            //br = new BufferedReader(new FileReader(inputFile));
            BufferedWriter  bw = new BufferedWriter(new FileWriter("src/output.txt"));
            bw.write("单词");
            bw.write(9);//tab
            bw.write("类别");
            bw.write(9);
            bw.write("值");
            bw.write(10);//换行
            while(!s.getTokenType().equals("空")){
                bw.write(s.getToken());
                bw.write(9);
                bw.write(s.getTokenType());
                bw.write(10);//换行
                s.next();
            }
            bw.flush();
            bw.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }*/

       /* GramSemaAnalysis gsa=new GramSemaAnalysis("src/sourcecode.txt","src/pcode.txt","src/error.txt");
        gsa.run();*/
        GramSemaAnalysis gsa=new GramSemaAnalysis("src/sourcecode.txt","src/pcode.txt","src/error.txt");
        if (gsa.run()){
            Interpreter inter=new Interpreter("src/pcode.txt","src/result.txt");
            inter.run();
        }

    }
}
