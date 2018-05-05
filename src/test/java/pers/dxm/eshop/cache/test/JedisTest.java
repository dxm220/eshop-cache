package pers.dxm.eshop.cache.test;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

public class JedisTest {
	
	public static void main(String[] args) throws Exception {
		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
        jedisClusterNodes.add(new HostAndPort("192.168.181.129", 7003));
        jedisClusterNodes.add(new HostAndPort("192.168.181.129", 7004));
        jedisClusterNodes.add(new HostAndPort("192.168.181.130", 7005));
        JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes);
        System.out.println(jedisCluster.get("product_info_1"));   
        System.out.println(jedisCluster.get("shop_info_1"));  
	}

}
