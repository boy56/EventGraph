package repository;

import java.util.*;

/**
 * 描述:
 *  数据信息描述
 * @Author boy56
 * @Date 2018-10-16
 */

public class DataInfo {
    //原始数据信息
    private String id;

    private String content;
    private String title;
    private String time;
    private String source;

    private List<String> keyWordsList;  //以文档为单位处理得到的关键词列表


    //新加入sentenceInfo
    public SentenceInfo titleSentence;
    public List<SentenceInfo> contentSentenceList;


    public DataInfo(String time, String title, String content){
        this.content = content;
        this.title = title;
        this.time = time;
    }

    public DataInfo(String id, String time, String source, String title, String content){
        this.id = id;
        this.content = content;
        this.title = title;
        this.time = time;
        this.source=source;
    }

    //set方法
    public void setId(String id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setKeyWordsList(List<String> keyWordsList) {
        this.keyWordsList = keyWordsList;
    }

    //get方法
    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }

    public String getSource() {
        return source;
    }

    public List<String> getKeyWordsList() {
        return keyWordsList;
    }

    //获取以文档为单位的各项信息, 从句子中得到
    public List<WordInfo> getWordList(){
        List<WordInfo> resultWordInfo = new ArrayList<>();
        resultWordInfo.addAll(this.titleSentence.wordsList);
        for(SentenceInfo sentenceInfo : this.contentSentenceList){
            resultWordInfo.addAll(sentenceInfo.wordsList);
        }

        return resultWordInfo;
    }

    public List<Trigger> getTriggerList() {

        //前后调节, 筛选
        List<Trigger> triggerList = new ArrayList<>();
        if(this.titleSentence.triggerList != null) triggerList.addAll(this.titleSentence.triggerList);
        for(SentenceInfo sentenceInfo : this.contentSentenceList){
            if(sentenceInfo.triggerList != null)triggerList.addAll(sentenceInfo.triggerList);
        }
        return triggerList;
    }

    public List<String> getPersonList() {

        //消除歧义
        Set<String> personSet = new HashSet<>();
        if(this.titleSentence.personList != null) personSet.addAll(this.titleSentence.personList);
        for(SentenceInfo sentenceInfo : this.contentSentenceList){
            if(sentenceInfo.personList != null)personSet.addAll(sentenceInfo.personList);
        }
        return new ArrayList<>(personSet);
    }

    public List<String> getLocationList() {
        Set<String> locationSet = new HashSet<>();
        if(this.titleSentence.locationList != null)locationSet.addAll(this.titleSentence.locationList);
        for(SentenceInfo sentenceInfo : this.contentSentenceList){
            if(sentenceInfo.locationList != null)locationSet.addAll(sentenceInfo.locationList);
        }

        return new ArrayList<>(locationSet);
    }

    public List<String> getOrganizationList() {
        Set<String> organizationSet = new HashSet<>();
        if(this.titleSentence.organizationList != null)organizationSet.addAll(this.titleSentence.organizationList);
        for(SentenceInfo sentenceInfo : this.contentSentenceList){
            if(sentenceInfo.organizationList != null)organizationSet.addAll(sentenceInfo.organizationList);
        }
        return new ArrayList<>(organizationSet);
    }



    @Override
    public String toString() {
        return "DataInfo{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", title='" + title + '\'' +
                ", time='" + time + '\'' +
                ", source='" + source + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataInfo dataInfo = (DataInfo) o;
        return Objects.equals(title, dataInfo.title);
    }

    @Override
    public int hashCode() {

        return Objects.hash(title);
    }
}
