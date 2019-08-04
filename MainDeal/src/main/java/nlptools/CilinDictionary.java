package nlptools;

import main.GlobalVar;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 描述:
 *  加载同义词词典
 *  代码地址: https://github.com/sea-boat/TextAnalyzer/blob/master/src/main/java/com/seaboat/text/analyzer/cilin/CilinDictionary.java
 * @Author boy56
 * @Date 2018-11-04
 */
public class CilinDictionary {
    protected static Logger logger = Logger.getLogger(CilinDictionary.class);
    private static Map<String, Set<String>> wordIndex = new HashMap<String, Set<String>>();
    private static Map<String, Set<String>> codeIndex = new HashMap<String, Set<String>>();
    private static CilinDictionary instance;
    private static String path = GlobalVar.getProp().getProperty("CilinDictFile");

    private CilinDictionary() {
        try {
            //放到resources中的写法
            //InputStreamReader read = new InputStreamReader(this.getClass().getResourceAsStream(path), "UTF-8");
            InputStreamReader read = new InputStreamReader(new FileInputStream(path), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(read);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] items = line.split(" ");
                Set<String> set = new HashSet<String>();
                for (int i = 1; i < items.length; i++) {
                    String code = items[i].trim();
                    if (!code.equals("")) {
                        set.add(code);
                        Set<String> codeWords = codeIndex.get(code);
                        if (codeWords == null) {
                            codeWords = new HashSet<String>();
                        }
                        codeWords.add(items[0]);
                        codeIndex.put(code, codeWords);
                    }
                }
                wordIndex.put(items[0], set);
            }
        } catch (Exception e) {
            logger.error("error occurs when loading cilin ....", e);
        }
    }

    public Set<String> getCilinCoding(String word) {
        return codeIndex.get(word);
    }

    public Set<String> getCilinWords(String code) {
        return wordIndex.get(code);
    }

    public static CilinDictionary getInstance() {
        //单例模式
        if (instance == null) {
            synchronized (CilinDictionary.class) {
                instance = new CilinDictionary();
            }
        }
        return instance;
    }
}
