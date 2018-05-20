package pers.dxm.eshop.cache.rebuild;

import pers.dxm.eshop.cache.model.ProductInfo;
import pers.dxm.eshop.cache.service.CacheService;
import pers.dxm.eshop.cache.spring.SpringContext;
import pers.dxm.eshop.cache.zookeeper.ZooKeeperSession;

import java.text.SimpleDateFormat;
import java.util.Date;



//缓存重建线程
public class RebuildCacheThread implements Runnable {
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public void run() {
		RebuildCacheQueue rebuildCacheQueue = RebuildCacheQueue.getInstance();
		ZooKeeperSession zkSession = ZooKeeperSession.getInstance();
		CacheService cacheService = (CacheService) SpringContext.getApplicationContext()
				.getBean("cacheService");
		//死循环不断的从缓存重建队列中获取数据，根据是否获取到zk锁的情况决定是否操作缓存
		//如果获取到zk锁后先从redis拿到现有的缓存，如果缓存不为null，然后比较要更新的缓存和已有的缓存时间版本
		//待更新的新于已有的就更新，反之不更新。如果从redis得不到缓存，直接将现有的数据更新到缓存中
		while(true) {
			ProductInfo productInfo = rebuildCacheQueue.takeProductInfo();
			zkSession.acquireDistributedLock(productInfo.getId());
			ProductInfo existedProductInfo = cacheService.getProductInfoFromReidsCache(productInfo.getId());
			if(existedProductInfo != null) {
				// 比较当前数据的时间版本比已有数据的时间版本是新还是旧
				try {
					Date date = sdf.parse(productInfo.getModifiedTime());
					Date existedDate = sdf.parse(existedProductInfo.getModifiedTime());
					if(date.before(existedDate)) {
						System.out.println("current date[" + productInfo.getModifiedTime() + "] is before existed date[" +
								existedProductInfo.getModifiedTime() + "]");
						continue;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("current date[" + productInfo.getModifiedTime() + "] is after existed date[" +
						existedProductInfo.getModifiedTime() + "]");
			} else {
				System.out.println("existed product info is null......");   
			}
			cacheService.saveProductInfo2LocalCache(productInfo);
			cacheService.saveProductInfo2ReidsCache(productInfo);  
			zkSession.releaseDistributedLock(productInfo.getId());
		}
	}
}
