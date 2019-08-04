package nlptools;

import repository.DataInfo;
import repository.EventInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *  事件抽取接口
 * @Author boy56
 * @Date 2018-10-31
 */
public interface EventExtractApi {
    /**
     *  对数据列表进行事件抽取
     * @param dataInfoArrayList 数据列表
     * @return 事件信息列表
     */
    List<EventInfo> eventExtract(List<DataInfo> dataInfoArrayList);


    /**
     * default修饰符定义默认方法
     */
    default void defaultMethod() {
        System.out.println("接口中的默认方法");
    }

}
