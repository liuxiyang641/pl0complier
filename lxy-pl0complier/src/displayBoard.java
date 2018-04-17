import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import complier.*;
import interpreter.Interpreter;

public class displayBoard {
    public static void main(String[] args) {
        JPanel panel1=new JPanel();
        //panel1.setLayout(new BorderLayout());

        JFrame frame = new JFrame("PL0displayboard");
        frame.setSize(300, 200);
        frame.setContentPane(panel1);
        frame.setLocation(150,75);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setSize(1000,2000);

        BorderLayout lay=new BorderLayout();
        panel1.setLayout(lay);
        JButton button1=new JButton("选择文件");
        button1.setPreferredSize(new Dimension(100,30));
        panel1.add(button1,"East");
        JButton button2=new JButton("PL0分析");
        button2.setPreferredSize(new Dimension(100,30));
        panel1.add(button2,"West");
        JTextArea textArea1=new JTextArea();
        textArea1.setPreferredSize(new Dimension(300,200));
        panel1.add(textArea1,"North");
        //JTextArea textArea2=new JTextArea();//area2用来显示
        //textArea2.setPreferredSize(new Dimension(300,200));
        //panel1.add(textArea2,"South");

        JTextArea jta = new JTextArea();
        JScrollPane jsp = new JScrollPane(jta);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.setPreferredSize(new Dimension(300,200));
        jta.setCaretPosition(jta.getText().length());
        panel1.add(jsp,"South");
        frame.pack();

        button1.addActionListener(new ActionListener() {    //选择TXT文件
            public void actionPerformed(ActionEvent e) {
                //按钮点击事件
                JFileChooser chooser = new JFileChooser();             //设置选择器
                chooser.setMultiSelectionEnabled(true);             //设为多选
                int returnVal = chooser.showOpenDialog(button1);        //是否打开文件选择框
                System.out.println("returnVal="+returnVal);

                if (returnVal == JFileChooser.APPROVE_OPTION) {          //如果符合文件类型
                    String filepath = chooser.getSelectedFile().getAbsolutePath();      //获取绝对路径
                    GramSemaAnalysis gsa=new GramSemaAnalysis(filepath, "src/pcode.txt","src/error.txt");
                    if (gsa.run()){
                        Interpreter inter=new Interpreter("src/pcode.txt","src/result.txt");
                        inter.run();
                    }
                    try {
                        BufferedReader br=new BufferedReader(new FileReader("src/pcode.txt"));//读取output文件的的内容进行显示
                        StringBuffer content=new StringBuffer();
                        String tchar;
                        while((tchar=br.readLine())!=null){
                            content.append(tchar);
                            content.append("\n");
                        }
                        br.close();
                        jta.setText(content.toString());

                        content.delete(0,content.length());
                        br=new BufferedReader(new FileReader("src/error.txt"));
                        while((tchar=br.readLine())!=null){
                            content.append(tchar);
                            content.append("\n");
                        }
                        br.close();
                        jta.append(content.toString());
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        });

        button2.addActionListener(new ActionListener() {    //输入文本
            public void actionPerformed(ActionEvent e) {
                //按钮点击事件
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter("src/sourcecode.txt"));//将输入的文本输入到input文件
                    String textcontennt=textArea1.getText();
                    bw.write(textcontennt);
                    bw.flush();
                    bw.close();
                    GramSemaAnalysis gsa=new GramSemaAnalysis("src/sourcecode.txt", "src/pcode.txt","src/error.txt");
                    if (gsa.run()){
                        Interpreter inter=new Interpreter("src/pcode.txt","src/result.txt");
                        inter.run();
                    }

                    BufferedReader br=new BufferedReader(new FileReader("src/pcode.txt"));//读取output文件的的内容进行显示
                    StringBuffer content=new StringBuffer();
                    String tchar;
                    while((tchar=br.readLine())!=null){
                        content.append(tchar);
                        content.append("\n");
                    }
                    br.close();
                    jta.setText(content.toString());

                    content.delete(0,content.length());
                    br=new BufferedReader(new FileReader("src/error.txt"));
                    while((tchar=br.readLine())!=null){
                        content.append(tchar);
                        content.append("\n");
                    }
                    br.close();
                    jta.append(content.toString());
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        });
        //panel1.add(button1);
        //frame.pack();
        frame.setVisible(true);
    }

}
