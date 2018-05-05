package pers.dxm.eshop.cache.kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

/**
 * kafka消费者，消费者消费到来自商品信息服务的请求后，交给具体的处理线程去处理
 * @author Administrator
 */
public class KafkaConsumer implements Runnable {

	private ConsumerConnector consumerConnector;
	private String topic;
	
	public KafkaConsumer(String topic) {
		this.consumerConnector = Consumer.createJavaConsumerConnector(
				createConsumerConfig());
		this.topic = topic;
	}
	
	@SuppressWarnings("rawtypes")
	public void run() {
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, 1);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap =
        		consumerConnector.createMessageStreams(topicCountMap);
        //获取到kafka生产者生产的消息，交给kafka处理线程去处理，调用相应的数据处理接口
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
        //每遍历得到一条kafka消息，就会生成一个KafkaMessageProcessor对象去处理这个请求
		//可以有N个KafkaMessageProcessor去处理请求，数量取决于kafka生产者产生的服务条数
        for (KafkaStream stream : streams) {
            new Thread(new KafkaMessageProcessor(stream)).start();
        }
	}
	
	/**
	 * 创建kafka cosumer config
	 * @return
	 */
	private static ConsumerConfig createConsumerConfig() {
        Properties props = new Properties();
        props.put("zookeeper.connect", "192.168.181.128:2181,192.168.181.129:2181,192.168.181.130:2181");
        props.put("group.id", "eshop-cache-group");
        props.put("zookeeper.session.timeout.ms", "40000");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        return new ConsumerConfig(props);
    }
}


