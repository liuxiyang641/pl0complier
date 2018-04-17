package complier;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

public class GramSemaAnalysis {
    private BufferedWriter bw;//写入Pcode存放文件
    private LexicalAnalysis lexAnalysis;//词法分析
    private String[] pCode=new String[2000];
    private int codeIndex=0;
    private enum instructionType{//指令类型
        LIT,OPR,LOD,STO,CAL,INT,JMP,JPC,RED,WRT
    }
    private Stack<Integer> jumpIndex=new Stack<>();//储存jump的指令地址，准备回填
    private Stack<Integer> jumpLevel=new Stack<>();//储存jump的嵌套层次
    private int instruLevel=-1;//记录当前代码的嵌套层次
    private enum symTableItemType{
        CONST,VAR,PROC
    }
    private class symTableItem{//符号表的符号
        private String name;//符号表中符号的名字
        private symTableItemType kind;//符号的种类
        private int val=-1;//符号的值
        private int level=-1;//符号的层次
        private int adr=-1;//变量的地址和过程的指令地址
    }
    private Stack<symTableItem>symTable=new Stack<>();//符号表
    private ErrorHelp errorInfo;
    private Vector<String>declBegSys=new Vector<>();//声明部分开始符号
    private Vector<String>statBegSys=new Vector<>();//语句部分开始符号
    private Vector<String>facBegSys=new Vector<>();//因子开始符号
    //private boolean jumpToken=false;//是否跳读过token

    public GramSemaAnalysis(String inputfile,String outputfile,String errorfile){        //初始化语义和语法分析
        lexAnalysis=new LexicalAnalysis(inputfile);
        errorInfo=new ErrorHelp(errorfile);
        try {
            bw=new BufferedWriter(new FileWriter(outputfile));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        declBegSys.add("const");
        declBegSys.add("var");
        declBegSys.add("procedure");

        statBegSys.add("begin");
        statBegSys.add("call");
        statBegSys.add("if");
        statBegSys.add("while");
        statBegSys.add("repeat");
        statBegSys.add("read");
        statBegSys.add("write");
    }

    public boolean run(){      //启动语法和语义分析
        if(program()==1&&errorInfo.getErrorNumber()==0){   //分析成功
            outputCode();
            return true;
        }
        else {  //分析失败
            errorInfo.outputErrorInfo();
            outputCode();
            return false;
        }
    }

    private  void outputCode(){//输出pcode
        try {
            for(int i=0;i<codeIndex;++i){
                bw.write(Integer.valueOf(i).toString());
                bw.write(9);
                bw.write(pCode[i]);
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

    private void test(Vector<String> set1){//出现错误，跳读字符
        if(searchFinSym(set1))
            return;
        while (!searchFinSym(set1)){//停止符号集
            errorInfo.addNewError("存在未识别的字符： "+lexAnalysis.getToken());
            lexAnalysis.next();
        }
    }

    private boolean searchFinSym(Vector<String> set1){
        if(lexAnalysis.getTokenType().equals("标识符")){
            for (String s:set1) {
                if(s.equals("标识符"))
                    return true;
            }
        }
        else {
            for (String s:set1) {
                if(lexAnalysis.getToken().equals(s))
                    return true;
            }
        }
        return false;
    }

    private void genCode(instructionType instruction,int opa,int opb){
        pCode[codeIndex++]=instruction.toString()+" "+opa+","+opb;
    }

    private int program(){         //<程序> ::= <分程序>.
        lexAnalysis.next();
        Vector<String> finSymSet = new Vector<>();//<分程序>终止符号集
        finSymSet.add(".");
        finSymSet.addAll(declBegSys);
        finSymSet.addAll(statBegSys);

        block(finSymSet);
        if (lexAnalysis.getTokenType().equals("分界符") && lexAnalysis.getToken().equals(".")) {
            lexAnalysis.next();
            if (lexAnalysis.getTokenType().equals("空"))//程序结束
                return 1;
            else {
                return -1;
            }
        } else {
            errorInfo.addNewError(9,lexAnalysis.getToken());//应该为句号
            return -1;
        }
    }

    private int block(Vector<String>finSymSet){           //<分程序> ::= [<常量说明部分>][变量说明部分>][<过程说明部分>]<语句>
        int jmpindex=codeIndex;
        genCode(instructionType.JMP,0,-1);
        instruLevel++;
        test(finSymSet);//保证分程序块内有可以被识别的符号
        Vector<String>finsscopy=new Vector<>(finSymSet);
        finsscopy.removeElement("const");
        constDeclaration(finsscopy);//合法后继不应该有const
        finsscopy.removeElement("var");
        varDeclaration(finsscopy);//合法后继不应该有var

        finsscopy.add("const");
        finsscopy.add("var");
        finsscopy.add("标识符");
        procedureDecal(finsscopy);
        finsscopy.removeElement("procedure");//合法后继不应该有procedure
        pCode[jmpindex]=instructionType.JMP.toString()+" 0,"+codeIndex;//回填jump
        genCode(instructionType.INT,0,countLevelVarNumber(instruLevel)+3);//开辟数据栈

        finsscopy.remove("const");
        finsscopy.remove("var");
        finsscopy.remove("procedure");
        statement(finsscopy);
        genCode(instructionType.OPR,0,0);//数据栈弹出

        instruLevel--;//嵌套层次减一
        test(finSymSet);
        return 1;
    }

    private void constDeclaration(Vector<String>finSymSet){     //<常量说明部分> ::= const<常量定义>{,<常量定义>};
        Vector<String> finsscopy=new Vector<>();
        finsscopy.addAll(finSymSet);
        if(lexAnalysis.getTokenType().equals("关键字")&&lexAnalysis.getToken().equals("const")){
            lexAnalysis.next();
            finsscopy.add(",");
            finsscopy.add(";");
            constDefine(finsscopy);
            while (lexAnalysis.getToken().equals(",")) { //{,<常量定义>}
                lexAnalysis.next();
                constDefine(finsscopy);
            }
            if (lexAnalysis.getToken().equals(";") && lexAnalysis.getTokenType().equals("分界符")) {
                lexAnalysis.next();
            } else {
                errorInfo.addNewError(17,lexAnalysis.getToken());//漏掉分号
            }
        }
        test(finSymSet);
    }

    private void constDefine(Vector<String>finSymSet){      //<常量定义> ::= <标识符>=<无符号整数>
        if(lexAnalysis.getTokenType().equals("标识符")){
            symTableItem tempsym=new symTableItem();
            tempsym.name=lexAnalysis.getToken();//填入常量名字
           if(searchConst(tempsym.name)!=-1)  {//查找是否有重名常量
               errorInfo.addNewError("常量重名");
               lexAnalysis.next();//存在重名常量
           }
            lexAnalysis.next();
            if(lexAnalysis.getTokenType().equals("单字符运算符")&&lexAnalysis.getToken().equals("=")){
                lexAnalysis.next();
                if(lexAnalysis.getTokenType().equals("整数常量")){
                    tempsym.kind=symTableItemType.CONST;//填入常量的类型
                    tempsym.val=Integer.valueOf(lexAnalysis.getToken());//填入常量的值
                    symTable.push(tempsym);//常量符号入栈
                    lexAnalysis.next();
                    if (!lexAnalysis.getToken().equals(",")&&!lexAnalysis.getToken().equals(";")){
                        errorInfo.addNewError(5,lexAnalysis.getToken());//漏掉，或者;
                    }
                }
                else {
                    errorInfo.addNewError(2,lexAnalysis.getToken());//等于号后面是数
                }
            }
            else if(lexAnalysis.getToken().equals(":=")){
                errorInfo.addNewError(1,lexAnalysis.getToken());//应该是=，不是:=
            }
            else {
                errorInfo.addNewError(3,lexAnalysis.getToken());//标识符后面是=
            }
        }
        else {
            errorInfo.addNewError(4,lexAnalysis.getToken());//const后面应该是标识符
        }
        test(finSymSet);
    }

    private void varDeclaration(Vector<String>finSymSet){   //<变量说明部分>::= var<标识符>{,<标识符>};
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        finsscopy.add(";");
        finsscopy.add(",");
        if (lexAnalysis.getTokenType().equals("关键字") && lexAnalysis.getToken().equals("var")) {
            lexAnalysis.next();
            if (!lexAnalysis.getTokenType().equals("标识符")) {//<标识符>
                errorInfo.addNewError(4,lexAnalysis.getToken());//var后面是标识符
                test(finsscopy);
            } else {
                symTableItem tempsym1 = new symTableItem();
                tempsym1.name = lexAnalysis.getToken();
                tempsym1.kind = symTableItemType.VAR;
                tempsym1.level = instruLevel;
                if (existVarProcChecking(tempsym1) == -1) {//查找重复的变量或者过程
                    tempsym1.adr = countLevelVarNumber(tempsym1.level) + 3;//填入变量的地址
                    symTable.push(tempsym1);//符号入表
                    lexAnalysis.next();
                    if (!lexAnalysis.getToken().equals(",")&&!lexAnalysis.getToken().equals(";")){
                        errorInfo.addNewError(5,lexAnalysis.getToken());//漏掉，或者;
                        lexAnalysis.next();
                        test(finsscopy);//找到一个合适的，或者;
                    }
                } else {  //存在重名的变量
                    errorInfo.addNewError("变量重名");
                }
                test(finsscopy);
            }
            while (lexAnalysis.getToken().equals(",")) {//   {,<标识符>}
                lexAnalysis.next();
                symTableItem tempsym2 = new symTableItem();
                tempsym2.name = lexAnalysis.getToken();//新的标识符
                tempsym2.kind = symTableItemType.VAR;
                tempsym2.level = instruLevel;
                if (!lexAnalysis.getTokenType().equals("标识符")) {//不是标识符
                    errorInfo.addNewError(4,lexAnalysis.getToken());//var后面是标识符
                    test(finsscopy);
                } else if (existVarProcChecking(tempsym2) == -1) {//符号可以命名
                    tempsym2.adr = countLevelVarNumber(tempsym2.level) + 3;//填入变量的地址
                    symTable.push(tempsym2);//符号入表
                    lexAnalysis.next();
                    if (!lexAnalysis.getToken().equals(",")&&!lexAnalysis.getToken().equals(";")){
                        errorInfo.addNewError(17,lexAnalysis.getToken());//漏掉，或者;
                        test(finsscopy);//找到一个合适的，或者;
                    }
                } else {//符号不可命名
                    errorInfo.addNewError("标识符重名");
                    test(finsscopy);
                }
            }
            if (lexAnalysis.getToken().equals(";") && lexAnalysis.getTokenType().equals("分界符")) {
                lexAnalysis.next();
                test(finSymSet);
            } else {//漏掉;
                errorInfo.addNewError(10,lexAnalysis.getToken());
                test(finSymSet);
            }
        }
    }

    private int procedureDecal(Vector<String>finSymSet){      //<过程说明部分>::= <过程首部><分程序>;{<过程说明部分>}
        Vector<String> finsscopy=new Vector<>();
        finsscopy.addAll(finSymSet);
        if(procdureHead(finSymSet)!=-1){
            finsscopy.add(";");
            if(block(finsscopy)!=-1){
                if(lexAnalysis.getTokenType().equals("分界符")&&lexAnalysis.getToken().equals(";")){
                    lexAnalysis.next();
                    while (procedureDecal(finSymSet)!=-1){}
                    test(finSymSet);
                    return 1;
                }
                else {//缺少;
                    errorInfo.addNewError(17,lexAnalysis.getToken());//应该是；或者end
                    test(finSymSet);
                    return -1;
                }
            }
            test(finSymSet);
            return -1;//分程序分析失败
        }
        else {
            test(finSymSet);
            return -1;//过程说明部分失败
        }
    }

    private int procdureHead(Vector<String>finSymSet){ //<过程首部>::= procedure<标识符>;
        Vector<String> finsscopy=new Vector<>();
        if(lexAnalysis.getTokenType().equals("关键字")&&lexAnalysis.getToken().equals("procedure")){
            lexAnalysis.next();
            if(lexAnalysis.getTokenType().equals("标识符")){
                symTableItem tempsym=new symTableItem();
                tempsym.name=lexAnalysis.getToken();
                tempsym.kind=symTableItemType.PROC;
                tempsym.level=instruLevel;
                if(existVarProcChecking(tempsym)==-1){//不存在重名过程
                    tempsym.adr=codeIndex;//设置过程的第一个指令的地址
                    symTable.push(tempsym);//过程入表
                    lexAnalysis.next();
                    if(lexAnalysis.getTokenType().equals("分界符")&&lexAnalysis.getToken().equals(";")){
                        lexAnalysis.next();
                        test(finSymSet);
                        return 1;
                    }
                    else {//缺少;
                        errorInfo.addNewError(5,lexAnalysis.getToken());//应该是;或者end
                        test(finSymSet);
                        return 1;
                    }
                }
                else {//存在重名过程
                    errorInfo.addNewError("存在重名过程");
                    test(finSymSet);
                    return -1;
                }
            }
            else {//不是标识符
                errorInfo.addNewError(4,lexAnalysis.getToken());//procdure后面是标识符
                test(finSymSet);
                return -1;
            }
        }
        else {
            test(finSymSet);
            return -1;
        }
    }

    private int statement(Vector<String>finSymSet){    //<语句> ::= <赋值语句>|<条件语句>|<当型循环语句>|<过程调用语句>|<读语句>|<写语句>|<复合语句>|<重复语句>|<空>
        if (lexAnalysis.getTokenType().equals("标识符")){//<赋值语句>
            assignStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("if")&&lexAnalysis.getTokenType().equals("关键字")){//<条件语句>
            conditionStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("while")){//<当型循环语句>
            whileStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("call")){//<过程调用语句>
            callStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("read") && lexAnalysis.getTokenType().equals("关键字")){//<读语句>
            readStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("write")&&lexAnalysis.getTokenType().equals("关键字")){//<写语句>
            writeStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("begin")&&lexAnalysis.getTokenType().equals("关键字")){//<复合语句>
            compoundStatement(finSymSet);
        }
        else if (lexAnalysis.getToken().equals("repeat")&&lexAnalysis.getTokenType().equals("关键字")){//<重复语句>
            repeatStatement(finSymSet);
        }
        test(finSymSet);
        return 1;
    }

    private void assignStatement(Vector<String>finSymSet){//<赋值语句> ::= <标识符>:=<表达式>
        Vector<String> finsscopy = new Vector<>();
        int varindex;
        if ((varindex = searchVar(lexAnalysis.getToken(), instruLevel)) != -1) {//查找符号表找到对应变量,表达式左边一定是变量
            lexAnalysis.next();
            finsscopy.addAll(finSymSet);
            finsscopy.add(":=");
            test(finsscopy);
            if (lexAnalysis.getToken().equals(":=")) {
                lexAnalysis.next();
                expression(finSymSet);//<表达式>
                genCode(instructionType.STO, instruLevel - symTable.get(varindex).level, symTable.get(varindex).adr);//变量值存储STO
            } else if (lexAnalysis.getToken().equals("=")){
                errorInfo.addNewError(13,lexAnalysis.getToken());//应该为:=
                lexAnalysis.next();//跳过:=
            }
        } else if ((varindex = searchConst(lexAnalysis.getToken())) != -1||searchProc(lexAnalysis.getToken(),instruLevel)!=-1) {
            errorInfo.addNewError(12,lexAnalysis.getToken());//不可向常量或过程赋值
            lexAnalysis.next();//当前标识符不可用
        } else {//没有找到对应的变量
            errorInfo.addNewError(11,lexAnalysis.getToken());//标识符未说明
            lexAnalysis.next();//当前标识符不可用
        }
        test(finSymSet);
    }

    private void expression(Vector<String>finSymSet){//<表达式> ::= [+|-]<项>{<加法运算符><项>}
        Vector<String> finsscopy = new Vector<>();
        if (lexAnalysis.getToken().equals("-")) {
            genCode(instructionType.OPR, 0, 1);//栈顶取-
            lexAnalysis.next();
        } else if (lexAnalysis.getToken().equals("+")) {
            lexAnalysis.next();
        } else if(!lexAnalysis.getToken().equals("(")&&!lexAnalysis.getTokenType().equals("标识符")&&!lexAnalysis.getTokenType().equals("整数常量")){
            errorInfo.addNewError(24,lexAnalysis.getToken());//表达式不能以此符号开始
            test(finSymSet);
            return;
        }
        finsscopy.addAll(finSymSet);
        finsscopy.add("+");
        finsscopy.add("-");
        term(finsscopy);//<项>
        while (lexAnalysis.getToken().equals("+") || lexAnalysis.getToken().equals("-")) {//{<加法运算符><项>}
            if (lexAnalysis.getToken().equals("+")) {
                lexAnalysis.next();
                term(finsscopy);
                genCode(instructionType.OPR, 0, 2);//次栈顶+栈顶

            } else {
                lexAnalysis.next();
                term(finsscopy);
                genCode(instructionType.OPR, 0, 3);//次栈顶-栈顶
            }
        }
        test(finSymSet);
    }

    private void term(Vector<String>finSymSet){//<项> ::= <因子>{<乘法运算符><因子>}
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        finsscopy.add("*");
        finsscopy.add("/");
        factor(finsscopy);//<因子>
        while (lexAnalysis.getToken().equals("*") || lexAnalysis.getToken().equals("/")) {//{<乘法运算符><因子>}
            if (lexAnalysis.getToken().equals("*")) {
                lexAnalysis.next();
                factor(finsscopy);
                genCode(instructionType.OPR, 0, 4);//次栈顶*栈顶
            } else {
                lexAnalysis.next();
                factor(finsscopy);
                genCode(instructionType.OPR, 0, 5);//次栈顶/栈顶
            }
        }
        test(finSymSet);
    }

    private void factor(Vector<String>finSymSet){//<因子>::=<标识符>|<无符号整数>|'('<表达式>')'
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        if (lexAnalysis.getTokenType().equals("标识符")){//此时是变量或者常量,<标识符>
            int symindex;
            if ((symindex=searchVar(lexAnalysis.getToken(),instruLevel))!=-1){//寻找最近的变量
                genCode(instructionType.LOD,instruLevel-symTable.get(symindex).level,symTable.get(symindex).adr);
                lexAnalysis.next();
            }
            else if ((symindex=searchConst(lexAnalysis.getToken()))!=-1){//寻找最近的常量
                genCode(instructionType.LIT,0,symTable.get(symindex).val);
                lexAnalysis.next();
            }
            else {//标识符未定义
                errorInfo.addNewError(11,lexAnalysis.getToken());
                lexAnalysis.next();
            }
        }
        else if(lexAnalysis.getTokenType().equals("整数常量")){//<无符号整数>
            genCode(instructionType.LIT,0,Integer.valueOf(lexAnalysis.getToken()));
            lexAnalysis.next();
        }
        else if(lexAnalysis.getToken().equals("(")){//'('<表达式>')'
            lexAnalysis.next();
            finsscopy.add(")");
            expression(finsscopy);
                if (lexAnalysis.getToken().equals(")")){
                    lexAnalysis.next();
                }
                else {
                    errorInfo.addNewError(22,lexAnalysis.getToken());//漏右括号
                }
        }
        else {
            errorInfo.addNewError(40,lexAnalysis.getToken());//应该为左括号
        }
        test(finSymSet);
    }

    private void conditionStatement(Vector<String> finSymSet) {//<条件语句> ::= if<条件>then<语句>[else<语句>]
        lexAnalysis.next();
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        finsscopy.add("then");
        condition(finsscopy);
        if (!lexAnalysis.getToken().equals("then")){
            errorInfo.addNewError(16,lexAnalysis.getToken());//应为then
        }
        if (lexAnalysis.getToken().equals("then")) {//then
            int tempcodeindex = codeIndex;//jrc指令地址
            genCode(instructionType.JPC, 0, -1);
            lexAnalysis.next();
            finsscopy.removeAllElements();
            finsscopy.addAll(finSymSet);
            finsscopy.add("else");
            statement(finsscopy);//<语句>
            pCode[tempcodeindex] = instructionType.JPC.toString() + " " + 0 + "," + (codeIndex + 1);//回填jrc地址,else语句的第一个指令地址
            tempcodeindex = codeIndex;//jmp指令地址
            genCode(instructionType.JMP, 0, -1);//跳过else语句
            if (lexAnalysis.getToken().equals("else")) {//[else<语句>]
                lexAnalysis.next();
                statement(finSymSet);
            }
            pCode[tempcodeindex] = instructionType.JMP.toString() + " " + "0" + "," + codeIndex;//回填jmp目的地址
        }
        test(finSymSet);
    }

    private void condition(Vector<String>finSymSet){//<条件> ::= <表达式><关系运算符><表达式>|odd<表达式>
        if (lexAnalysis.getToken().equals("odd") && lexAnalysis.getTokenType().equals("关键字")) {
            lexAnalysis.next();
            expression(finSymSet);
            genCode(instructionType.OPR, 0, 6);//栈顶的奇偶判断
        } else {
            Vector<String> finsscopy = new Vector<>();
            finsscopy.addAll(finSymSet);
            finsscopy.add("<");
            finsscopy.add(">");
            finsscopy.add("=");
            finsscopy.add(">=");
            finsscopy.add("<=");
            finsscopy.add("<>");
            expression(finsscopy);
            if (lexAnalysis.getToken().equals(">")) {
                lexAnalysis.next();
                expression(finSymSet);
                genCode(instructionType.OPR, 0, 11);
            } else if (lexAnalysis.getToken().equals("<")) {
                lexAnalysis.next();
                expression(finSymSet);
                genCode(instructionType.OPR, 0, 9);
            } else if (lexAnalysis.getToken().equals("=")) {
                lexAnalysis.next();
                expression(finSymSet);
                genCode(instructionType.OPR, 0, 7);
            } else if (lexAnalysis.getToken().equals("<=")) {
                lexAnalysis.next();
                expression(finSymSet);
                genCode(instructionType.OPR, 0, 12);
            } else if (lexAnalysis.getToken().equals(">=")) {
                lexAnalysis.next();
                expression(finSymSet);
                genCode(instructionType.OPR, 0, 10);
            } else if (lexAnalysis.getToken().equals("<>")) {
                lexAnalysis.next();
                expression(finSymSet);
                genCode(instructionType.OPR, 0, 8);
            } else {
                errorInfo.addNewError(20,lexAnalysis.getToken());//应该是关系运算符
            }
        }
        test(finSymSet);
    }

    private void whileStatement(Vector<String>finSymSet){//<当型循环语句> ::= while<条件>do<语句>
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        lexAnalysis.next();
        int conditionindex = codeIndex;//条件指令开始的地址
        finsscopy.add("do");
        condition(finsscopy);
        int jrcodeindex = codeIndex;//记录下jrc指令的地址
        genCode(instructionType.JPC, 0, -1);
        if (!lexAnalysis.getToken().equals("do")) {
            errorInfo.addNewError(18,lexAnalysis.getToken());//应该为do
            test(finsscopy);
        }
        lexAnalysis.next();
        statement(finSymSet);
        pCode[jrcodeindex] = instructionType.JPC.toString() + " 0," + (codeIndex + 1);//jrc跳过jump
        genCode(instructionType.JMP, 0, conditionindex);//jump到条件表达式计算的开始
        test(finSymSet);
    }

    private void callStatement(Vector<String>finSymSet){//<过程调用语句> ::= call<标识符>
        lexAnalysis.next();
        if (lexAnalysis.getTokenType().equals("标识符")) {
            int symindex;
            if ((symindex = searchProc(lexAnalysis.getToken(), instruLevel)) != -1) {//查找对应的过程符号
                genCode(instructionType.CAL, instruLevel - symTable.get(symindex).level, symTable.get(symindex).adr);
                lexAnalysis.next();
            } else if (searchConst(lexAnalysis.getToken()) != -1 || searchVar(lexAnalysis.getToken(), instruLevel) != -1) {
                errorInfo.addNewError(15,lexAnalysis.getToken());//不可调用常量或变量
            }
        } else {
            errorInfo.addNewError(14,lexAnalysis.getToken());//call后面应该是标识符
        }
        test(finSymSet);
    }

    private void readStatement(Vector<String>finSymSet) {//<读语句> ::= read'('<标识符>{,<标识符>}')'
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        lexAnalysis.next();
        if (lexAnalysis.getTokenType().equals("分界符") && lexAnalysis.getToken().equals("(")) {
            lexAnalysis.next();
            finsscopy.add(",");
            finsscopy.add(")");
            while (!lexAnalysis.getTokenType().equals("标识符")) {
                test(finsscopy);//跳到下一个合法的字符
                if (lexAnalysis.getToken().equals(","))
                    lexAnalysis.next();
                else if (lexAnalysis.getToken().equals(")")){//如果是）需要分析下一个
                    lexAnalysis.next();
                    break;
                }
                else break;
            }

            if (lexAnalysis.getTokenType().equals("标识符")){
                int symindex;
                if ((symindex = searchVar(lexAnalysis.getToken(), instruLevel)) != -1) {//查找符号表中的符号
                    genCode(instructionType.RED, instruLevel - symTable.get(symindex).level, symTable.get(symindex).adr);
                    lexAnalysis.next();
                    finsscopy.add(",");
                    finsscopy.add(")");
                    while (lexAnalysis.getToken().equals(",")) {//{,<标识符>}
                        lexAnalysis.next();
                        if (lexAnalysis.getTokenType().equals("标识符")) {
                            if ((symindex = searchVar(lexAnalysis.getToken(), instruLevel)) != -1) {//查找符号表中的符号
                                genCode(instructionType.RED, instruLevel - symTable.get(symindex).level, symTable.get(symindex).adr);
                                lexAnalysis.next();
                            } else if (searchConst(lexAnalysis.getToken()) != -1 || searchProc(lexAnalysis.getToken(), instruLevel) != -1) {//此标识符是常量或者过程
                                errorInfo.addNewError(12, lexAnalysis.getToken());
                            } else {
                                errorInfo.addNewError(11, lexAnalysis.getToken());
                            }
                        } else {//再出现，的情况下标识符错误
                            test(finsscopy);
                        }
                    }
                    if (lexAnalysis.getToken().equals(")")) {
                        lexAnalysis.next();
                    } else {//缺少)
                        errorInfo.addNewError(22, lexAnalysis.getToken());
                    }
                } else {//标识符未声明
                    errorInfo.addNewError(11, lexAnalysis.getToken());
                }
            }
        } else {//缺少(
            errorInfo.addNewError(40, lexAnalysis.getToken());
        }
        test(finSymSet);
    }

    private void writeStatement(Vector<String>finSymSet) {//<写语句> ::= write'('<标识符>{,<标识符>}')'
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        finsscopy.add(")");
        finsscopy.add(",");
        lexAnalysis.next();
        if (lexAnalysis.getToken().equals("(")) {
            lexAnalysis.next();
            test(finsscopy);
            if (lexAnalysis.getTokenType().equals("标识符")||lexAnalysis.getToken().equals(",")) {//得到标识符,或者开始一个,
                int tempindex;
                if(lexAnalysis.getTokenType().equals("标识符")){//得到的十一标识符
                    if ((tempindex = searchConst(lexAnalysis.getToken())) != -1 || (tempindex = searchVar(lexAnalysis.getToken(), instruLevel)) != -1) {//标识符是常量或者变量
                        if ((tempindex = searchConst(lexAnalysis.getToken())) != -1) {//查找最近的常量
                            genCode(instructionType.LIT, 0, symTable.get(tempindex).val);
                            genCode(instructionType.WRT, 0, 0);
                        } else if ((tempindex = searchVar(lexAnalysis.getToken(), instruLevel)) != -1) {//查找最近的变量
                            genCode(instructionType.LOD, instruLevel - symTable.get(tempindex).level, symTable.get(tempindex).adr);
                            genCode(instructionType.WRT, 0, 0);
                        }
                        lexAnalysis.next();
                    } else {
                        errorInfo.addNewError(11, lexAnalysis.getToken());//标识符未说明
                        lexAnalysis.next();
                        test(finsscopy);
                    }
                }
                while (lexAnalysis.getToken().equals(",")) {//{,<标识符>}
                    lexAnalysis.next();
                    if (lexAnalysis.getTokenType().equals("标识符")) {
                        if ((tempindex = searchConst(lexAnalysis.getToken())) != -1) {//标识符是常量
                            genCode(instructionType.LIT, 0, symTable.get(tempindex).val);
                            genCode(instructionType.WRT, 0, 0);
                            lexAnalysis.next();
                        } else if ((tempindex = searchVar(lexAnalysis.getToken(), instruLevel)) != -1) {//标识符是变量
                            genCode(instructionType.LOD, instruLevel - symTable.get(tempindex).level, symTable.get(tempindex).adr);
                            genCode(instructionType.WRT, 0, 0);
                            lexAnalysis.next();
                        } else {
                            errorInfo.addNewError(11, lexAnalysis.getToken());//标识符未声明
                        }
                    } else {
                        errorInfo.addNewError(11, lexAnalysis.getToken());//标识符未说明
                    }
                    test(finsscopy);
                }
                if (lexAnalysis.getToken().equals(")")) {
                    lexAnalysis.next();
                } else {
                    errorInfo.addNewError(22, lexAnalysis.getToken());//漏掉右括号
                }
            }
        }
        else {
            errorInfo.addNewError(40, lexAnalysis.getToken());//应该为左括号
        }
        test(finSymSet);
    }


    private void compoundStatement(Vector<String>finSymSet){//<复合语句> ::= begin<语句>{;<语句>}end
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        lexAnalysis.next();
        finsscopy.add(";");
        finsscopy.add("end");
        if (statement(finsscopy) != -1) {
            while (lexAnalysis.getToken().equals(";")||searchFinSym(statBegSys)||lexAnalysis.getTokenType().equals("标识符")) {
                if(searchFinSym(statBegSys)||lexAnalysis.getTokenType().equals("标识符")){
                    errorInfo.addNewError(10,lexAnalysis.getToken());//语句之间漏分号
                }
                if (lexAnalysis.getToken().equals(";"))//如果是下一个语句就不跳过
                    lexAnalysis.next();
                if (statement(finsscopy) == -1) {
                    break;//语句错误
                }
            }
            if (lexAnalysis.getToken().equals("end") && lexAnalysis.getTokenType().equals("关键字")) {
                lexAnalysis.next();
            } else {
                errorInfo.addNewError(17,lexAnalysis.getToken());//缺失end
            }
        }
        test(finSymSet);
    }

    private void repeatStatement(Vector<String>finSymSet){//<重复语句> ::= repeat<语句>{;<语句>}until<条件>
        Vector<String> finsscopy = new Vector<>();
        finsscopy.addAll(finSymSet);
        lexAnalysis.next();
        int jumpdes = codeIndex;//jump的目标指令地址，语句的第一条地址
        finsscopy.add(";");
        finsscopy.add("until");
        if (statement(finsscopy) != -1) {
            while (lexAnalysis.getToken().equals(";")) {
                lexAnalysis.next();
                statement(finsscopy);
            }
            if (lexAnalysis.getToken().equals("until")) {
                lexAnalysis.next();
                condition(finSymSet);
                genCode(instructionType.JPC, 0, codeIndex + 2);//jrc跳过jump
                genCode(instructionType.JMP, 0, jumpdes);//跳回去执行语句
            } else {
                errorInfo.addNewError("缺少until");
            }
        }
        test(finSymSet);
    }

    private int searchConst(String name){//查找符号表中的常量是否重复
        for(int i=0;i<symTable.size();++i){
            if(symTable.get(i).kind==symTableItemType.CONST){
                if(symTable.get(i).name.equals(name))
                    return i;
            }
            else return -1;
        }
        return -1;
    }

    private int existVarProcChecking(symTableItem tempsym){//查找符号表中的变量和过程是否重复,从末端开始找，直至嵌套层次不等
        for(int i=symTable.size()-1;i>=0;--i){
            if(symTable.get(i).level==tempsym.level){
                if(symTable.get(i).name.equals(tempsym.name)){
                    return i;
                }
            }
            else return -1;
        }
        return -1;
    }

    private int searchVar(String name,int level){//寻找最近的变量
        for(int i=symTable.size()-1;i>=0;--i){
            if(symTable.get(i).level==level){
                if(symTable.get(i).name.equals(name)&&symTable.get(i).kind==symTableItemType.VAR){
                    return i;
                }
            }
            else if(symTable.get(i).level<level){
                level--;
                if(symTable.get(i).name.equals(name)&&symTable.get(i).kind==symTableItemType.VAR){
                    return i;
                }
            }
            else if(symTable.get(i).level>level){
                continue;
            }
        }
        return -1;
    }

    private int searchProc(String name,int level){
        for(int i=symTable.size()-1;i>=0;--i){
            if(symTable.get(i).level==level){
                if(symTable.get(i).name.equals(name)&&symTable.get(i).kind==symTableItemType.PROC){
                    return i;
                }
            }
            else if(symTable.get(i).level<level){
                level--;
                if(symTable.get(i).name.equals(name)&&symTable.get(i).kind==symTableItemType.PROC){
                    return i;
                }
            }
            else if(symTable.get(i).level>level){
                continue;
            }
        }
        return -1;
    }

    private int countLevelVarNumber(int level){//查找相同block下所有的变量个数
        int count=0;
        for(int i=symTable.size()-1;i>=0;--i){
            if(symTable.get(i).level==level){
                if(symTable.get(i).kind.equals(symTableItemType.VAR)){
                    count++;
                }
            }
            else if(symTable.get(i).level>level){
                continue;
            }
            else return count;
        }
        return count;
    }
}
