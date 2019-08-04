package main;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * 描述:
 *  对工程全局变量的封装
 * @Author boy56
 * @Date 2018-12-11
 */
public class GlobalVar {
    private static Properties prop;  //单例模式, 惰性加载

    public static Properties getProp(){
        if (prop == null) {
            synchronized (GlobalVar.class) {
               prop = new Properties();
            }
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                prop.load(new InputStreamReader(loader.getResourceAsStream("main.properties"), "UTF-8"));
            } catch (IOException e) {
                System.out.println("main.properties load error");
                e.printStackTrace();
            }
        }
        return prop;
    }

}
