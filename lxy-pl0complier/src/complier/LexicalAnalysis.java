package complier;

import java.io.*;

public class LexicalAnalysis {
    private BufferedReader br;
    private BufferedWriter bw;

    private String[] reservedWord = {"const", "var", "procedure", "odd", "if","then","else","while","do","call","begin","end","repeat","until","read","write"};//保留字

    private StringBuffer savedToken = new StringBuffer();  //stringbuffer是一个可变量,保存的token

    private enum TokenType {
        EMPTY, IDENTIFER, CONSTANTOFINT, CONSTANTOFDECIMAL,DELIMITER,DOUBLECHAROPERATER,SINGLECHAROPERATER,ERRORTOKEN,COLON
    }

    private TokenType savedTokenType = TokenType.EMPTY;//token的类型

    private int handelSym;   //当前读进的字符的AScII码表示

    private enum SymTye {
        CHAR, NUM, PLUS, MINUS, MULTIPLY, DIVIDE, EQUAL, BIGGER, SMALLER, BRACKET, SEMICOLON,COLON, DOT, COMMA,NOUSE, END,INVALIDSYM
    }

    private SymTye handelSymType;//读入字符的类型
    private String returnToken=new String();    //用于语法分析的token
    private TokenType returnTokenType=TokenType.EMPTY;      //用于语法分析的token类型

    public LexicalAnalysis(String inputFile, String outputFile) {         //初始化读文件与写文件
        try {
            br = new BufferedReader(new FileReader(inputFile));
            bw = new BufferedWriter(new FileWriter(outputFile));
            bw.write("单词");
            bw.write(9);//tab
            bw.write("类别");
            bw.write(9);
            bw.write("值");
            bw.write(10);//换行
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public LexicalAnalysis(String inputFile){           //初始化读文件
        try{
            br = new BufferedReader(new FileReader(inputFile));
        }catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    private void returnToken(){         //得到一个送给语法分析的token
        returnToken=savedToken.toString();
        returnTokenType=savedTokenType;

        savedToken.delete(0,savedToken.length());
        savedTokenType=TokenType.EMPTY;
    }

    public String getToken(){   //获得token值
        return returnToken;
    }

    public String getTokenType(){   //获得tokentype
        switch (returnTokenType){
            case IDENTIFER://标识符
                if(reserve(returnToken.toString())){
                    return  "关键字";
                }else {
                    return "标识符";
                }
            case CONSTANTOFINT:
                return "整数常量";
            case CONSTANTOFDECIMAL:
                return "小数常量";
            case DELIMITER:
                return "分界符";
            case DOUBLECHAROPERATER:
                return "双字符运算符";
            case SINGLECHAROPERATER:
                return "单字符运算符";
            case ERRORTOKEN:
                return "错误的字符";
            case EMPTY:         //唯一可能是空的情况是程序分析完毕
                return "空";
            default:
                return "空";
        }
    }

    public void next() {             //每次找到一个token
        boolean getnewtoken=false;
        while(!getnewtoken && (handelSymType = scanner()) != SymTye.END) {
            switch (handelSymType) {
                case CHAR:
                    if (savedTokenType == TokenType.EMPTY || savedTokenType == TokenType.IDENTIFER) {
                        catToken();
                        savedTokenType = TokenType.IDENTIFER;//标识符
                    } else {
                        if(savedTokenType!=TokenType.EMPTY){
                            returnToken();
                            catToken();
                            savedTokenType = TokenType.IDENTIFER;//标识符
                            getnewtoken=true;
                        }
                        else {
                            catToken();
                            savedTokenType = TokenType.IDENTIFER;//标识符
                        }
                    }
                    break;
                case NUM:
                    if (savedTokenType == TokenType.EMPTY) {
                        catToken();
                        savedTokenType = TokenType.CONSTANTOFINT;//常量
                    } else if (savedTokenType == TokenType.CONSTANTOFINT || savedTokenType == TokenType.IDENTIFER || savedTokenType == TokenType.CONSTANTOFDECIMAL) {
                        catToken();
                    } else {
                        returnToken();
                        catToken();
                        savedTokenType = TokenType.CONSTANTOFINT;//常量
                        getnewtoken=true;
                    }
                    break;
                case PLUS:// +
                case MINUS:// -
                case MULTIPLY:// *
                case DIVIDE://  /
                    if(savedTokenType!=TokenType.EMPTY){
                        returnToken();
                        catToken();
                        savedTokenType = TokenType.SINGLECHAROPERATER;
                        getnewtoken=true;
                        break;
                    }
                    else {
                        catToken();
                        savedTokenType = TokenType.SINGLECHAROPERATER;
                        break;
                    }
                case BIGGER:        // >
                    if (savedToken.toString().equals("<")) {// <>
                        catToken();
                        savedTokenType = TokenType.DOUBLECHAROPERATER;
                    } else { // >
                        if(savedTokenType!=TokenType.EMPTY){
                            returnToken();
                            catToken();
                            savedTokenType = TokenType.SINGLECHAROPERATER;
                            getnewtoken=true;
                        }
                        else {
                            catToken();
                            savedTokenType = TokenType.SINGLECHAROPERATER;
                        }
                    }
                    break;
                case SMALLER:// <
                    if(savedTokenType!=TokenType.EMPTY){
                        returnToken();
                        catToken();
                        getnewtoken=true;
                    }
                    else {
                        catToken();
                    }
                    savedTokenType = TokenType.SINGLECHAROPERATER;
                    break;
                case EQUAL:
                    if (savedToken.toString().equals(":")) {// :=
                        catToken();
                        savedTokenType = TokenType.DOUBLECHAROPERATER;
                    } else if (savedToken.toString().equals("<")) {// <=
                        catToken();
                        savedTokenType = TokenType.DOUBLECHAROPERATER;
                    } else if (savedToken.toString().equals(">")) {// >=
                        catToken();
                        savedTokenType = TokenType.DOUBLECHAROPERATER;
                    } else {// =
                        if(savedTokenType!=TokenType.EMPTY){
                            getnewtoken=true;
                        }
                        returnToken();
                        catToken();
                        savedTokenType = TokenType.SINGLECHAROPERATER;
                    }
                    break;
                case BRACKET:// (),{}
                case SEMICOLON:// ;
                case COMMA:// ,
                    if(savedTokenType!=TokenType.EMPTY){
                        getnewtoken=true;
                    }
                    returnToken();
                    catToken();
                    savedTokenType = TokenType.DELIMITER;//分界符
                    break;
                case DOT:// .
                    if (savedTokenType != TokenType.EMPTY && savedToken.charAt(savedToken.length() - 1) >= 48 && savedToken.charAt(savedToken.length() - 1) <= 57) {
                        //此时是小数点
                        catToken();
                        savedTokenType = TokenType.CONSTANTOFDECIMAL;
                        break;
                    } else {//此时是分界符
                        if(savedTokenType!=TokenType.EMPTY){
                            getnewtoken=true;
                        }
                        returnToken();
                        catToken();
                        savedTokenType = TokenType.DELIMITER;//分界符
                        break;
                    }
                case COLON:// :
                    if(savedTokenType!=TokenType.EMPTY){
                        getnewtoken=true;
                    }
                    returnToken();
                    catToken();
                    savedTokenType = TokenType.ERRORTOKEN;
                    break;
                case NOUSE://遇到空白的输出token
                    if(savedTokenType!=TokenType.EMPTY){
                        returnToken();
                        getnewtoken=true;
                        break;
                    }
                    else break;
                case INVALIDSYM:
                    if(savedTokenType!=TokenType.EMPTY){
                        getnewtoken=true;
                    }
                    returnToken();
                    catToken();
                    savedTokenType = TokenType.ERRORTOKEN;
                    break;
            }
        }
        if(handelSymType==SymTye.END){
            returnToken();
        }
    }

    public void run() {             //向写文件当中输入token流,把源程序完全扫描一遍
        while ((handelSymType = scanner()) != SymTye.END) {
            switch (handelSymType) {
                case CHAR:
                    if (savedTokenType == TokenType.EMPTY || savedTokenType == TokenType.IDENTIFER){
                        catToken();
                        savedTokenType=TokenType.IDENTIFER;//标识符
                    } else{
                        output();
                        catToken();
                        savedTokenType=TokenType.IDENTIFER;//标识符
                    }
                    break;
                case NUM:
                    if (savedTokenType == TokenType.EMPTY){
                        catToken();
                        savedTokenType=TokenType.CONSTANTOFINT;//常量
                    }else if( savedTokenType==TokenType.CONSTANTOFINT||savedTokenType == TokenType.IDENTIFER||savedTokenType==TokenType.CONSTANTOFDECIMAL){
                        catToken();
                    }else{
                        output();
                        catToken();
                        savedTokenType=TokenType.CONSTANTOFINT;//常量
                    }
                    break;
                case PLUS:
                case MINUS:
                case MULTIPLY:
                case DIVIDE:
                    output();
                    catToken();
                    savedTokenType=TokenType.SINGLECHAROPERATER;
                    break;
                case BIGGER:
                    if(savedToken.toString().equals("<")){// <>
                        catToken();
                        savedTokenType=TokenType.DOUBLECHAROPERATER;
                    }else { // >
                        output();
                        catToken();
                        savedTokenType=TokenType.SINGLECHAROPERATER;
                    }
                    break;
                case SMALLER://<
                    output();
                    catToken();
                    savedTokenType=TokenType.SINGLECHAROPERATER;
                    break;
                case EQUAL:
                    if(savedToken.toString().equals(":")){// :=
                        catToken();
                        savedTokenType=TokenType.DOUBLECHAROPERATER;
                    } else if(savedToken.toString().equals("<")){// <=
                        catToken();
                        savedTokenType=TokenType.DOUBLECHAROPERATER;
                    } else if(savedToken.toString().equals(">")){// >=
                        catToken();
                        savedTokenType=TokenType.DOUBLECHAROPERATER;
                    }
                    else {// =
                        output();
                        catToken();
                        savedTokenType=TokenType.SINGLECHAROPERATER;
                    }
                    break;
                case BRACKET:// (),{}
                case SEMICOLON:// ;
                case COMMA:// ,
                    output();
                    catToken();
                    savedTokenType=TokenType.DELIMITER;//分界符
                    break;
                case DOT:// .
                    if(savedTokenType!=TokenType.EMPTY&&savedToken.charAt(savedToken.length()-1)>=48&&savedToken.charAt(savedToken.length()-1)<=57){
                        //此时是小数点
                        catToken();
                        savedTokenType=TokenType.CONSTANTOFDECIMAL;
                        break;
                    }
                    else {//此时是分界符
                        output();
                        catToken();
                        savedTokenType=TokenType.DELIMITER;//分界符
                        break;
                    }
                case COLON:// :
                    output();
                    catToken();
                    savedTokenType=TokenType.ERRORTOKEN;
                    break;
                case NOUSE://遇到空白的输出token
                    output();
                    break;
                case INVALIDSYM:
                    output();
                    catToken();
                    savedTokenType=TokenType.ERRORTOKEN;
                    break;
            }
        }
        output();//防止有没有输出的token
        try {
            bw.flush();
            bw.close();
            br.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    private SymTye scanner() {
        try {
            if ((handelSym = br.read()) != -1) {//读入sym
                if (isSpace() || isNewline() || isTab()||isEnter()) {
                    return SymTye.NOUSE;
                } else if (isLetter()) {
                    return SymTye.CHAR;
                } else if (isDigit()) {
                    return SymTye.NUM;
                } else if (isEqu()) {
                    return SymTye.EQUAL;
                } else if (isPlus()) {
                    return SymTye.PLUS;
                } else if (isMinux()) {
                    return SymTye.MINUS;
                } else if (isMulity()) {
                    return SymTye.MULTIPLY;
                } else if (isDivi()) {
                    return SymTye.DIVIDE;
                } else if (isBracket() || isSemi()) {//分号或者括号
                    return SymTye.BRACKET;
                } else if (isSemi()){
                    return SymTye.SEMICOLON;
                } else if (isBigger()){
                    return SymTye.BIGGER;
                } else if (isSmall()){
                    return SymTye.SMALLER;
                } else if (isColon()){
                    return SymTye.COLON;
                } else if(isDot()){
                    return SymTye.DOT;
                }else if(isComma()){
                    return SymTye.COMMA;
                }
                else{
                    return SymTye.INVALIDSYM;
                }

            } else {
                return SymTye.END;
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        return SymTye.NOUSE;
    }

    private void catToken() {
        savedToken.append((char) handelSym);
    }

    private boolean reserve(String identifer) {
        for (int i = 0; i < reservedWord.length; i++) {
            if (identifer.equals(reservedWord[i])) {
                return true;
            }
        }
        return false;
    }

    private void output(){
        try{
            if(savedTokenType!=TokenType.EMPTY){
                bw.write(savedToken.toString());
                bw.write(9);
                switch (savedTokenType){
                    case IDENTIFER://标识符
                        if(reserve(savedToken.toString())){
                            bw.write("关键字");
                        }else {
                            bw.write("标识符");
                        }
                        bw.write(9);
                        bw.write(savedToken.toString());
                        break;
                    case CONSTANTOFINT:
                        bw.write("整数常量");
                        bw.write(9);
                        bw.write(Integer.toBinaryString(Integer.parseInt(savedToken.toString())));//二进制输出
                        break;
                    case CONSTANTOFDECIMAL:
                        bw.write("小数常量");
                        bw.write(9);
                        bw.write(Long.toBinaryString(Double.doubleToLongBits(Double.valueOf(savedToken.toString()))));//二进制输出
                        break;
                    case DELIMITER:
                        bw.write("分界符");
                        bw.write(9);
                        bw.write(savedToken.toString());
                        break;
                    case DOUBLECHAROPERATER:
                        bw.write("双字符运算符");
                        bw.write(9);
                        bw.write(savedToken.toString());
                        break;
                    case SINGLECHAROPERATER:
                        bw.write("单字符运算符");
                        bw.write(9);
                        bw.write(savedToken.toString());
                        break;
                    case ERRORTOKEN:
                        bw.write("错误的字符");
                }
                bw.write(10);//换行
                savedToken.delete(0,savedToken.length());
                savedTokenType=TokenType.EMPTY;
            }

        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isSpace() {
        if (handelSym == 32) //32是空格的ASCII码值
            return true;
        else return false;
    }

    private boolean isEnter() {
        if (handelSym == 13) //13是回车的ASCII码值
            return true;
        else return false;
    }

    private boolean isNewline() {
        if (handelSym == 10) //10是换行的ASCII码值
            return true;
        else return false;
    }

    private boolean isTab() {
        if (handelSym == 9) //9是TAB的ASCII码值
            return true;
        else return false;
    }

    private boolean isLetter() {
        if ((handelSym >= 65 && handelSym <= 90) || (handelSym >= 97 && handelSym <= 122)) {
            return true;
        }
        return false;
    }

    private boolean isDigit() {
        if (handelSym >= 48 && handelSym <= 57) {
            return true;
        }
        return false;
    }

    private boolean isEqu() {
        if (handelSym == 61)
            return true;
        else return false;
    }

    private boolean isPlus() {
        if (handelSym == 43)
            return true;
        else return false;
    }

    private boolean isMinux() {
        if (handelSym == 45)
            return true;
        else return false;
    }

    private boolean isMulity() {
        if (handelSym == 42)
            return true;
        else return false;
    }

    private boolean isDivi() {
        if (handelSym == 47)
            return true;
        else return false;
    }

    private boolean isBracket() {//括号
        if (handelSym == 40 || handelSym == 41 || handelSym == 123 || handelSym == 125)
            return true;
        else return false;
    }

    /*public boolean isLpar(){
        if(handelSym==40)
            return true;
        else return false;
    }
    public boolean isRpar(){
        if(handelSym==41)
            return true;
        else return false;
    }
    public boolean isLbrace(){
        if(handelSym==123)
            return true;
        else return false;
    }
    public boolean isRbrace(){
        if(handelSym==125)
            return true;
        else return false;
    }*/
    private boolean isComma() {//逗号
        if (handelSym == 44)
            return true;
        else return false;
    }

    private boolean isSemi() {//分号
        if (handelSym == 59)
            return true;
        else return false;
    }

    private boolean isDot() {//点号
        if (handelSym == 46)
            return true;
        else return false;
    }

    private boolean isColon() {//冒号
        if (handelSym == 58)
            return true;
        else return false;
    }
    private boolean isBigger() {//大于
        if (handelSym == 62)
            return true;
        else return false;
    }
    private boolean isSmall() {//小于
        if (handelSym == 60)
            return true;
        else return false;
    }
}

