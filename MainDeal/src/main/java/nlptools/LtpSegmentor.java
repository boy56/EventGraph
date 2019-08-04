package nlptools;

import edu.hit.ir.ltp4j.Segmentor;
import main.GlobalVar;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *     LTP分词器
 * @Author boy56
 * @Date 2018-10-16
 */

public class LtpSegmentor implements SegmentApi{

    private String modelPath;

    public LtpSegmentor() {
    }

    public LtpSegmentor(String modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    public List<String> wordSeg(String text) {

        Segmentor segmentor = new Segmentor();
        List<String> words = new ArrayList<String>();
        if(segmentor.create(GlobalVar.getProp().getProperty("LTPCwsModel"))<0){
            System.err.println("LtpSegmentor load model failed");
            return null;
        }

        segmentor.segment(text, words);
        segmentor.release();
        return words;
    }
}
