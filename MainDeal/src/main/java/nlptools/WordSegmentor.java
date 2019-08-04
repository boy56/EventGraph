package nlptools;

import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.SegmentationAlgorithm;
import org.apdplat.word.segmentation.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *    Word分词
 * @Author boy56
 * @Date 2018-10-12
 */
public class WordSegmentor implements SegmentApi {

    @Override
    public List<String> wordSeg(String text) {
        List<String> result = new ArrayList<>();
        for(Word w : WordSegmenter.seg(text, SegmentationAlgorithm.MaxNgramScore)){
            result.add(w.getText());
        }
        return result;
    }
}
