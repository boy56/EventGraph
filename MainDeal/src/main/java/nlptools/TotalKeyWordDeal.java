package nlptools;

import repository.DataInfo;
import repository.SentenceInfo;
import repository.WordInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class TotalKeyWordDeal {

    private static HashMap<String, Float> wordTextrankScoreMap = new HashMap<>();  //根据textrank计算出来的每个单词分数, 用于替代TF-IDF算法中的TF
    private static HashMap<String, Integer> wordNewsCountMap = new HashMap<>();  //包含该单词的文档个数
    private static final double keywordNumPara = 0.4;   //最后的关键词个数占总单词个数的大小

    //textrank参数
    private static final float d = 0.84f;           // 阻尼系数
    private static final int max_iter = 200;        // 迭代次数
    private static final float min_diff = 0.0001f;  // 判断是否继续递归
    private static final int coOccuranceWindow = 5; // 共现窗口

    /**
     *  对所有文本进行关键词抽取的入口函数
     * @param dataInfoList 分好词后的文本集合
     * @param outFilePath 结果输出文件
     */
    public static void keywordDeal(List<DataInfo> dataInfoList, String outFilePath){
        HashMap<String, Double> resultWordsScore = new HashMap<>();

        List<String> titleWords = new ArrayList<>();
        List<String> contentWords = new ArrayList<>();
        int totalTextCount = dataInfoList.size();

        for(DataInfo dataInfo : dataInfoList) {
            for(WordInfo w : dataInfo.titleSentence.wordsList){
                if (Function.getInstance().filterWordByText(w.name)) continue;  //需要被过滤
                if(TotalKeyWordDeal.filterWordsByPos(w.posTag)) continue;
                titleWords.add(w.name);
            }

            for (SentenceInfo sentenceInfo : dataInfo.contentSentenceList) {
                for(WordInfo w : sentenceInfo.wordsList){
                    if (Function.getInstance().filterWordByText(w.name)) continue;  //需要被过滤
                    if(TotalKeyWordDeal.filterWordsByPos(w.posTag)) continue;
                    contentWords.add(w.name);
                }
            }

            TotalKeyWordDeal.getTextrankWordScore(titleWords, contentWords);//独立计算句子中的关键词
        }

        //根据TK-IDF(变形的TF-IDF算法)算法计算词的分数
        for(Map.Entry<String, Float> entry: wordTextrankScoreMap.entrySet()){
            resultWordsScore.put(entry.getKey(), entry.getValue() * Math.log(totalTextCount/wordNewsCountMap.get(entry.getKey())));
        }

        List<Map.Entry<String, Double>> wordsScoreList = new ArrayList<>(resultWordsScore.entrySet());

        //对最终结果进行排序
        Collections.sort(wordsScoreList, new Comparator<Map.Entry<String, Double>>() {//根据分数排序
                    @Override
                    public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                        return (o2.getValue().compareTo(o1.getValue()));
                    }
                }
        );

        //输出中间信息
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(outFilePath), "UTF-8")) {
            for(int i = 0; i < keywordNumPara * wordsScoreList.size(); i++){
                fw.write(wordsScoreList.get(i).getKey() + "\t" + wordsScoreList.get(i).getValue() + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *  根据textrank算法对每篇文章的单词进行打分
     * @param titleWordList 题目的单词集合
     * @param contentWordList 内容的单词集合
     * @return 返回<单词,分数></单词,分数>形式的Map表
     */
    private static Map<String, Float> getTextrankWordScore(List<String> titleWordList, List<String> contentWordList) {//计算每个词的分数
        ArrayList<String> sl = new ArrayList<String>(){};

        for(int i = 0;i < 3;i++) sl.addAll(titleWordList);
        sl.addAll(contentWordList);

        int count=1;
        Map<String,Integer> wordPosition = new HashMap<String,Integer>();

        List<String> wordList=new ArrayList<String>();

        for (String s : sl) {//计数
            wordList.add(s);
            if(!wordPosition.containsKey(s)) {
                wordPosition.put(s,count);
                count++;
            }
        }
        Map<String, Set<String>> wordsNeighborMap = new HashMap<String, Set<String>>();

        //que为滑动窗口, 实际通过推动wordList的移动实现
        Queue<String> que = new LinkedList<String>();
        for (String w : wordList) {//计算窗口
            if (!wordsNeighborMap.containsKey(w)) {
                wordsNeighborMap.put(w, new HashSet<String>());
            }
            que.offer(w);
            if (que.size() > coOccuranceWindow) {
                que.poll();
            }
            for (String w1 : que){
                for (String w2 : que) {
                    if (w1.equals(w2)) {
                        continue;
                    }
                    wordsNeighborMap.get(w1).add(w2);
                    wordsNeighborMap.get(w2).add(w1);
                }
            }
        }

        //根据公式迭代并更新权重, 直至收敛
        Map<String, Float> wordScoreMap = new HashMap<String, Float>();
        for (int i = 0; i < max_iter; ++i) {//计算分数
            Map<String, Float> m = new HashMap<String, Float>();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : wordsNeighborMap.entrySet()) {
                String key = entry.getKey();
                Set<String> value = entry.getValue();
                m.put(key, 1 - d);
                for (String other : value) {
                    int size = wordsNeighborMap.get(other).size();
                    if (key.equals(other) || size == 0) continue;
                    m.put(key, m.get(key) + d / size * (wordScoreMap.get(other) == null ? 0 : wordScoreMap.get(other)));      //PageRank迭代公式
                }
                max_diff = Math.max(max_diff, Math.abs(m.get(key) - (wordScoreMap.get(key) == null ? 1 : wordScoreMap.get(key))));
            }
            wordScoreMap = m;
            if (max_diff <= min_diff) break;
        }

        //将计算到的单个文档单词分数加入全局索引表中
        for(Map.Entry<String, Float> tmp: wordScoreMap.entrySet()){
            //更新wordTextrankScoreMap
            Float wordScore = tmp.getValue();
            if(wordTextrankScoreMap.containsKey(tmp.getKey())){
                wordScore += wordTextrankScoreMap.get(tmp.getKey());
            }
            wordTextrankScoreMap.put(tmp.getKey(), wordScore);

            //更新wordNewsCountMap, 在该篇文章中出现, 次数+1
            if(wordNewsCountMap.containsKey(tmp.getKey())){
                wordNewsCountMap.put(tmp.getKey(), wordNewsCountMap.get(tmp.getKey()) + 1);
            }else{
                wordNewsCountMap.put(tmp.getKey(), 1);
            }
        }
        
        return wordScoreMap;
}

    /**
     * 查询中文字符的个数
     * @param o
     * @return
     */
    private static int hznum(Object o){
        char[] c = null;
        c = o.toString().toCharArray();
        int count = 0;
        for(int i = 0;i < c.length;i++) if(Character.toString(c[i]).matches("[\\u4E00-\\u9FA5]+")) count++;//匹配中文
        return count;
    }

    /**
     *  从词性的角度进行过滤
     * @param s 单词词性标注结果, LTP标注集
     * @return 需要被过滤掉返回true, 否则返回false
     */
    private static Boolean filterWordsByPos(String s){
        if(s.equals("b") ||
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
}
