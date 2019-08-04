import main.GlobalVar;
import nlptools.CilinSimilarity;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 描述:
 * foo
 *
 * @Author boy56
 * @Date 2018-10-16
 */
public class Foo {
    public static void main(String[] args) {
        File file = new File("cache");
        try {
            OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream("outInfo/words.txt"), "UTF-8");
            for(File f : file.listFiles()){
                System.out.println(f.getName());
                try (InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8")) {//打开data
                    BufferedReader br = new BufferedReader(isr);

                    String line;
                    while((line = br.readLine())!=null){
                        if(line.equals("---------")) continue;
                        String[] words = line.split(" ");
                        StringBuffer stringBuffer = new StringBuffer();
                        for(String word : words){
                            stringBuffer.append(word.split("\\|")[0]);
                            stringBuffer.append(" ");
                        }
                        stringBuffer.append("\n");
                        fw.write(stringBuffer.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }




    }

    private void creatStopWords(){
        String srcPath = "dict/stopwords";
        File fileFolder = new File(srcPath);
        File[] fileList = fileFolder.listFiles();
        HashSet<String> stopWordsSet = new HashSet<>();

        assert fileList != null;
        for (File file : fileList) {
            System.out.println(file.getName());
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8")) {//打开data
                BufferedReader br = new BufferedReader(isr);
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        //System.out.println(line);
                        stopWordsSet.add(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(stopWordsSet.size());
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream("dict/stopword.txt"), "UTF-8")) {//输出新闻预处理文件
            for(String s : stopWordsSet){
                fw.write(s + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeArray(int[][] array){
        System.out.println(array.length);
        for(int i = 0; i < array.length; i++){
            array[i][0]++;
           // System.out.println(array[i]);
        }
    }


}

