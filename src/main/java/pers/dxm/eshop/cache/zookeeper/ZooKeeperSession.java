package pers.dxm.eshop.cache.zookeeper;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * zookeeper的包装类，用来处理获取和销毁分布式锁的逻辑
 */
public class ZooKeeperSession {
    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private ZooKeeper zookeeper;

    public ZooKeeperSession() {
        // 去连接zookeeper server，创建会话的时候，是异步去进行的
        // 所以要给一个监听器，告诉我们什么时候才是真正完成了跟zk server的连接
        try {
            this.zookeeper = new ZooKeeper(
                    "192.168.181.128:2181,192.168.181.129:2181,192.168.181.130:2181",
                    50000,
                    new ZooKeeperWatcher());
            // 给一个状态CONNECTING，连接中
            System.out.println(zookeeper.getState());
            try {
                // CountDownLatch（java多线程并发同步的一个工具类）
                // 用法：实例化CountDownLatch对象时会传递进去一些数字，比如说1,2,3都可以,
                // 然后await()，如果数字不是0，那么就卡住等待，其他的线程可以调用coutnDown()，将数字减1，
                // 如果数字减到0，那么之前所有在await的线程，都会逃出阻塞的状态，继续向下运行
                connectedSemaphore.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("ZooKeeper session established......");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取分布式锁（服务于缓存重建和数据源更改造成的缓存更新的并发冲突，根据productid获取锁）
    public void acquireDistributedLock(Long productId) {
        String path = "/product-lock-" + productId;
        try {
            zookeeper.create(path, "".getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            System.out.println("success to acquire lock for product[id=" + productId + "]");
        } catch (Exception e) {
            // 如果某个商品id对应的锁的node已经存在了（说明有人已经操作该商品的缓存了）
            // 这时另一个人再操作该商品的缓存，到这里再尝试获取分布式锁时就会报错NodeExistsException
            // 程序既然能进入到catch中，就说明已经捕获了NodeExistsException异常，即有不止一个人想在相同时间内操作一个商品的缓存
            // 所以说在死循环中间隔一段时间后不断地试图获取分布式锁，并打印尝试获取锁的次数，直到获取到锁为止
            int count = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    zookeeper.create(path, "".getBytes(),
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                } catch (Exception e2) {
                    count++;
                    System.out.println("the " + count + " times try to acquire lock for product[id=" + productId + "]......");
                    continue;
                }
                System.out.println("success to acquire lock for product[id=" + productId + "] after " + count + " times try......");
                break;
            }
        }
    }

    //获取分布式锁（服务于缓存预热的并发冲突，第一重锁，根据taskid获取锁）
    public boolean acquireFastFailedDistributedLock(String path) {
        try {
            zookeeper.create(path, "".getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            System.out.println("success to acquire lock for " + path);
            return true;
        } catch (Exception e) {
            System.out.println("fail to acquire lock for " + path);
        }
        return false;
    }

    //获取分布式锁（服务于缓存预热的并发冲突，第二重锁，根据预热状态获取锁）
    public void acquireDistributedLock(String path) {
        try {
            zookeeper.create(path, "".getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            System.out.println("success to acquire lock for " + path);
        } catch (Exception e) {
            int count = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    zookeeper.create(path, "".getBytes(),
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                } catch (Exception e2) {
                    count++;
                    System.out.println("the " + count + " times try to acquire lock for " + path + "......");
                    continue;
                }
                System.out.println("success to acquire lock for " + path + " after " + count + " times try......");
                break;
            }
        }
    }

    //释放掉一个分布式锁（服务于缓存重建和数据源更改造成的缓存更新的并发冲突）
    public void releaseDistributedLock(Long productId) {
        String path = "/product-lock-" + productId;
        try {
            zookeeper.delete(path, -1);
            System.out.println("release the lock for product[id=" + productId + "]......");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //释放掉一个分布式锁（服务于缓存预热的并发冲突）
    public void releaseDistributedLock(String path) {
        try {
            zookeeper.delete(path, -1);
            System.out.println("release the lock for " + path + "......");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNodeData(String path) {
        try {
            return new String(zookeeper.getData(path, false, new Stat()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void setNodeData(String path, String data) {
        try {
            zookeeper.setData(path, data.getBytes(), -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createNode(String path) {
        try {
            zookeeper.create(path, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {

        }
    }

    //A建立zk session的watcher
    private class ZooKeeperWatcher implements Watcher {

        public void process(WatchedEvent event) {
            System.out.println("Receive watched event: " + event.getState());
            if (KeeperState.SyncConnected == event.getState()) {
                connectedSemaphore.countDown();
            }
        }
    }

    //封装单例的静态内部类
    private static class Singleton {
        private static ZooKeeperSession instance;

        static {
            instance = new ZooKeeperSession();
        }

        public static ZooKeeperSession getInstance() {
            return instance;
        }

    }

    //获取单例
    public static ZooKeeperSession getInstance() {
        return Singleton.getInstance();
    }

    //初始化单例的便捷方法
    public static void init() {
        getInstance();
    }

}
