package repository;

import org.neo4j.graphdb.Label;

/**
 * 描述:
 *   实体标签，主要为地点、人物、关键词，其中在关键词下还有附属的子标签
 * @Author boy56
 * @Date 2018-06-09
 */
public enum EntityLabelEnum implements Label {
    LOCATION,
    PERSON,
    ORGANIZATION,
    TRIGGER,
    KEYWORDS;

    public static EntityLabelEnum convertToLabel(String labelString){
        labelString = labelString.toUpperCase();
        /**
         * 待完善
         */

        return null;
    }
}
