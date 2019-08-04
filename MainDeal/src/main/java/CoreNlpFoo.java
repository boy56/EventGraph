
import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.Word;
import org.apdplat.word.tagging.PartOfSpeechTagging;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;


import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;


/**
 * 描述:
 * 测试使用
 *
 * @Author boy56
 * @Date 2018-06-08
 */
public class CoreNlpFoo {
    public static void main(String[] args) {

        List<Word> words = WordSegmenter.seg("魏则西及其家人因在百度推荐的武警北京市总队第二医院接受了未经审批且效果未经确认的治疗方法最终不治去世。");

        System.out.println(words);
        StringBuilder wordsString = new StringBuilder();
        for(Word w : words){
            wordsString.append(w.getText()).append(" ");
        }
        /*
        //词性标注

        PartOfSpeechTagging.process(words);
        for(int i = 0; i < words.size(); i++){
            System.out.println(words.get(i).getText());
            System.out.println(words.get(i).getPartOfSpeech().getPos());
            System.out.println("------");
        }
        */


        //System.out.println("标注词性："+words);
        /*
        ArrayList<Integer> foo = new ArrayList<>();
        foo.add(1);
        foo.add(2);
        foo.remove(0);
        System.out.println(foo);
        */


        /*
        foo f = new foo();
        try {
            f.test();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        String str = "我 去 吃饭 ， 告诉 李强 一声 。";
        ExtractDemo extractDemo = new ExtractDemo();
        System.out.println(extractDemo.doNer(wordsString.toString()));
        System.out.println("Complete!");


    }

    public void test() throws Exception {
        //构造一个StanfordCoreNLP对象，配置NLP的功能，如lemma是词干化，ner是命名实体识别等
        Properties props = new Properties();
        props.load(this.getClass().getResourceAsStream("/StanfordCoreNLP-chinese.properties"));
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        String text = "袁隆平是中国科学院的院士,他于2009年10月到中国山东省东营市东营区永乐机场附近承包了一千亩盐碱地,"
                + "开始种植棉花, 年产量达到一万吨, 哈哈, 反正棣琦说的是假的,逗你玩儿,明天下午2点来我家吃饭吧。"
                + "棣琦是山东大学毕业的,目前在百度做java开发,位置是东北旺东路102号院,手机号14366778890";

        long startTime = System.currentTimeMillis();
        // 创造一个空的Annotation对象
        Annotation document = new Annotation(text);

        // 对文本进行分析
        pipeline.annotate(document);

        //获取文本处理结果
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                //                // 获取句子的token（可以是作为分词后的词语）
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                System.out.println(word);
                //词性标注
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                System.out.println(pos);
                // 命名实体识别
                String ne = token.get(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class);
                String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                System.out.println(word + " | analysis : {  original : " + ner + "," + " normalized : "
                        + ne + "}");
                //词干化处理
                String lema = token.get(CoreAnnotations.LemmaAnnotation.class);
                System.out.println(lema);
            }

            // 句子的解析树
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            System.out.println("句子的解析树:");
            tree.pennPrint();

            // 句子的依赖图
            SemanticGraph graph =
                    sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            System.out.println("句子的依赖图");
            System.out.println(graph.toString(SemanticGraph.OutputFormat.LIST));

        }

        long endTime = System.currentTimeMillis();
        long time = endTime - startTime;
        System.out.println("The analysis lasts " + time + " seconds * 1000");

        // 指代词链
        //每条链保存指代的集合
        // 句子和偏移量都从1开始
        Map<Integer, CorefChain> corefChains = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        if (corefChains == null) {
            return;
        }
        for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
            System.out.println("Chain " + entry.getKey() + " ");
            for (CorefChain.CorefMention m : entry.getValue().getMentionsInTextualOrder()) {
                // We need to subtract one since the indices count from 1 but the Lists start from 0
                List<CoreLabel> tokens = sentences.get(m.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);
                // We subtract two for end: one for 0-based indexing, and one because we want last token of mention
                // not one following.
                System.out.println(
                        "  " + m + ", i.e., 0-based character offsets [" + tokens.get(m.startIndex - 1).beginPosition()
                                +
                                ", " + tokens.get(m.endIndex - 2).endPosition() + ")");
            }
        }
    }
}

class ExtractDemo {
    private static AbstractSequenceClassifier<CoreLabel> ner;

    public ExtractDemo() {
        InitNer();
    }

    public void InitNer() {
        String serializedClassifier = "edu/stanford/nlp/models/ner/chinese.misc.distsim.crf.ser.gz"; // chinese.misc.distsim.crf.ser.gz
        if (ner == null) {
            ner = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
        }
    }

    public String doNer(String sent) {
        return ner.classifyWithInlineXML(sent);
    }
}

