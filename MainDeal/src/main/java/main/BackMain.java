package main;

import com.hankcs.hanlp.corpus.io.IOUtil;
import nlptools.*;
import repository.DataInfo;
import repository.SentenceInfo;
import repository.WordInfo;

import java.io.*;
import java.util.*;

/**
 * 描述:
 *     后台运行处理主函数
 * @Author boy56
 * @Date 2018-06-13
 */
public class BackMain {
    //private static final String dataFilePath = "data/foo_test.txt";
    //private static final String dataFilePath = "data/data_Webnews_李文星.txt";
    //private static final String dataFilePath = "data/徐玉玉.txt";
    //private static final String dataFilePath = GlobalVar.getProp().getProperty("DataSrcDir") + GlobalVar.getProp().getProperty("EventName")+".txt";
    //private static final String globalKeyWordsPath = "dict/";

    private static final String localGraphDBPath = "F:/Neo4j/neo4j-community-3.3.5/data/databases/graph.db";
    private static final String baseUrl = "http://shuyantech.com/api/cndbpedia/";

    public static void main(String[] args){
        /**
         * 记得更改路径
         */
        NLPAPi LTPAPi = new LTPOpt();
        EventExtractApi eventExtractApi = new FindEventBySentence(LTPAPi);
        List<DataInfo> dataInfoList;
        String dataFilePath = GlobalVar.getProp().getProperty("DataSrcDir") + GlobalVar.getProp().getProperty("EventName")+".txt";

        //读取数据
        dataInfoList = readFile(dataFilePath);

        //数据分词与全局关键词表建立
        infoPreProcess(dataInfoList, LTPAPi);

        //用于LSTM进行序列标注实验
        //creatSrcDataForLSTM(dataInfoList, LTPAPi);

        //事件抽取
        eventExtractApi.eventExtract(dataInfoList);

        /*
        //HanLp 预处理测试
        HanLpDeal hanLpDeal = new HanLpDeal();

        for(DataInfo fooDataInfo:dataInfoList) {
            hanLpDeal.hanLpPre(fooDataInfo);

            System.out.println("words:" + fooDataInfo.titleSentence.wordsList);
            System.out.println("keywords:" + fooDataInfo.titleSentence.keywordsList);
            System.out.println("persons:" + fooDataInfo.titleSentence.personList);
            System.out.println("organizations:" + fooDataInfo.titleSentence.organizationList);
            System.out.println("loacations:" + fooDataInfo.titleSentence.locationList);
        }
        */


        System.out.println("add over");

    }

    /**
     * 读取文件并返回按照时间先后排序的新闻列表
     * @param path
     * @return
     */
    public static ArrayList<DataInfo> readFile(String path){
        ArrayList<DataInfo> dataInfoArrayList = new ArrayList<>();  //pre中的新闻列表
        HashSet<String> titleSet = new HashSet<>(); //去重的时候使用
        int dataInfoId = 0;
        int testCount = 0;

        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(path), "UTF-8")) {//打开data
            BufferedReader br = new BufferedReader(isr);
            try {
                String line;
                while((line = br.readLine()) != null) {
                    //得到新闻的基本信息
                    String[] str = line.split("\t");//切分
                    if(str.length < 3) continue;
                    /**
                     * 验证Date格式
                     */
                    DataInfo tmpInfo = new DataInfo(str[0],str[1],str[2]);  //date, title, content
                    testCount++;
                    if(titleSet.contains(str[1])){
                        continue;
                    }else{
                        int sentenceId = 0;
                        tmpInfo.setId(dataInfoId+"");

                        //设置titleSentence
                        tmpInfo.titleSentence = new SentenceInfo(""+sentenceId, tmpInfo.getTitle());

                        //设置contentSentenceList
                        String[] sentenceArray =  tmpInfo.getContent().split("。|!|;|？|；| |!|\\?"); //分句处理
                        tmpInfo.contentSentenceList = new ArrayList<>();
                        for (String s : sentenceArray) {
                            s = s.replace(" ","");
                            s = s.replace("&nbsp", "");
                            if(s.length() > 5) {
                                sentenceId++;
                                SentenceInfo contentSentence = new SentenceInfo(tmpInfo.getId() + "_" + sentenceId, s);
                                tmpInfo.contentSentenceList.add(contentSentence);
                            }
                        }
                        dataInfoArrayList.add(tmpInfo);
                        titleSet.add(str[1]);
                        dataInfoId++;
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



        //测试去重效果
        System.out.println("去重前文本个数: " + testCount);
        System.out.println("去重后文本个数: " + dataInfoArrayList.size());
        return dataInfoArrayList;
    }

    /**
     *  对文本进行分词预处理并提取关键词
     */
    public static void infoPreProcess(List<DataInfo> dataInfoList, NLPAPi nlpAPi){
        String keyWordPath = "dict/" + GlobalVar.getProp().getProperty("EventName") + "_keywords.txt";
        String wordSegPath = "cache/" + GlobalVar.getProp().getProperty("EventName") + "_words.txt";

        System.out.println("before wordSeg--");

        //分词
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(wordSegPath), "UTF-8")) {
            for(DataInfo dataInfo: dataInfoList){
                nlpAPi.wordsSeg(dataInfo);

                for(WordInfo w : dataInfo.titleSentence.wordsList){
                    fw.write(w.name + "|" + w.posTag + " ");
                }
                fw.write("\n");
                for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
                    for(WordInfo w : sentenceInfo.wordsList){
                        fw.write(w.name + "|" + w.posTag + " ");
                    }
                    fw.write("\n");
                }
                fw.write("---------\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        //提取全局关键词
        //TotalKeyWordDeal.keywordDeal(dataInfoList, keyWordPath);

        /* 利用分词文件作为中间缓存
        if (!IOUtil.isFileExisted(wordSegPath)) {
            //输出分词信息
            try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(wordSegPath), "UTF-8")) {
                for(DataInfo dataInfo: dataInfoList){
                    nlpAPi.wordsSeg(dataInfo);

                    for(WordInfo w : dataInfo.titleSentence.wordsList){
                        fw.write(w.name + "|" + w.posTag + " ");
                    }
                    fw.write("\n");
                    for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
                        for(WordInfo w : sentenceInfo.wordsList){
                            fw.write(w.name + "|" + w.posTag + " ");
                        }
                        fw.write("\n");
                    }
                    fw.write("---------\n");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(wordSegPath), "UTF-8")) {//打开data
                BufferedReader br = new BufferedReader(isr);
                try {
                    String line;

                    for(DataInfo dataInfo : dataInfoList){
                        //加载题目分词结果
                        line = br.readLine();
                        System.out.println(line);
                        String[] str = line.split(" ");
                        List<WordInfo> wordInfos = new ArrayList<>();
                        for(String s : str){
                            System.out.println(s);
                            String[] t = s.split("\\|");
                            wordInfos.add(new WordInfo(t[0], t[1]));
                        }
                        dataInfo.titleSentence.wordsList = wordInfos;
                        //加载正文分词结果
                        for(SentenceInfo sentenceInfo : dataInfo.contentSentenceList){
                           line = br.readLine();
                           str = line.split(" ");
                           wordInfos = new ArrayList<>();
                           for(String s : str){
                               String[] t = s.split("\\|");
                               wordInfos.add(new WordInfo(t[0], t[1]));
                           }
                           sentenceInfo.wordsList = wordInfos;
                        }
                        br.readLine();
                    }

                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        */


        System.out.println("after wordSeg--");

        /* 利用关键词文件作为中间缓存
        if (!IOUtil.isFileExisted(keyWordPath)) {
            TotalKeyWordDeal.keywordDeal(dataInfoList, keyWordPath);
        }else{
            System.out.println("测试用: keywords表已经提前处理好");
        }
        */
    }

    /**
     *  人工制作触发词字典时用, 输出候选词典再人工筛选
     */
    public static void outCandidateTrigger(List<DataInfo> dataInfoList, NLPAPi nlpAPi){

    }

    public static void creatSrcDataForLSTM(List<DataInfo> dataInfoArrayList, NLPAPi nlpAPi){
        String middleInfoPath = GlobalVar.getProp().getProperty("MiddleInfoDir")
                + GlobalVar.getProp().getProperty("EventName") + "_srcData.txt";

        //输出到middleInfo文件夹中
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(middleInfoPath), "UTF-8")) {
            for (DataInfo dataInfo : dataInfoArrayList) {
                for (SentenceInfo sentenceInfo : dataInfo.contentSentenceList) {
                    nlpAPi.triggerExtract(sentenceInfo);
                    if (sentenceInfo.triggerList != null && sentenceInfo.triggerList.size() != 0) {
                        fw.write(sentenceInfo.text + "\t");
                        fw.write(sentenceInfo.triggerList + "\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
