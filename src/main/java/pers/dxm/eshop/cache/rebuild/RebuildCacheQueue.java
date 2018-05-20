package pers.dxm.eshop.cache.rebuild;

import pers.dxm.eshop.cache.model.ProductInfo;

import java.util.concurrent.ArrayBlockingQueue;


/**
 * 重建缓存的内存队列
 **/
public class RebuildCacheQueue {
    //构建一个可以容纳1000个数据的阻塞队列用来存放待修改缓存的排队数据
    private ArrayBlockingQueue<ProductInfo> queue = new ArrayBlockingQueue<ProductInfo>(1000);

    //将商品信息放到队列中
    public void putProductInfo(ProductInfo productInfo) {
        try {
            queue.put(productInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //将商品信息从队列中删除
    public ProductInfo takeProductInfo() {
        try {
            return queue.take();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //内部单例类，获取RebuildCacheQueue对象
    private static class Singleton {
        private static RebuildCacheQueue instance;

        static {
            instance = new RebuildCacheQueue();
        }

        public static RebuildCacheQueue getInstance() {
            return instance;
        }
    }

    public static RebuildCacheQueue getInstance() {
        return Singleton.getInstance();
    }

    public static void init() {
        getInstance();
    }
}
