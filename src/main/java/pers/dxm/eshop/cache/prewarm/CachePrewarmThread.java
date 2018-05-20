package pers.dxm.eshop.cache.prewarm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import pers.dxm.eshop.cache.model.ProductInfo;
import pers.dxm.eshop.cache.service.CacheService;
import pers.dxm.eshop.cache.spring.SpringContext;
import pers.dxm.eshop.cache.zookeeper.ZooKeeperSession;

/**
 * Created by douxm on 2018\5\11 0011.
 */
public class CachePrewarmThread extends Thread {
    //双重锁确保一个taskid对应的
    @Override
    public void run() {
        CacheService cacheService = (CacheService) SpringContext.
                getApplicationContext().getBean("cacheService");
        ZooKeeperSession zkSession = ZooKeeperSession.getInstance();
        // 获取storm taskid列表
        String taskidList = zkSession.getNodeData("/taskid-list");
        System.out.println("【CachePrwarmThread获取到taskid列表】taskidList=" + taskidList);
        if (taskidList != null && !"".equals(taskidList)) {
            String[] taskidListSplited = taskidList.split(",");
            for (String taskid : taskidListSplited) {
                String taskidLockPath = "/taskid-lock-" + taskid;
                //先获取第一重分布式锁，第一重锁以taskid为依据获取，确保同一时间只有一个缓存实例操作某一个task实例
                boolean result = zkSession.acquireFastFailedDistributedLock(taskidLockPath);
                if (!result) {
                    continue;
                }
                String taskidStatusLockPath = "/taskid-status-lock-" + taskid;
                //获取第二重分布式锁，第一重锁以预热状态为依据获取，查看预热状态，
                //如果预热状态不为空，说明该商品已经被预热过，直接不做任何操作释放锁
                //如果预热状态为空，将商品放到各级缓存中，完成缓存预热
                //确保某个商品的预热操作只做一次，不会被多个缓存实例重复操作。
                zkSession.acquireDistributedLock(taskidStatusLockPath);
                String taskidStatus = zkSession.getNodeData("/taskid-status-" + taskid);
                System.out.println("【CachePrewarmThread获取task的预热状态】taskid=" + taskid + ", status=" + taskidStatus);
                if ("".equals(taskidStatus)) {
                    String productidList = zkSession.getNodeData("/task-hot-product-list-" + taskid);
                    System.out.println("【CachePrewarmThread获取到task的热门商品列表】productidList=" + productidList);
                    JSONArray productidJSONArray = JSONArray.parseArray(productidList);
                    for (int i = 0; i < productidJSONArray.size(); i++) {
                        Long productId = productidJSONArray.getLong(i);
                        //模拟从数据源获取到的商品数据
                        String productInfoJSON = "{\"id\": " + productId + ", \"name\": \"iphone7手机\", " +
                                "\"price\": 5599, \"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", " +
                                "\"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", " +
                                "\"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:00:00\"}";
                        ProductInfo productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
                        cacheService.saveProductInfo2LocalCache(productInfo);
                        System.out.println("【CachePrwarmThread将商品数据设置到本地缓存中】productInfo=" + productInfo);
                        cacheService.saveProductInfo2ReidsCache(productInfo);
                        System.out.println("【CachePrwarmThread将商品数据设置到redis缓存中】productInfo=" + productInfo);
                    }
                    //预热结束后，将task内容放到对应的znode中
                    zkSession.createNode("/taskid-status-" + taskid);
                    zkSession.setNodeData("/taskid-status-" + taskid, "success");
                }
                zkSession.releaseDistributedLock(taskidStatusLockPath);
                zkSession.releaseDistributedLock(taskidLockPath);
            }
        }
    }
}
