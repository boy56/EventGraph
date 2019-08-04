package nlptools;

import java.util.Set;
import org.apache.log4j.Logger;

/**
 * 描述:
 *   依据《同义词林》计算词语相似度
 *   参考博客: https://blog.csdn.net/wangyangzhizhou/article/details/80837829
 *   代码地址: https://github.com/sea-boat/TextAnalyzer/blob/master/src/main/java/com/seaboat/text/analyzer/similarity/CilinSimilarity.java
 * @Author boy56
 * @Date 2018-11-04
 */
public class CilinSimilarity implements WordSimApi {
    protected static Logger logger = Logger.getLogger(CilinSimilarity.class);
    private static double[] WEIGHT = new double[] { 1.2, 1.2, 1.0, 1.0, 0.8, 0.4 };
    private static double TOTAL_WEIGHT = 5.6;

    @Override
    public double getWordSimilarity(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 1.0;
        } else if (s1 == null || s2 == null) {
            return 0.0;
        } else if (s1.equalsIgnoreCase(s2)) {
            return 1.0;
        }
        //每一个单词可能对应多个编码
        Set<String> codeSet1 = CilinDictionary.getInstance().getCilinCoding(s1);
        Set<String> codeSet2 = CilinDictionary.getInstance().getCilinCoding(s2);

        if (codeSet1 == null || codeSet2 == null) {
            return 0.0;
        }
        double similarity = 0.0;
        for (String code1 : codeSet1) {
            for (String code2 : codeSet2) {
                double s = sumWeight(code1, code2) / TOTAL_WEIGHT;
                logger.debug(code1 + "-" + code2 + "-" + sumWeight(code1, code2));
                if (similarity < s)
                    similarity = s;
            }
        }
        return similarity;
    }

    private static double sumWeight(String code1, String code2) {
        double weight = 0.0;
        for (int i = 1; i <= 6; i++) {
            String c1 = getLevelCode(code1, i);
            String c2 = getLevelCode(code2, i);
            if (c1.equals(c2)) {
                weight += WEIGHT[i - 1];
            } else {
                break;
            }
        }
        return weight;
    }

    private static String getLevelCode(String code, int level) {
        switch (level) {
            case 1:
                return code.substring(0, 1);
            case 2:
                return code.substring(1, 2);
            case 3:
                return code.substring(2, 4);
            case 4:
                return code.substring(4, 5);
            case 5:
                return code.substring(5, 7);
            case 6:
                return code.substring(7);
        }
        return "";
    }


}
