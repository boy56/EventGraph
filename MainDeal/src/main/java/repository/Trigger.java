package repository;

import java.util.Objects;

/**
 * 描述:
 *  事件的触发词元素
 * @Author boy56
 * @Date 2018-12-04
 */
public class Trigger {
    public String verbCore;    //触发词的核心动词
    public String nounCore;    //触发词的辅助信息

    public Trigger(String verbCore) {
        this.verbCore = verbCore;
        this.nounCore = null;
    }

    public Trigger(String verbCore, String nounCore) {
        this.verbCore = verbCore;
        this.nounCore = nounCore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return Objects.equals(verbCore, trigger.verbCore) &&
                Objects.equals(nounCore, trigger.nounCore);
    }

    @Override
    public int hashCode() {

        return Objects.hash(verbCore, nounCore);
    }

    @Override
    public String toString() {
        /*
        return "Trigger{" +
                "verbCore='" + verbCore + '\'' +
                ", nounCore='" + nounCore + '\'' +
                '}';
        */
        if(nounCore != null)return "{" + verbCore + "-" + nounCore + "}";
        else return "{" + verbCore + "}";
    }
}
