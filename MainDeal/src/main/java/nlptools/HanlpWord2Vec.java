package nlptools;

import com.hankcs.hanlp.corpus.io.IOUtil;
import com.hankcs.hanlp.mining.word2vec.DocVectorModel;
import com.hankcs.hanlp.mining.word2vec.Word2VecTrainer;
import com.hankcs.hanlp.mining.word2vec.WordVectorModel;

import java.io.*;
import java.util.*;

/**
 * 描述:
 *
 * @Author boy56
 * @Date 2018-11-01
 */
public class HanlpWord2Vec {
    private static final String TRAIN_FILE_NAME = "搜狗文本分类语料库已分词.txt";
    //private static final String MODEL_FILE_NAME = "model/hanlpmodel/own_vectors.txt";
    private static final String MODEL_FILE_NAME = "dict/words_vec.txt";

    public static void main(String[] args) throws IOException
    {

        WordVectorModel wordVectorModel = trainOrLoadModel();
        System.out.println(wordVectorModel.similarity("徐玉玉", "徐玉"));

        /*
        List<String> wordsList = new ArrayList<>(loadTriggerDict("dict/学生侵害-Triggers.txt"));
        double totalScore = 0;
        int count = 0;

        CilinSimilarity cilinSimilarityTool = new CilinSimilarity();

        //输出中间信息
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream("middleInfo/similarityScore-T.txt"), "UTF-8")) {
            for(int i = 0; i < wordsList.size()-1; i++){
                for(int j = i + 1; j < wordsList.size(); j++){
                    double vecScore = wordVectorModel.similarity(wordsList.get(i), wordsList.get(j));
                    double cilinScore = cilinSimilarityTool.getWordSimilarity(wordsList.get(i), wordsList.get(j));
                    fw.write(wordsList.get(i) + ", " + wordsList.get(j) + "     vecSore:" + vecScore + ", cilinSore:" + cilinScore + "\n");
                    //totalScore += vecScore;
                    //count++;
                }
            }
            //fw.write("平均值: " + totalScore/count + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println(cilinSimilarityTool.getWordSimilarity("死亡", "去世"));
        */
    }

    static void printNearest(String word, WordVectorModel model)
    {
        System.out.printf("\n                                                Word     Cosine\n------------------------------------------------------------------------\n");
        for (Map.Entry<String, Float> entry : model.nearest(word))
        {
            System.out.printf("%50s\t\t%f\n", entry.getKey(), entry.getValue());
        }
    }

    static void printNearestDocument(String document, String[] documents, DocVectorModel model)
    {
        printHeader(document);
        for (Map.Entry<Integer, Float> entry : model.nearest(document))
        {
            System.out.printf("%50s\t\t%f\n", documents[entry.getKey()], entry.getValue());
        }
    }

    private static void printHeader(String query)
    {
        System.out.printf("\n%50s          Cosine\n------------------------------------------------------------------------\n", query);
    }

    static WordVectorModel trainOrLoadModel() throws IOException
    {
        if (!IOUtil.isFileExisted(MODEL_FILE_NAME))
        {
            if (!IOUtil.isFileExisted(TRAIN_FILE_NAME))
            {
                System.err.println("语料不存在，请阅读文档了解语料获取与格式：https://github.com/hankcs/HanLP/wiki/word2vec");
                System.exit(1);
            }
            Word2VecTrainer trainerBuilder = new Word2VecTrainer();
            return trainerBuilder.train(TRAIN_FILE_NAME, MODEL_FILE_NAME);
        }

        return loadModel();
    }

    static WordVectorModel loadModel() throws IOException
    {
        return new WordVectorModel(MODEL_FILE_NAME);
    }

    private static HashSet<String> loadTriggerDict(String path){
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
}
