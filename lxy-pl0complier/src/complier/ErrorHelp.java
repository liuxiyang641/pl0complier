package complier;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class ErrorHelp {
    private String[]errorInfo=new String[200];
    private BufferedWriter bw;
    private int errorNumber=0;

    public ErrorHelp(String outputfile){
        try {
            bw=new BufferedWriter(new FileWriter(outputfile));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void outputErrorInfo(){
        try {
            for(int i=0;i<errorNumber;++i){
                bw.write(errorInfo[i]);
                bw.write(10);//换行
            }
            bw.flush();
            bw.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public void addNewError(String errorinfo){
        errorInfo[errorNumber++]=errorinfo;
    }

    public int getErrorNumber(){
        return errorNumber;
    }

    public void addNewError(int errortype,String errorToken){
        String s=errorToken;
        s+="： ";
        switch (errortype){
            case 1:
                s+="应该是=而不是:=";
                break;
            case 2:
                s+="=后面应该是数";
                break;
            case 3:
                s+="标识符后面是=";
                break;
            case 4:
                s+="const,var,procedure后面是标识符";
                break;
            case 5:
                s+="漏掉，或者；";
                break;
            case 6:
                s+="过程说明之后的符号不正确";
                break;
            case 7:
                s+="应该是语句";
                break;
            case 8:
                s+="程序体内语句部分后面的符号不正确";
                break;
            case 9:
                s+="应为句号";
                break;
            case 10:
                s+="语句之间漏分号";
                break;
            case 11:
                s+="标识符未说明";
                break;
            case 12:
                s+="不可向常量或者过程赋值";
                break;
            case 13:
                s+="应为赋值运算符:=";
                break;
            case 14:
                s+="call后应为标识符";
                break;
            case 15:
                s+="不可调用常量或者变量";
                break;
            case 16:
                s+="应为then";
                break;
            case 17:
                s+="应为；或end";
                break;
            case 18:
                s+="应为do";
                break;
            case 19:
                s+="语句后的符号不正确";
                break;
            case 20:
                s+="应为关系运算符";
                break;
            case 21:
                s+="表达式内不可有过程标识符";
                break;
            case 22:
                s+="漏右括号";
                break;
            case 23:
                s+="因子后不可为此符号";
                break;
            case 24:
                s+="表达式不能以此符号开始";
                break;
            case 30:
                s+="这个数太大";
                break;
            case 40:
                s+="应为左括号";
                break;
        }
        errorInfo[errorNumber++]=s;
    }
}
