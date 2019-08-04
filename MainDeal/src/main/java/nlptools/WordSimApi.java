package nlptools;

/**
 * 描述:
 *  计算单词相似度的API
 * @Author boy56
 * @Date 2018-11-04
 */
public interface WordSimApi {
    /**
     *  计算s1和s2单词的相似度
     * @param s1 单词s1
     * @param s2 单词s2
     * @return 返回s1与s2的相似度评分[0, 1]
     */
    double getWordSimilarity(String s1, String s2);
}
