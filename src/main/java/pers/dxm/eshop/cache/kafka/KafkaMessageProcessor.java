package pers.dxm.eshop.cache.kafka;

import com.alibaba.fastjson.JSONObject;


import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import pers.dxm.eshop.cache.model.ProductInfo;
import pers.dxm.eshop.cache.model.ShopInfo;
import pers.dxm.eshop.cache.service.CacheService;
import pers.dxm.eshop.cache.spring.SpringContext;

/**
 * kafka消息处理线程，用于处理kafka消费者接收到的消息
 *
 * @author Administrator
 */
@SuppressWarnings("rawtypes")
public class KafkaMessageProcessor implements Runnable {

    private KafkaStream kafkaStream;
    private CacheService cacheService;

    public KafkaMessageProcessor(KafkaStream kafkaStream) {
        this.kafkaStream = kafkaStream;
        this.cacheService = (CacheService) SpringContext.getApplicationContext()
                .getBean("cacheService");
    }

    @SuppressWarnings("unchecked")
    public void run() {
        ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
        while (it.hasNext()) {
            String message = new String(it.next().message());
            // 首先将kafka生产者生产的message转换成json对象
            JSONObject messageJSONObject = JSONObject.parseObject(message);
            // 从这里提取出消息对应的服务的标识
            String serviceId = messageJSONObject.getString("serviceId");
            // 如果是商品信息服务，这里只模拟了两种类型的数据信息服务，在真实的环境中，
            // 只要在详情页中对实时性要求不高的数据，都可以分类放到这里处理
            if ("productInfoService".equals(serviceId)) {
                processProductInfoChangeMessage(messageJSONObject);
            } else if ("shopInfoService".equals(serviceId)) {
                processShopInfoChangeMessage(messageJSONObject);
            }
        }
    }

    /**
     * 处理商品信息变更的消息
     *
     * @param messageJSONObject
     */
    private void processProductInfoChangeMessage(JSONObject messageJSONObject) {
        // 提取出商品id
        Long productId = messageJSONObject.getLong("productId");
        // 调用商品信息服务的接口（商品信息服务是另一个服务，需要通过远程调用的方式获取接口）
        // 直接用注释模拟：getProductInfo?productId=1，传递过去
        // 商品信息服务，一般来说就会去查询数据库，去获取productId=1的商品信息，然后返回回来（假设返回的json如下所示）
        String productInfoJSON = "{\"id\": 1, \"name\": \"iphone7手机\", " +
                "\"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": " +
                "\"iphone7的规格\", \"service\": \"iphone7的售后服务\", " +
                "\"color\": \"红色,白色,黑色\", \"size\": \"5.5\", \"shopId\": 1}";
        ProductInfo productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
        cacheService.saveProductInfo2LocalCache(productInfo);//先存到本地缓存
        System.out.println("===================获取刚保存到本地缓存的商品信息：" + cacheService.getProductInfoFromLocalCache(productId));
        cacheService.saveProductInfo2ReidsCache(productInfo);//再存到redis缓存
    }

    /**
     * 处理店铺信息变更的消息
     *
     * @param messageJSONObject
     */
    private void processShopInfoChangeMessage(JSONObject messageJSONObject) {
        // 提取出商品id
        Long productId = messageJSONObject.getLong("productId");
        Long shopId = messageJSONObject.getLong("shopId");
        // 直接用注释模拟：getShopInfo?productId=1，传递过去
        // 模拟调用商品信息服务的接口，返回店铺消息（json格式如下）
        String shopInfoJSON = "{\"id\": 1, \"name\": \"小王的手机店\", \"level\": 5, \"goodCommentRate\":0.99}";
        ShopInfo shopInfo = JSONObject.parseObject(shopInfoJSON, ShopInfo.class);
        cacheService.saveShopInfo2LocalCache(shopInfo);//先存到本地缓存
        System.out.println("===================获取刚保存到本地缓存的店铺信息：" + cacheService.getShopInfoFromLocalCache(shopId));
        cacheService.saveShopInfo2ReidsCache(shopInfo);//再存到redis缓存
    }

}
