package pers.dxm.eshop.cache.service;


import pers.dxm.eshop.cache.model.ProductInfo;
import pers.dxm.eshop.cache.model.ShopInfo;

/**
 * 缓存service接口
 *
 * @author Administrator
 */
public interface CacheService {
    ProductInfo saveProductInfo2LocalCache(ProductInfo productInfo);//将商品信息保存到本地的ehcache缓存中
    ShopInfo saveShopInfo2LocalCache(ShopInfo shopInfo);//将店铺信息保存到本地的ehcache缓存中
    ProductInfo getProductInfoFromLocalCache(Long productId);//从本地ehcache缓存中获取商品信息
    ShopInfo getShopInfoFromLocalCache(Long shopId);//从本地ehcache缓存中获取店铺信息
    void saveProductInfo2ReidsCache(ProductInfo productInfo);//将商品信息保存到redis中
    void saveShopInfo2ReidsCache(ShopInfo shopInfo);//将店铺信息保存到redis中
    ProductInfo getProductInfoFromReidsCache(Long productId);//从redis中获取商品信息
    ShopInfo getShopInfoFromReidsCache(Long shopId);//从redis中获取店铺信息
}
