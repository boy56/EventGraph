package nlptools;

import repository.DataInfo;
import repository.EventInfo;
import repository.SentenceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *     进行文本实体抽取、关系抽取的接口
 * @Author boy56
 * @Date 2018-07-01
 */
public interface NLPAPi {

    /**
     * 对DataInfo中的title和content属性进行分词与词性标注
     * @param dataInfo 新闻篇章级数据
     */
    void wordsSeg(DataInfo dataInfo);

    /**
     *  以句子为单位进行分词与词性标注
     * @param sentenceInfo 句子级数据
     */
    void wordsSeg(SentenceInfo sentenceInfo);

    /**
     *  对DataInfo中的title和content进行关键词提取
     * @param dataInfo 新闻篇章级数据
     */
    void keywordsExtract(DataInfo dataInfo);

    /**
     *  对SentenceInfo进行关键词抽取
     * @param sentenceInfo 句子级数据
     */
    void keywordsExtract(SentenceInfo sentenceInfo);


    /**
     * 对数据进行实体抽取, 在使用wordsExtract 以及 wordsPos功能之后使用
     * @param dataInfo  新闻篇章级数据
     */
    void entityExtract(DataInfo dataInfo);

    /**
     * 以句子为单位进行实体抽取
     * @param sentenceInfo 句子级数据
     */
    void entityExtract(SentenceInfo sentenceInfo);

    /**
     * 对数据进行触发词抽取
     * @param dataInfo 新闻篇章级数据
     */
    void triggerExtract(DataInfo dataInfo);

    /**
     * 以句子为单位进行触发词抽取
     * @param sentenceInfo 句子级数据
     */
    void triggerExtract(SentenceInfo sentenceInfo);
}
