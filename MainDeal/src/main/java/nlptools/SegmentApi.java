package nlptools;

import java.util.List;

/**
 * 描述:
 *    NLP 分词接口
 * @Author boy56
 * @Date 2018-10-12
 */
public interface SegmentApi {
    /**
     *  分词函数, 将text文本进行分词处理, 将分词结果按顺序放在String[]数组中
     * @param text
     * @return 分词结果列表
     */
    List<String> wordSeg(String text);
}
