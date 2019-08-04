package nlptools;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.hankcs.hanlp.model.crf.CRFNERecognizer;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.hankcs.hanlp.tokenizer.NotionalTokenizer;
import repository.DataInfo;
import repository.SentenceInfo;
import repository.WordInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *  HanLp预处理函数
 * @Author boy56
 * @Date 2018-10-30
 */
public class HanLpDeal {
    public void hanLpPre(DataInfo dataInfo){

        String title = dataInfo.getTitle();
        String content = dataInfo.getContent();
        int id = 0;
        List<SentenceInfo> contentSentenceInfo = new ArrayList<>();
        String[] contentSentences = content.split("。|!|;|:");

        dataInfo.titleSentence = convertSentceInfo(""+id, title);
        for(String sentence:contentSentences){
            id++;
            contentSentenceInfo.add(convertSentceInfo(""+id, sentence));
        }
        dataInfo.contentSentenceList = contentSentenceInfo;
    }

    private SentenceInfo convertSentceInfo(String id, String text){
        SentenceInfo sentenceInfo = null;
        //CRF分词与实体识别
        try {
            CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
            List<Term> titleTermList;
            List<List<Term>> contentTermList;

            sentenceInfo = new SentenceInfo("" + id, text);

            List<WordInfo> wordsList = new ArrayList<>();

            List<String> personListTmp = new ArrayList<>();
            List<String> locationListTmp = new ArrayList<>();
            List<String> organizationListTmp = new ArrayList<>();

            analyzer.enableNameRecognize(true);
            analyzer.enablePlaceRecognize(true);
            analyzer.enableOrganizationRecognize(true);
            titleTermList = analyzer.seg(text);
            CoreStopWordDictionary.apply(titleTermList); //去除停用词

            //命名实体识别结果HMM方式
            for(Term t : titleTermList){
                if(t.length() > 1){ //去除单个字的分词结果
                    wordsList.add(new WordInfo(t.word, t.nature.toString()));

                    if(t.nature.toString().equals("nr")){   //人名
                        personListTmp.add(t.word);
                    }else if(t.nature.toString().equals("ns")){ //地名
                        locationListTmp.add(t.word);
                    }else if(t.nature.toString().equals("nt")){//机构名
                        organizationListTmp.add(t.word);
                    }
                }
            }

            sentenceInfo.keywordsList = HanLP.extractKeyword(text, 3);
            sentenceInfo.personList = personListTmp;
            sentenceInfo.locationList = locationListTmp;
            sentenceInfo.organizationList = organizationListTmp;
            sentenceInfo.wordsList = wordsList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sentenceInfo;

    }
}
