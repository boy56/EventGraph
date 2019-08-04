import main.BackMain;
import nlptools.LTPOpt;
import nlptools.NLPAPi;
import repository.DataInfo;
import repository.SentenceInfo;
import repository.WordInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *  HanLp组件功能测试
 * @Author boy56
 * @Date 2018-10-30
 */
public class HanLpFoo {
    public static void main(String[] args){
        String path = "data";
        File file = new File(path);
        File[] fileArray = file.listFiles();
        List<DataInfo> dataInfoList = new ArrayList<>();
        NLPAPi LTPApi = new LTPOpt();

        for(File f : fileArray){
            dataInfoList.addAll(BackMain.readFile(path + "/" + f.getName()));
        }

        //输出中间信息
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream("middleInfo/AllWords.txt"), "UTF-8")) {


            for (DataInfo dataInfo : dataInfoList) {
                System.out.println(dataInfo.getTitle());
                LTPApi.wordsSeg(dataInfo.titleSentence);
                for(WordInfo wordInfo: dataInfo.titleSentence.wordsList){
                    fw.write(wordInfo.name + " ");
                }
                fw.write("\n");

                for (SentenceInfo sentenceInfo : dataInfo.contentSentenceList) {
                   LTPApi.wordsSeg(sentenceInfo);
                   for(WordInfo wordInfo : sentenceInfo.wordsList){
                       fw.write(wordInfo.name + " ");
                   }
                   fw.write("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
