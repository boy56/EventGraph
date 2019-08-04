package repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *  以句子为单位的信息描述
 * @Author boy56
 * @Date 2018-10-30
 */
public class SentenceInfo {
    public String id;
    public String text;

    public List<WordInfo> wordsList;  //分词结果

    public List<String> keywordsList;   //关键词
    public List<Trigger> triggerList;    //触发词
    public List<String> personList;     //人名
    public List<String> locationList;   //地名
    public List<String> organizationList;   //机构名

    public SentenceInfo(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public List<String> getWordList(){
        List<String> resultList = new ArrayList<>();
        for(WordInfo wordInfo : wordsList){
            resultList.add(wordInfo.name);
        }
        return resultList;
    }

}
