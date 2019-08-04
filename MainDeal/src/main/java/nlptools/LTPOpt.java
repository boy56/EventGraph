package nlptools;

import edu.hit.ir.ltp4j.NER;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.Postagger;
import main.GlobalVar;
import repository.*;
import sun.text.normalizer.TrieIterator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 描述:
 *    LTP的NLP工具实现
 * @Author boy56
 * @Date 2018-07-01
 */
public class LTPOpt implements NLPAPi {

    private SegmentApi segmentApi;    //分词器

    private Postagger postagger;    //词性标注
    private NER ner;    //命名实体识别
    private Parser parser; //依存句法分析
    private Set<String> triggerDict;    //触发词字典
    private Set<String> specialTriggerDict; //特殊类触发词字典, 通用, 其后需要加宾语


    public LTPOpt() {
        this.segmentApi = new LtpSegmentor();

        this.postagger = new Postagger();
        this.ner = new NER();
        this.parser = new Parser();

        //加载标注模型
        if(postagger.create(GlobalVar.getProp().getProperty("LTPPosModel"))<0) {
            System.err.println("Ltp postagger load failed");
        }

        if(ner.create(GlobalVar.getProp().getProperty("LTPNerModel"))<0) {
            System.err.println("Ltp ner model load failed");
        }

        if(parser.create(GlobalVar.getProp().getProperty("LTPParserModel")) < 0){
            throw new RuntimeException("Ltp parser model load failed");
        }

        //加载触发词字典
        this.triggerDict = loadTriggerDict(GlobalVar.getProp().getProperty("TriggerDictFile"));    //学生侵害类词典

    }

    @Override
    public void wordsSeg(DataInfo dataInfo) {
        //title分词
        sentenceSeg(dataInfo.titleSentence);

        //content分词
        for (SentenceInfo contentSentence : dataInfo.contentSentenceList) {

           sentenceSeg(contentSentence);
        }
    }

    @Override
    public void wordsSeg(SentenceInfo sentenceInfo) {
       sentenceSeg(sentenceInfo);
    }

    @Override
    public void keywordsExtract(DataInfo dataInfo) {
        List<String> keyWordList = new ArrayList<>();
        //LTPSeg + LTPPOS + textrank
        List<String> titleWords = new ArrayList<>();
        List<String> contentWords = new ArrayList<>();
        //获取题目内的候选词
        for(WordInfo w : dataInfo.titleSentence.wordsList){
            //从内容角度过滤
            if(Function.getInstance().filterWordByText(w.name)) continue;
            //从词性角度过滤
            if(filterWordsByPos(w.posTag)) continue;
            titleWords.add(w.name);
        }

        //获取内容中的候选词
        for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
            for(WordInfo w : sentenceInfo.wordsList){
                //从内容角度过滤
                if(Function.getInstance().filterWordByText(w.name)) continue;
                //从词性角度过滤
                if(filterWordsByPos(w.posTag)) continue;
                contentWords.add(w.name);
            }
        }

        /*
            更新TotalKeyWordDeal后尚未写完
         */
        dataInfo.setKeyWordsList(keyWordList);

    }

    @Override
    public void keywordsExtract(SentenceInfo sentenceInfo) {
        //放在entityExtract之后进行
        Set<String> entitySet = new HashSet<>();
        List<String> candidateList = new ArrayList<>();
        Set<String> candidateSet = new HashSet<>();
        sentenceInfo.keywordsList = new ArrayList<>();

        //根据词性筛选关键信息
        for(WordInfo wordInfo : sentenceInfo.wordsList){
            //根据单词内容筛选
            if(Function.getInstance().filterWordByText(wordInfo.name)) continue; //符合过滤要求，进行过滤，不予选用

            String s = wordInfo.posTag;
            //根据词性筛选
            if(s.equals("j")||
                    s.equals("n")||
                            s.equals("nh")||
                            s.equals("ni")||
                            s.equals("nl")||
                            s.equals("ns")||
                            s.equals("nz")||
                            s.equals("v")){
                if(!candidateSet.contains(wordInfo.name)) { //在保留顺序的前提下去重
                    candidateList.add(wordInfo.name);
                    candidateSet.add(wordInfo.name);
                }
            }
        }

        if(sentenceInfo.personList == null){
            System.out.println("Warning in LTPOpt->keywordsExtract: entityExtract should be done before this");
        }else{
            entitySet.addAll(sentenceInfo.personList);
            entitySet.addAll(sentenceInfo.locationList);
            entitySet.addAll(sentenceInfo.organizationList);
        }

        for(String w : candidateList){
            if(!entitySet.contains(w)){ //不是实体, 从而作为关键词存储
                sentenceInfo.keywordsList.add(w);
            }
        }


    }

    @Override
    public void entityExtract(DataInfo dataInfo) {
        //对题目进行实体抽取
        sentenceNer(dataInfo.titleSentence);
        //对文本内容进行实体抽取
        for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
            sentenceNer(sentenceInfo);
        }
    }

    @Override
    public void entityExtract(SentenceInfo sentenceInfo) {
        sentenceNer(sentenceInfo);
    }

    @Override
    public void triggerExtract(DataInfo dataInfo) {
        sentenceTrigger(dataInfo.titleSentence);
        for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
            sentenceTrigger(sentenceInfo);
        }
    }

    @Override
    public void triggerExtract(SentenceInfo sentenceInfo) {
        sentenceTrigger(sentenceInfo);
    }


    /**
     *  从词性的角度进行过滤
     * @param s 单词词性标注结果, LTP标注集
     * @return 需要被过滤掉返回true, 否则返回false
     */
    private Boolean filterWordsByPos(String s){
        if(//s.equals("b") ||
                s.equals("j")||
                s.equals("n")||
                s.equals("nh")||
                s.equals("ni")||
                s.equals("nl")||
                s.equals("ns")||
                s.equals("nz")||
                s.equals("v")){
            return false;
        }
        return true;
    }

    /**
     *  以句子为单位进行分词和词性标注处理
     * @param sentenceInfo 句子信息对象
     */
    private void sentenceSeg(SentenceInfo sentenceInfo){
        List<String> wordsListTmp;
        List<String> posListTmp;

        posListTmp = new ArrayList<>();
        wordsListTmp = segmentApi.wordSeg(sentenceInfo.text);
        postagger.postag(wordsListTmp, posListTmp);
        sentenceInfo.wordsList = wordInfoConvert(wordsListTmp, posListTmp);
    }

    /**
     *  以句子为单位进行实体抽取, 抽取后根据内容过滤一遍
     * @param sentenceInfo 句子信息对象
     */
    private void sentenceNer(SentenceInfo sentenceInfo){

        List<String> totalWords = new ArrayList<>();
        List<String> totalPos = new ArrayList<>();

        for(WordInfo w : sentenceInfo.wordsList){
            totalWords.add(w.name);
            totalPos.add(w.posTag);
        }

        Set<String> peronSet = new HashSet<>();
        Set<String> locationSet = new HashSet<>();
        Set<String> organizationSet = new HashSet<>();

        HashMap<EntityLabelEnum, Set<String>> nerMap = new HashMap<>();

        //初始化nerMap
        nerMap.put(EntityLabelEnum.PERSON, peronSet);
        nerMap.put(EntityLabelEnum.LOCATION, locationSet);
        nerMap.put(EntityLabelEnum.ORGANIZATION, organizationSet);

        List<String> tmpNers = new ArrayList<>();
        //System.out.println("in: " + totalWords);
        ner.recognize(totalWords, totalPos, tmpNers);   //若传入的数组为空则会出错
        //System.out.println("out");
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < tmpNers.size(); i++){
            if(!tmpNers.get(i).equals("O")){
                String[] t = tmpNers.get(i).split("-");
                if(t[0].equals("S")){
                    if(!Function.getInstance().filterWordByText(totalWords.get(i))){
                        nerMap.get(switchToEntityLabel(t[1])).add(totalWords.get(i));//不满足过滤条件证明可以留下该实体
                    }
                }else if(t[0].equals("B") || t[0].equals("I")){
                    stringBuilder.append(totalWords.get(i));
                }else if(t[0].equals("E")){
                    stringBuilder.append(totalWords.get(i));
                    if(!Function.getInstance().filterWordByText(stringBuilder.toString())) {
                        nerMap.get(switchToEntityLabel(t[1])).add(stringBuilder.toString());//不满足过滤条件证明可以留下该实体
                    }
                    stringBuilder = new StringBuilder();
                }
            }
        }
        //ner.release();
        sentenceInfo.personList = new ArrayList<>(peronSet);
        sentenceInfo.locationList = new ArrayList<>(locationSet);
        sentenceInfo.organizationList = new ArrayList<>(organizationSet);
    }

    /**
     *  以句子为单位进行触发词抽取, 抽取后根据内容过滤一遍
     * @param sentenceInfo 句子信息对象
     */
    private void sentenceTrigger(SentenceInfo sentenceInfo){
        List<Integer> heads = new ArrayList<>();
        List<String> deprels = new ArrayList<>();
        List<String> sentenceSeg = new ArrayList<>();
        List<String> sentencePos = new ArrayList<>();

        for(WordInfo w : sentenceInfo.wordsList) {
            sentenceSeg.add(w.name);
            sentencePos.add(w.posTag);
        }

        parser.parse(sentenceSeg, sentencePos, heads, deprels);
        HashMap<Integer, ParseNode> parseNodeHashMap = new HashMap<>();   //<id, 节点>索引表
        //建立节点关系(建树)过程
        //ROOT节点的id为0(与head中的编号为准)
        for(int i = 0; i < deprels.size(); i++){
            int childNodeId = i + 1;
            int headNodeId = heads.get(i);
            ParseNode childNode;
            ParseNode headNode;

            //构建子节点
            if(parseNodeHashMap.containsKey(childNodeId)){
                childNode = parseNodeHashMap.get(childNodeId);
            }else {
                childNode = new ParseNode(childNodeId, sentenceSeg.get(i), sentencePos.get(i));
                parseNodeHashMap.put(childNodeId,childNode);
            }

            //构建父节点
            if(parseNodeHashMap.containsKey(headNodeId)){
                headNode = parseNodeHashMap.get(headNodeId);
            }else{
                if(headNodeId == 0){    //ROOT
                    headNode = new ParseNode(headNodeId, "ROOT", "");
                }else {
                    headNode = new ParseNode(headNodeId, sentenceSeg.get(headNodeId - 1), sentencePos.get(headNodeId - 1));
                }
                parseNodeHashMap.put(headNodeId, headNode);
            }

            //从父节点建立指向子节点的关系
            headNode.childRelationMap.put(childNodeId, deprels.get(i));
        }


        //对于VOB、FOB关系加宾语共同作为触发词表示, 在此步骤中只有动词参与字典判断, 宾语不参加
        // LTP句法分析中head中索引减一才是真实索引, 因为0被"ROOT"占据
        // A-->B, 其中A是head
        /*
        //初步制作触发词然后筛选构造字典时用
        for(int i = 0; i < deprels.size(); i++) {
            if(deprels.get(i).equals("HED")){   //其head值为0, 表示ROOT
                if (!Function.getInstance().filterWordByText(sentenceSeg.get(i))) {
                    triTmp.add(sentenceSeg.get(i)); //不满足过滤条件证明可以留下该触发词
                }
            }else{
                String headWord = sentenceSeg.get(heads.get(i) - 1);
                String childWord = sentenceSeg.get(i);
                if(deprels.get(i).equals("SBV")){
                    if (!Function.getInstance().filterWordByText(headWord)) {
                        triTmp.add(headWord);//不满足过滤条件证明可以留下该触发词
                    }
                }else if(deprels.get(i).equals("VOB")){
                    if (!Function.getInstance().filterWordByText(headWord)) {
                        triTmp.add(headWord);//不满足过滤条件证明可以留下该触发词
                        //triInfoMap.put(headWord, childWord);
                    }
                }else if(deprels.get(i).equals("FOB")){
                    if (!Function.getInstance().filterWordByText(headWord)) {
                        triTmp.add(headWord);//不满足过滤条件证明可以留下该触发词
                        //triInfoMap.put(headWord, childWord);
                    }
                }
            }
        }*/


        Set<Integer> usedSet = new HashSet<>(); //在VOB, FOB判断中用, 防止 w1-VOB->w2-VOB->w3的连环情况
        HashMap<Integer, Trigger> triggerMap = new HashMap<>(); //一个节点可作为多个元素, 例如ROOT-HED->w1-VOB->w2

        List<Trigger> triggerList = new ArrayList<>();
        boolean isLoadTriggerDict = GlobalVar.getProp().getProperty("IsLoadTriggerDict").equals("true");

        for(int i = 0; i < deprels.size(); i++){
            int childId = i + 1;
            int headId = heads.get(i);
            Trigger trigger;

            if(deprels.get(i).equals("HED")){
                if(isLoadTriggerDict){
                    if(!triggerDict.contains(parseNodeHashMap.get(childId).word)) continue;
                }
                if(!triggerMap.containsKey(childId)
                        &&!Function.getInstance().filterWordByText(parseNodeHashMap.get(childId).word)){
                    trigger = new Trigger(parseNodeHashMap.get(childId).word);
                    triggerMap.put(childId, trigger);
                    triggerList.add(trigger);
                }

            }else if(deprels.get(i).equals("SBV")){
                if(isLoadTriggerDict){
                    if(!triggerDict.contains(parseNodeHashMap.get(headId).word)) continue;
                }

                if(!triggerMap.containsKey(headId)
                        &&!Function.getInstance().filterWordByText(parseNodeHashMap.get(headId).word)){
                    trigger = new Trigger(parseNodeHashMap.get(headId).word);
                    triggerMap.put(headId, trigger);
                    triggerList.add(trigger);
                }

            }else if(deprels.get(i).equals("VOB")
                    ||deprels.get(i).equals("FOB")){
                if(isLoadTriggerDict){
                    if(!triggerDict.contains(parseNodeHashMap.get(headId).word)) continue;
                }

                if(!usedSet.contains(headId)
                        && !Function.getInstance().filterWordByText(parseNodeHashMap.get(headId).word)){
                    ParseNode nounInfo = findTriggerNounCore(headId, parseNodeHashMap);
                    if(triggerMap.containsKey(headId)){
                        if(nounInfo != null) triggerMap.get(headId).nounCore = nounInfo.word;
                    }else{
                        if(nounInfo != null) trigger = new Trigger(parseNodeHashMap.get(headId).word, nounInfo.word);
                        else trigger = new Trigger(parseNodeHashMap.get(headId).word);
                        triggerMap.put(headId, trigger);
                        triggerList.add(trigger);
                    }
                    usedSet.add(headId);
                    if(nounInfo != null) usedSet.add(nounInfo.id);
                }
            }
        }

        sentenceInfo.triggerList = triggerList;
    }

    /**
     *  根据句法分析树寻找触发词的辅助信息
     * @param nodeId 触发词在句法分析树中的节点id
     * @param parseNodeHashMap ID-节点索引表
     * @return 返回触发词辅助信息, 找到合适的返回, 未找到返回null
     */
    private ParseNode findTriggerNounCore(int nodeId, HashMap<Integer, ParseNode> parseNodeHashMap){
        List<ParseNode> nodeList = new ArrayList<>(); //候选信息队列
        ParseNode parseNode = parseNodeHashMap.get(nodeId);
        ParseNode tmp;

        for(int i : parseNode.childRelationMap.keySet()){
            if(parseNode.childRelationMap.get(i).equals("VOB")
                    ||parseNode.childRelationMap.get(i).equals("FOB")){
                nodeList.add(parseNodeHashMap.get(i));
            }
        }

        int firstSize = nodeList.size();

        //查询第一层子节点是否有符合条件的
        for(int i = 0; i < firstSize; i++){
            tmp = nodeList.get(i);

            //判断当前信息是否满足筛选条件
            if (!Function.getInstance().filterWordByText(tmp.word)
                    && !filterWordsByPos(tmp.wordTag)){
                return tmp;    //满足条件则将其返回
            }else{
                for(int key : tmp.childRelationMap.keySet()){
                    if(tmp.childRelationMap.get(key).equals("ATT")){    //寻找宾语的定中关系
                        nodeList.add(parseNodeHashMap.get(key));
                    }
                }
            }
        }

        //查询子节点的下一层节点中是否有符合条件的
        for(int i = firstSize; i < nodeList.size(); i++){
            tmp = nodeList.get(i);
            //判断当前信息是否满足筛选条件
            if (!Function.getInstance().filterWordByText(tmp.word)
                    && !filterWordsByPos(tmp.wordTag)){
                return tmp;    //满足条件则将其返回
            }
        }

        return null;    //没有符合条件的辅助信息
    }

    /**
     *  识别LTP实体标记
     * @param s LTP实体标记字符串
     * @return EntityLabelEnum 类型
     */
    private EntityLabelEnum switchToEntityLabel(String s){
        if(s.equals("Nh")){
            return EntityLabelEnum.PERSON;
        }else if(s.equals("Ni")){
            return EntityLabelEnum.ORGANIZATION;
        }else if(s.equals("Ns")){
            return EntityLabelEnum.LOCATION;
        }else{
            System.out.println("Error: LTPOpt->switchToEntityLabel s cann't be recognized: " + s);
            return EntityLabelEnum.LOCATION;
        }
    }

    /**
     *  加载触发词字典
     * @param path 触发词字典存储路径
     * @return 返回触发词字典
     */
    private HashSet<String> loadTriggerDict(String path){
        HashSet<String> triggerDict = new HashSet<>();
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(path), "UTF-8")) {//打开data
            BufferedReader br = new BufferedReader(isr);
            try {
                String line;
                while((line = br.readLine()) != null) {
                    triggerDict.add(line);
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return triggerDict;
    }



    /**
     *  将分词的结果和词性标注的结果转换成WordInfo形式
     * @param wordsName 分词结果列表
     * @param posList 词性标注结果列表
     * @return WordInfo格式的列表
     */
    private List<WordInfo> wordInfoConvert(List<String> wordsName, List<String> posList){
        List<WordInfo> wordInfoList = new ArrayList<>();
        for(int i = 0; i < wordsName.size(); i++){
            WordInfo wordInfo = new WordInfo(wordsName.get(i), posList.get(i));
            wordInfoList.add(wordInfo);
        }
        return wordInfoList;
    }

}



//句法分析树的节点
class ParseNode{
    int id;     //句法分析中在单词数组中的下标
    String word;
    String wordTag;

    Map<Integer, String> childRelationMap; //<id, 关系类型>

    public ParseNode(int id, String word, String wordTag) {
        this.id = id;
        this.word = word;
        this.wordTag = wordTag;

        this.childRelationMap = new HashMap<>();
    }
}
