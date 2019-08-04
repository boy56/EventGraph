package nlptools;

import main.GlobalVar;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 描述:
 *  一些常用函数
 * @Author boy56
 * @Date 2018-10-30
 */
public class Function {

    private static Set<String> stopWordSet;
    private static Function instance;   //单例模式


    public Function() {
        stopWordSet = loadStopWord();
    }

    /**
     *  从单词文本的角度进行过滤,
     * @param word 单词
     * @return 如果该单词需要被过滤掉则返回true, 否则返回false
     */
    public boolean filterWordByText(String word){
        if(word.length() < 2) return true;
        if(stopWordSet.contains(word)) return true;
        if(hasNonChineseChar(word)) return true;
        return false;
    }



    /**
     * 加载停用词  dict/stopword.txt
     * @return  停用词典
     */
    private Set<String> loadStopWord(){
        Set<String> stopWordsSet = new HashSet<>();
        try(InputStreamReader isr = new InputStreamReader(new FileInputStream(GlobalVar.getProp().getProperty("StopWordDictFile")),"UTF-8")){
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                stopWordsSet.add(line);
            }

        }catch (IOException e){
            e.printStackTrace();
        }
        return stopWordsSet;
    }

    /**
     *  查看单词s是否包含非中文字符
     * @param s 单词
     * @return 包含非中文字符，返回true，否则返回false
     */
    private boolean hasNonChineseChar(String s){
        for(int i = 0; i < s.length(); i++){
            if(!Character.toString(s.charAt(i)).matches("[\\u4E00-\\u9FA5]+")) return true;
        }
        return false;
    }

    /**
     *
     * @param date
     * @return
     */
    public String DateToString(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(date == null) return "";
        return sdf.format(date);
    }


    /**
     *
     * @param s
     * @return
     */
    public Date StringToDate(String s){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        if(s.equals("")) return null;
        try {
            date = sdf.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Function getInstance() {
        //单例模式
        if (instance == null) {
            synchronized (Function.class) {
                instance = new Function();
            }
        }
        return instance;
    }

}
