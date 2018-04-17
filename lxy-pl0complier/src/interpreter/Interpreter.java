package interpreter;

import java.io.*;
import java.util.Scanner;
import java.util.Vector;

public class Interpreter {
    private BufferedWriter bw;//写入解释运行结果存放地址
    private BufferedReader br;//读入pcode
    private String pcodeLine;
    //private Vector<String> pcode=new Vector<>();
    private Vector<String> instruction=new Vector<>();
    private Vector<Integer> op1=new Vector<>();
    private Vector<Integer> op2=new Vector<>();
    private int[] dataStack=new int[2000];//数据栈
    private int basePointer=0;//数据区基地址指针
    private int topPointer=-1;//数据栈顶部指针
    private int codePointer=0;//代码指针

    public Interpreter(String pcodefile,String resultfile){
        try {
            br = new BufferedReader(new FileReader(pcodefile));
            bw = new BufferedWriter(new FileWriter(resultfile));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public void run(){
        readPcode();
        execute();
    }

    private void readPcode(){
        try{
            while ((pcodeLine=br.readLine())!=null){
                instruction.add(pcodeLine.substring(pcodeLine.indexOf(9)+1,pcodeLine.indexOf(9)+4));//读取指令
                op1.add(Integer.valueOf(pcodeLine.substring(pcodeLine.indexOf(" ")+1,pcodeLine.indexOf(","))));//读取op1
                op2.add(Integer.valueOf(pcodeLine.substring(pcodeLine.indexOf(",")+1,pcodeLine.length())));//读取op2
            }
            br.close();
        }catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    private void execute(){
        int codeMax=instruction.size();
        Scanner scan = new Scanner(System.in);
        dataStack[2]=codeMax;
        while (codePointer<codeMax){
            //System.out.println(codePointer);
            switch (instruction.elementAt(codePointer)){
                case "OPR":
                    switch (op2.elementAt(codePointer)){
                        case 0://数据栈弹出
                            topPointer=basePointer-1;
                            codePointer=dataStack[topPointer+3];
                            basePointer=dataStack[topPointer+2];
                            break;
                        case 1:
                            dataStack[topPointer]=-dataStack[topPointer];
                            break;
                        case 2:
                            topPointer--;
                            dataStack[topPointer]=dataStack[topPointer]+dataStack[topPointer+1];
                            break;
                        case 3:
                            topPointer--;
                            dataStack[topPointer]=dataStack[topPointer]-dataStack[topPointer+1];
                            break;
                        case 4:
                            topPointer--;
                            dataStack[topPointer]=dataStack[topPointer]*dataStack[topPointer+1];
                            break;
                        case 5:
                            topPointer--;
                            dataStack[topPointer]=dataStack[topPointer]/dataStack[topPointer+1];
                            break;
                        case 6://如果是奇数，返回true
                            if (dataStack[topPointer]%2==0)
                                dataStack[topPointer]=0;
                            else dataStack[topPointer]=1;
                            break;
                        case 7:
                            topPointer--;
                            if (dataStack[topPointer]==dataStack[topPointer+1])
                                dataStack[topPointer]=1;
                            else dataStack[topPointer]=0;
                            break;
                        case 8:
                            topPointer--;
                            if (dataStack[topPointer]!=dataStack[topPointer+1])
                                dataStack[topPointer]=1;
                            else dataStack[topPointer]=0;
                            break;
                        case 9:
                            topPointer--;
                            if (dataStack[topPointer]<dataStack[topPointer+1])
                                dataStack[topPointer]=1;
                            else dataStack[topPointer]=0;
                            break;
                        case 10:
                            topPointer--;
                            if (dataStack[topPointer]>=dataStack[topPointer+1])
                                dataStack[topPointer]=1;
                            else dataStack[topPointer]=0;
                            break;
                        case 11:
                            topPointer--;
                            if (dataStack[topPointer]>dataStack[topPointer+1])
                                dataStack[topPointer]=1;
                            else dataStack[topPointer]=0;
                            break;
                        case 12:
                            topPointer--;
                            if (dataStack[topPointer]<=dataStack[topPointer+1])
                                dataStack[topPointer]=1;
                            else dataStack[topPointer]=0;
                            break;
                    }
                    ++codePointer;
                    break;
                case "LIT":
                    topPointer++;
                    dataStack[topPointer]=op2.elementAt(codePointer);
                    ++codePointer;
                    break;
                case "LOD":
                    topPointer++;
                    dataStack[topPointer]=dataStack[base(op1.elementAt(codePointer))+op2.elementAt(codePointer)];//加载到数据区上
                    ++codePointer;
                    break;
                case "STO":
                    dataStack[base(op1.elementAt(codePointer))+op2.elementAt(codePointer)]=dataStack[topPointer];//存储
                    ++codePointer;
                    break;
                case "CAL":
                    dataStack[topPointer+1]=base(op1.elementAt(codePointer));//嵌套外层基地址
                    dataStack[topPointer+2]=basePointer;//调用数据区基地址
                    dataStack[topPointer+3]=codePointer;
                    basePointer=topPointer+1;
                    codePointer=op2.elementAt(codePointer);//跳转至过程开始指令
                    break;
                case "INT":
                    topPointer=topPointer+op2.elementAt(codePointer);
                    ++codePointer;
                    break;
                case "JMP":
                    codePointer=op2.elementAt(codePointer);
                    break;
                case "JPC":
                    if(dataStack[topPointer]==0)
                        codePointer=op2.elementAt(codePointer);
                    else
                        ++codePointer;
                    topPointer--;//条件结果出栈
                    break;
                case "RED":
                    System.out.println("请输入相应的整数");
                    dataStack[base(op1.elementAt(codePointer))+op2.elementAt(codePointer)]= scan.nextInt();
                    ++codePointer;
                    break;
                case "WRT":
                    System.out.println(dataStack[topPointer]);
                    topPointer--;
                    ++codePointer;
                    break;
            }
        }
    }

    private int base(int level){//寻找层次差为l的数据区的基地址
        int tempbase=basePointer;
        while (level!=0){
            tempbase=dataStack[tempbase];//转移到嵌套外层的数据区基地址
            level--;
        }
        return tempbase;
    }
}
