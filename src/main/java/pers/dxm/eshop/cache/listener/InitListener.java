package pers.dxm.eshop.cache.listener;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import pers.dxm.eshop.cache.kafka.KafkaConsumer;
import pers.dxm.eshop.cache.spring.SpringContext;

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
    }

    public void contextDestroyed(ServletContextEvent sce) {

    }
}
