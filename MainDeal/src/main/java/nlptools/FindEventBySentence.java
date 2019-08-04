package nlptools;

import main.GlobalVar;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import repository.DataInfo;
import repository.EventInfo;
import repository.SentenceInfo;
import repository.Trigger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述:
 *  先从文档中挑选候选句子集
 *  然后对句子排序, 根据句子前后关系构建触发词的前后关系
 *  将文档生成的触发词树合并到大的树(森林)中
 *  time 均为 yyyy-mm-dd的格式
 * @Author boy56
 * @Date 2018-11-01
 */
public class FindEventBySentence implements EventExtractApi {
    private HashMap<Trigger, TriggerNode> triggerNodeMap = new HashMap<>();   //触发词--叶子节点对应表
    private HashMap<Trigger, SentenceInfo> triggerSentenceMap = new HashMap<>(); //触发词--句子信息索引表

    //key值为“触发词->触发词”, 例如“发现->打捞”
    private HashMap<String, TriggerEdge> triggerEdgeMap = new HashMap<>();  //触发词-触发词--边索引表

    private NLPAPi nlpAPi;


    //IcO模式
    public FindEventBySentence(NLPAPi nlpAPi) {
        this.nlpAPi = nlpAPi;
    }

    @Override
    public List<EventInfo> eventExtract(List<DataInfo> dataInfoArrayList) {

        String outXmlPath = GlobalVar.getProp().getProperty("OutInfoDir")
                + GlobalVar.getProp().getProperty("EventName") + ".xml";

        dataProcess(dataInfoArrayList);

        for(DataInfo dataInfo : dataInfoArrayList){
            textDeal(dataInfo);
        }

        outPutXmlInfo(outXmlPath, dataInfoArrayList.size());

        return null;
    }

    /**
     * 对数据利用NLPAPi接口的功能实现分词、关键词提取、实体抽取、触发词抽取等功能
     *
     * @param dataInfoArrayList 数据列表
     */
    private void dataProcess(List<DataInfo> dataInfoArrayList) {
        String pattern = ".*\\d{1,2}月\\d{1,2}日.*";
        Set<Trigger> triggerSet = new HashSet<>();
        String middleInfoPath = GlobalVar.getProp().getProperty("MiddleInfoDir")
                + GlobalVar.getProp().getProperty("EventName") + "_SentenceInfo.txt";

        for (DataInfo dataInfo : dataInfoArrayList) {
            for (SentenceInfo sentenceInfo : dataInfo.contentSentenceList) {
                if (Pattern.matches(pattern, sentenceInfo.text)) { //筛选候选句的操作
                    nlpAPi.entityExtract(sentenceInfo);
                    nlpAPi.keywordsExtract(sentenceInfo);
                    nlpAPi.triggerExtract(sentenceInfo);
                }
            }
        }

        //输出中间信息
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(middleInfoPath), "UTF-8")) {
            for (DataInfo dataInfo : dataInfoArrayList) {
                for (SentenceInfo sentenceInfo : dataInfo.contentSentenceList) {
                    if (sentenceInfo.triggerList != null && sentenceInfo.triggerList.size() != 0) {
                        fw.write(sentenceInfo.text + "\n");
                        fw.write("Person:" + sentenceInfo.personList + "\n");
                        fw.write("Location:" + sentenceInfo.locationList + "\n");
                        fw.write("Organization:" + sentenceInfo.organizationList + "\n");
                        fw.write("Trigger:" + sentenceInfo.triggerList + "\n");
                        fw.write("Keywords:" + sentenceInfo.keywordsList + "\n");

                        triggerSet.addAll(sentenceInfo.triggerList);    //构建trigger字典
                    }
                }
                fw.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        //输出trigger字典
        String triggerInfoPath = GlobalVar.getProp().getProperty("MiddleInfoDir")
                + GlobalVar.getProp().getProperty("EventName") + "_triggers.txt";
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(triggerInfoPath), "UTF-8")) {
           for(Trigger t : triggerSet){
               String tmp = t.toString().replace("{","").replace("}","");
               fw.write(tmp + "\n");
           }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *  以文档为单位建立触发词之间的连接关系
     * @param dataInfo 文本级信息
     */
    private void textDeal(DataInfo dataInfo){
        Set<TriggerNode> triggerNodeSet = new HashSet<>();
        List<TriggerNode> triggerNodeList = new ArrayList<>();

        for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
            if(sentenceInfo.triggerList == null || sentenceInfo.triggerList.size() == 0) continue;        //筛选候选句的操作
            for(TriggerNode triggerNode : sentenceDeal(sentenceInfo, dataInfo.getTime())){
                if(!triggerNodeSet.contains(triggerNode)){
                    triggerNodeSet.add(triggerNode);
                    triggerNodeList.add(triggerNode);
                }
            }
        }


        //对触发词列表按照时间排序
        Collections.sort(triggerNodeList, new Comparator<TriggerNode>() {
            @Override
            public int compare(TriggerNode o1, TriggerNode o2) {
                return o1.time.compareTo(o2.time);
            }
        });

        //建立触发词之间的联系
        for(int i = 1; i < triggerNodeList.size(); i++){
            creatEdge(triggerNodeList.get(i-1), triggerNodeList.get(i));
        }

    }


    /**
     *  从句子中匹配到时间数据, 并与触发词做好对应(触发词所对应时间为最近的时间数据)
     * @param sentenceInfo 句子级信息
     * @param newsTime 句子所在新闻的发布日期
     * @return
     */
    private List<TriggerNode> sentenceDeal(SentenceInfo sentenceInfo, String newsTime){
        String text = sentenceInfo.text;
        int year = Integer.parseInt(newsTime.substring(0,4));//获取新闻发布的年份
        String pattern1 = "\\d{4}年\\d{1,2}月\\d{1,2}日";
        String pattern2 = "\\d{1,2}月\\d{1,2}日";
        HashMap<Integer, String> timeIndex = new HashMap<>(); //时间及其在句子中出现的位置
        List<Map.Entry<Integer,String>> timeIndexList = new ArrayList<>();

        List<TriggerNode> resultNodes = new ArrayList<>();

        // 创建 Pattern 对象
        Pattern r1 = Pattern.compile(pattern1);
        Pattern r2 = Pattern.compile(pattern2);

        // 匹配*年*月*日
        Matcher m1 = r1.matcher(text);
        while (m1.find()){
            text = text.replace(m1.group(),"--");//相同年月日多次出现则只会匹配出来一次
            String[] timeArray = m1.group().split("年|月|日");
            if(timeArray.length != 3){
                System.out.println("Error: FindEventBySentence-->dateExtract-->timeArray.length != 3");
            }
            if(timeArray[1].length() == 1) timeArray[1] = "0" + timeArray[1];
            if(timeArray[2].length() == 1) timeArray[2] = "0" + timeArray[2];
            String time = timeArray[0] + "-" + timeArray[1] + "-" + timeArray[2];
            timeIndex.put(m1.start(), time);
        }

        //匹配*月*日
        Matcher m2 = r2.matcher(text);
        while(m2.find()){
            String[] timeArray = m2.group().split("月|日");

            if(timeArray.length != 2){
                System.out.println("Error: FindEventBySentence-->dateExtract-->timeArray.length != 2");
            }
            if(timeArray[0].length() == 1) timeArray[0] = "0" + timeArray[0];
            if(timeArray[1].length() == 1) timeArray[1] = "0" + timeArray[1];
            String time = year + "-" + timeArray[0] + "-" + timeArray[1];
            timeIndex.put(m2.start(), time);
        }

        //对时间表根据出现位置排序
        timeIndexList.addAll(timeIndex.entrySet());
        TimeKeyComparator kc = new TimeKeyComparator();
        Collections.sort(timeIndexList,kc);

        //找到触发词对应的时间
        for(Trigger trigger:sentenceInfo.triggerList){

            //判断当前触发词信息是否在图中出现过
            TriggerNode tmpNode = isRepeatTrigger(trigger);
            if(tmpNode != null){
                resultNodes.add(tmpNode);
                continue;
            }

            String triggerTime; //触发词对应的时间
            int loc = sentenceInfo.text.indexOf(trigger.verbCore);
            if(loc < timeIndexList.get(0).getKey()) {
                triggerTime = timeIndexList.get(0).getValue();
            }else {
                int i = 0;
                for(; (i + 1 < timeIndexList.size()) && (timeIndexList.get(i + 1).getKey() > loc); i++);//找到就近的位于触发词左侧的时间
                triggerTime = timeIndexList.get(i).getValue();
            }

            //不是与事件发生的同一年发生的
            if((triggerTime.compareTo(year + "-01-01") < 0) ||
                    (triggerTime.compareTo(year + "-12-31") > 0)){
                continue;
            }

            TriggerNode nodeTmp = new TriggerNode(triggerTime, trigger);
            this.triggerNodeMap.put(trigger, nodeTmp);
            this.triggerSentenceMap.put(trigger,sentenceInfo);
            resultNodes.add(nodeTmp);
        }


        return resultNodes;
    }

    /**
     *  建立两个触发词节点之间的联系
     * @param startNode 开始触发词
     * @param endNode 结束触发词
     */
    private void creatEdge(TriggerNode startNode, TriggerNode endNode){
        String edgeKey = startNode.trigger + "->" + endNode.trigger;
        TriggerEdge triggerEdge = null;


        if(this.triggerEdgeMap.containsKey(edgeKey)){//两者之间已经建立过联系
            triggerEdge = this.triggerEdgeMap.get(edgeKey);
            triggerEdge.addWeight();    //更新边的权重
        }else{  //两者之间第一次建立联系
            //更新endNode的isRoot
            endNode.isRoot = false;

            //将endNode放入startNode的nextTriggerNodes中
            startNode.nextTriggerNodes.add(endNode);

            triggerEdge = new TriggerEdge(startNode.trigger, endNode.trigger);
            this.triggerEdgeMap.put(edgeKey, triggerEdge);

        }
    }


    private void outPutXmlInfo(String outPath, int textSize){
        Document doc = DocumentHelper.createDocument();
        //从符合过滤条件的边中添加触发词节点
        Set<TriggerNode> triggerNodeSet = new HashSet<>();
        Set<TriggerEdge> triggerEdgeSet = new HashSet<>();

        for(TriggerEdge triggerEdge : this.triggerEdgeMap.values()){
            if(isRetainEdges(triggerEdge, textSize)){   //过滤低频边
                triggerNodeSet.add(this.triggerNodeMap.get(triggerEdge.startTrigger));
                triggerNodeSet.add(this.triggerNodeMap.get(triggerEdge.endTrigger));
                triggerEdgeSet.add(triggerEdge);
            }
        }

        Element root = doc.addElement("gexf");
        Element graph = root.addElement("graph");

        //输出node信息
        Element nodesEle = graph.addElement("nodes");

        //增加触发词node
        for(TriggerNode triggerNode : triggerNodeSet){
            Element nodeEle = nodesEle.addElement("node");
            nodeEle.addAttribute("id", triggerNode.trigger.toString());
            nodeEle.addAttribute("labels", ":ENTITY:TRIGGER");

            Element dataEle_1 = nodeEle.addElement("data");
            dataEle_1.addAttribute("key", "time");
            dataEle_1.addText(triggerNode.time);

            Element dataEle_2 = nodeEle.addElement("data");
            dataEle_2.addAttribute("key", "name");
            dataEle_2.addText(triggerNode.trigger.toString());
        }

        //增加符合要求的事件node, 即选中触发词相对应的
        for(Map.Entry<Trigger, SentenceInfo> entry : this.triggerSentenceMap.entrySet()){
            if(!triggerNodeSet.contains(triggerNodeMap.get(entry.getKey()))) continue;
            Element nodeEle = nodesEle.addElement("node");
            nodeEle.addAttribute("id", entry.getKey()+"_e");
            nodeEle.addAttribute("labels",":EVENTNODE");

            Element dataEle_1 = nodeEle.addElement("data");
            dataEle_1.addAttribute("key", "trigger");
            dataEle_1.addText(entry.getValue().triggerList.toString());
            //dataEle_1.addText(entry.getKey());

            Element dataEle_2 = nodeEle.addElement("data");
            dataEle_2.addAttribute("key", "title");
            dataEle_2.addText(entry.getValue().text);

            Element dataEle_3 = nodeEle.addElement("data");
            dataEle_3.addAttribute("key", "time");
            dataEle_3.addText(this.triggerNodeMap.get(entry.getKey()).time);

            Element dataEle_4 = nodeEle.addElement("data");
            dataEle_4.addAttribute("key", "person");
            dataEle_4.addText(entry.getValue().personList.toString());

            Element dataEle_5 = nodeEle.addElement("data");
            dataEle_5.addAttribute("key", "location");
            dataEle_5.addText(entry.getValue().locationList.toString());

            Element dataEle_6 = nodeEle.addElement("data");
            dataEle_6.addAttribute("key", "organization");
            dataEle_6.addText(entry.getValue().organizationList.toString());

            Element dataEle_7 = nodeEle.addElement("data");
            dataEle_7.addAttribute("key", "keywords");
            dataEle_7.addText(entry.getValue().keywordsList.toString());
        }

        //输出edge信息
        Element edgesEle = graph.addElement("edges");
        int edgeId = 0;
        for(TriggerEdge triggerEdge : triggerEdgeSet){

            //增加触发词联系
            Element edgeEle = edgesEle.addElement("edge");
            edgeEle.addAttribute("id","e"+edgeId++);
            edgeEle.addAttribute("source", triggerEdge.startTrigger.toString());
            edgeEle.addAttribute("target", triggerEdge.endTrigger.toString());
            edgeEle.addAttribute("label", "NEXTEVENT weight:" + triggerEdge.weight);

            Element dataEle = edgeEle.addElement("data");
            dataEle.addAttribute("key", "weight");
            dataEle.addText(triggerEdge.weight+"");

        }

        //4、创建一个文件输出流
        FileOutputStream fos = null;
        try {
            OutputFormat formate=OutputFormat.createPrettyPrint();  //格式化输出
            //fos = new FileOutputStream("middleInfo/AllTriggers.xml");
            fos = new FileOutputStream(outPath);
            //装饰者模式  写XML文档的输出流
            XMLWriter writer = new XMLWriter(fos, formate);
            writer.write(doc);
            //writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     *  判断该触发词信息是否出现过
     * @param trigger 触发词对象
     * @return 已经存在类似的触发词信息, 返回该触发词, 否则返回null
     */
    private TriggerNode isRepeatTrigger(Trigger trigger){
        TriggerNode resultNode = null;
        double maxScore = 0;
        Trigger maxSimT = null;

        //已经存在v 与 n完全相同的信息
        if(this.triggerNodeMap.containsKey(trigger)){
            return(this.triggerNodeMap.get(trigger));
        }

        /**
         * v+n计算触发词信息相似度
         */
        for(Trigger trip : this.triggerNodeMap.keySet()){
            //计算verb的相似度

            //计算noun的相似度
        }


        return resultNode;
    }

    /**
     *  根据边的权重判断其是否被保留
     * @param triggerEdge 触发词边对象
     * @param textSize 处理的文档数量
     * @return 保留返回true, 过滤掉返回false
     */
    private boolean isRetainEdges(TriggerEdge triggerEdge, int textSize){
        //double filterPara = 0.01 * textSize;        //处理李文星事件参数
        double filterPara = 0.001 * textSize;
        if(triggerEdge.weight < filterPara){
            return false;
        }else{
            return true;
        }
    }

}


//触发词节点
class TriggerNode{
    String time;
    Trigger trigger;
    Boolean isRoot;  //作为creatEdge的endNode时被设置为false
    Boolean isWriten;   //已经被输出
    Set<TriggerNode> nextTriggerNodes;

    TriggerNode(String time, Trigger trigger) {
        this.time = time;
        this.trigger = trigger;
        this.isRoot = true;
        this.isWriten = false;
        this.nextTriggerNodes = new HashSet<>();
    }

    @Override
    public String toString() {
        return "TriggerNode{" +
                "time='" + time + '\'' +
                ", trigger='" + trigger + '\'' +
                ", isRoot=" + isRoot +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggerNode that = (TriggerNode) o;
        return Objects.equals(trigger, that.trigger);
    }

    @Override
    public int hashCode() {

        return Objects.hash(trigger);
    }

}

//触发词与触发词之间连接的边
class TriggerEdge{
    Trigger startTrigger;
    Trigger endTrigger;
    int weight;

    TriggerEdge(Trigger startTrigger, Trigger endTrigger) {
        this.startTrigger = startTrigger;
        this.endTrigger = endTrigger;
        this.weight = 1;
    }

    void addWeight(){
        this.weight++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggerEdge that = (TriggerEdge) o;
        return Objects.equals(startTrigger + "->" + endTrigger, that.startTrigger + "->" + that.endTrigger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTrigger + "->" + endTrigger);
    }
}

class TimeKeyComparator implements Comparator<Map.Entry<Integer, String>>
{
    public int compare(Map.Entry<Integer, String> mp1, Map.Entry<Integer, String> mp2)
    {
        return mp1.getKey() - mp2.getKey();
    }
}



