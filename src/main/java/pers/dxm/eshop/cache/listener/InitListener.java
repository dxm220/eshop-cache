package pers.dxm.eshop.cache.listener;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import pers.dxm.eshop.cache.kafka.KafkaConsumer;
import pers.dxm.eshop.cache.rebuild.RebuildCacheThread;
import pers.dxm.eshop.cache.spring.SpringContext;
import pers.dxm.eshop.cache.zookeeper.ZooKeeperSession;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by douxm on 2018\3\23 0023.
 */
public class InitListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(sc);
        SpringContext.setApplicationContext(context);
        //初始化一个kafka消费者线程，去监听kafka生产者发送过来的消息
        new Thread(new KafkaConsumer("cache-message")).start();
        //初始化一个缓存重建线程，开启后死循环不断监听缓存重建队列中的请求
        new Thread(new RebuildCacheThread()).start();
        ZooKeeperSession.init();
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
