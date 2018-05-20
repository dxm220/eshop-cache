package pers.dxm.eshop.cache.controller;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import pers.dxm.eshop.cache.model.ProductInfo;
import pers.dxm.eshop.cache.model.ShopInfo;
import pers.dxm.eshop.cache.prewarm.CachePrewarmThread;
import pers.dxm.eshop.cache.rebuild.RebuildCacheQueue;
import pers.dxm.eshop.cache.service.CacheService;


/**
 * 缓存Controller
 *
 * @author Administrator
 */
@Controller
public class CacheController {

    @Resource
    private CacheService cacheService;

    @RequestMapping("/testPutCache")
    @ResponseBody
    public String testPutCache(ProductInfo productInfo) {
        cacheService.saveProductInfo2LocalCache(productInfo);
        return "success";
    }

    @RequestMapping("/testGetCache")
    @ResponseBody
    public ProductInfo testGetCache(Long id) {
        return cacheService.getProductInfoFromLocalCache(id);
    }

    @RequestMapping("/getProductInfo")
    @ResponseBody
    public ProductInfo getProductInfo(Long productId) {
        ProductInfo productInfo = null;
        //先尝试从redis缓存中获取数据信息
        productInfo = cacheService.getProductInfoFromReidsCache(productId);
        if (productInfo != null) {
            System.out.println("=================从redis中获取缓存，商品信息=" + productInfo);
        }
        //如果从redis获取的数据为空，表示redis没有该缓存，就尝试从ehcache中获取缓存信息
        if (productInfo == null) {
            productInfo = cacheService.getProductInfoFromLocalCache(productId);
            if (productInfo != null) {
                System.out.println("=================从ehcache中获取缓存，商品信息=" + productInfo);
            }
        }
        //如果从ehcache中也获取不到缓存，此时三级缓存中都没有缓存数据，需要从数据源服务重新拉取数据过来重建缓存
        if (productInfo == null) {
            // 假设下面json是从数据源服务拉取到的商品消息
            String productInfoJSON = "{\"id\": " + productId + ", \"name\": \"iphone7手机\", \"price\": 5599, " +
                    "\"pictureList\":\"a.jpg,b.jpg\", \"specification\": \"iphone7的规格\", " +
                    "\"service\": \"iphone7的售后服务\", \"color\": \"红色,白色,黑色\", \"size\": \"5.5\", " +
                    "\"shopId\": 1, \"modifiedTime\": \"2017-01-01 12:01:00\"}";
            productInfo = JSONObject.parseObject(productInfoJSON, ProductInfo.class);
            // 将数据推送到一个内存队列中
            RebuildCacheQueue rebuildCacheQueue = RebuildCacheQueue.getInstance();
            rebuildCacheQueue.putProductInfo(productInfo);
        }
        return productInfo;
    }

    @RequestMapping("/getShopInfo")
    @ResponseBody
    public ShopInfo getShopInfo(Long shopId) {
        //获取店铺信息的过程与获取商品信息的过程类似，看上面方法的注释
        ShopInfo shopInfo = null;
        shopInfo = cacheService.getShopInfoFromReidsCache(shopId);
        System.out.println("=================从redis中获取缓存，店铺信息=" + shopInfo);
        if (shopInfo == null) {
            shopInfo = cacheService.getShopInfoFromLocalCache(shopId);
            System.out.println("=================从ehcache中获取缓存，店铺信息=" + shopInfo);
        }
        if (shopInfo == null) {

        }
        return shopInfo;
    }

    @RequestMapping("/prewarmCache")
    @ResponseBody
    //缓存预热请求
    public void prewarmCache() {
        new CachePrewarmThread().start();
    }
}
