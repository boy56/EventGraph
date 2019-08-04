import edu.hit.ir.ltp4j.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *  Ltp功能测试
 * @Author boy56
 * @Date 2018-10-10
 */
public class LtpFoo {
    public static void main(String[] args) {
        LtpFoo ltpFoo = new LtpFoo();

        int size = 0;
        //String text = "国务院总理李克强调研上海外高桥时提出，支持上海积极探索新机制。";
        String text = "宝象金融涉嫌自融，项目造假，请停止犯罪！?各人看到没有，信达金控没有一个字的介绍，又是一个套路";
        //String text = "5月19日，收到聘用通知函";
        //String text = "今日下午，城关派出所一工作人员告诉记者，7月14日18点56分，有路人报警称，在G104国道旁的一水坑内，有一男性尸体浮在水面";
        //String text = "8月14日，距东北大学毕业生李文星的尸体在天津静海被发现已过去一个月的时间";
        //String text = "中国青年网记者了解到，8月6日凌晨，静海区组织开展打击传销“凌晨行动”，截至8月6日上午11时，共出动执法人员2000余人，排查村街社区418个，发现传销窝点301处，清理传销人员63名。";

        Segmentor segmentor = new Segmentor();
        if(segmentor.create("model/ltpmodel/cws.model")<0){
            System.err.println("load failed");
            return;
        }

        Postagger postagger = new Postagger();
        if(postagger.create("model/ltpmodel/pos.model")<0) {
            System.err.println("load failed");
            return;
        }

        NER ner = new NER();
        if(ner.create("model/ltpmodel/ner.model")<0) {
            System.err.println("load failed");
            return;
        }

        Parser parser = new Parser();
        if(parser.create("model/ltpmodel/parser.model") < 0){
            throw new RuntimeException("fail to load parser model");
        }

        SRL srl = new SRL();
        if(parser.create("model/ltpmodel/pisrl.model") < 0){
            throw new RuntimeException("fail to load srl model");
        }

        List<String> words = new ArrayList<String>();
        List<String> postags= new ArrayList<String>();
        List<String> ners = new ArrayList<String>();
        List<Integer> heads = new ArrayList<>();
        List<String> deprels = new ArrayList<>();
        List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<>();

        segmentor.segment(text, words);
        segmentor.release();

        postagger.postag(words,postags);
        postagger.release();

        size = parser.parse(words, postags, heads, deprels);

        for(int i=0; i<size; i++){
            if(heads.get(i) == 0){
                System.out.println("ROOT" + "-" + deprels.get(i) + "->" + words.get(i));
            }else{
                System.out.println(words.get(heads.get(i) - 1) + "-" + deprels.get(i) + "->" + words.get(i));
            }
        }

        System.out.println();

        System.out.println("words:" + words);
        System.out.println("postags:" + postags);
        System.out.println("heads:" + heads);
        System.out.println("size:" + size);
        parser.release();

        /*
        *  语义角色分析报错
        * */

        /*
        System.out.println("-");
        size = srl.srl(words, postags, ners, heads, deprels, srls);
        System.out.println("???");
        for(int i=0; i<size; i++){
            System.out.print(srls.get(i).first + ": ");
            for(int j = 0; j < srls.get(i).second.size(); j++) {
                System.out.print(srls.get(i).second.get(j).first + " ");
                System.out.print(srls.get(i).second.get(j).second.toString() + " | ");
            }
            System.out.println();
        }
        System.out.println();
        srl.release();
        */
    }

    private void segmentTest(){
        Segmentor segmentor = new Segmentor();
        if(segmentor.create("model/ltpmodel/cws.model")<0){
            System.err.println("load failed");
            return;
        }

        String sent = "魏则西及其家人因在百度推荐的武警北京市总队第二医院接受了未经审批且效果未经确认的治疗方法最终不治去世。";
        List<String> words = new ArrayList<String>();
        int size = segmentor.segment(sent,words);

        for(int i = 0; i<size; i++) {
            System.out.print(words.get(i));
            if(i==size-1) {
                System.out.println();
            } else{
                System.out.print("\t");
            }
        }
        segmentor.release();
    }

    private void postagTest(){
        Postagger postagger = new Postagger();
        if(postagger.create("model/ltpmodel/pos.model")<0) {
            System.err.println("load failed");
            return;
        }

        List<String> words= new ArrayList<String>();
        words.add("我");   words.add("是");
        words.add("中国"); words.add("人");
        List<String> postags= new ArrayList<String>();

        int size = postagger.postag(words,postags);
        for(int i = 0; i < size; i++) {
            System.out.print(words.get(i)+"_"+postags.get(i));
            if(i==size-1) {
                System.out.println();
            } else {
                System.out.print("|");
            }
        }
        postagger.release();
    }

    private void nerTest(){
        NER ner = new NER();
        if(ner.create("model/ltpmodel/ner.model")<0) {
            System.err.println("load failed");
            return;
        }
        List<String> words = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        List<String> ners = new ArrayList<String>();
        words.add("中国");tags.add("ns");
        words.add("国际");tags.add("n");
        words.add("广播");tags.add("n");
        words.add("电台");tags.add("n");
        words.add("创办");tags.add("v");
        words.add("于");tags.add("p");
        words.add("1941年");tags.add("m");
        words.add("12月");tags.add("m");
        words.add("3日");tags.add("m");
        words.add("。");tags.add("wp");

        ner.recognize(words, tags, ners);

        for (int i = 0; i < words.size(); i++) {
            System.out.println(ners.get(i));
        }

        ner.release();
    }

    private void parserTest(){
        Parser parser = new Parser();
        if(parser.create("model/ltpmodel/parser.model") < 0){
            throw new RuntimeException("fail to load parser model");
        }
        List<String> words = new ArrayList<>();
        List<String> postags = new ArrayList<>();
        words.add("一把手");    postags.add("n");
        words.add("亲自");      postags.add("d");
        words.add("过河");      postags.add("v");
        words.add("。");        postags.add("wp");

        List<Integer> heads = new ArrayList<>();
        List<String> deprels = new ArrayList<>();

        parser.parse(words, postags, heads, deprels);

        for(int i=0; i< heads.size(); i++){
            System.out.println(heads.get(i));
            System.out.println(deprels.get(i));
        }
        parser.release();
    }
}
