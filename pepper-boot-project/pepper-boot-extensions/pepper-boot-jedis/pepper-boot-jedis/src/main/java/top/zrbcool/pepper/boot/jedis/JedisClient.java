package top.zrbcool.pepper.boot.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;

/**
 * @author zhangrongbincool@163.com
 * @version 19-10-24
 */
public class JedisClient extends Jedis {
    private static final Long RELEASE_SUCCESS = 1L;
    private final JedisPool jedisPool;
    private final String namespace;
//    private PjedisPool jedisPool;
    private final JedisPoolConfig jedisPoolConfig;
    private final String address;
    private final int port;

    JedisPool getJedisPool() {
        return jedisPool;
    }

    public JedisClient(String namespace, JedisPoolConfig jedisPoolConfig, String address, int port) {
        this.namespace = namespace;
        this.jedisPoolConfig = jedisPoolConfig;
        this.address = address;
        this.port = port;
//        JedisPropsHolder.NAMESPACE.set(namespace);
        this.jedisPool = new JedisPool(jedisPoolConfig, address, port);
    }

    public boolean tryLock(String lockKey, String requestId, int expireTime) {
        try (Jedis jedis = jedisPool.getResource()){
            String result = jedis.set(lockKey, requestId, "NX", "PX", expireTime);
            if ("OK".equals(result)) {
                return true;
            }
        }
        return false;
    }

    public  boolean releaseDistributedLock(String lockKey, String requestId) {
        try (Jedis jedis = jedisPool.getResource()){
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }
        }
        return false;
    }

    public void doExecute(JedisInternalScripts scripts) {
        try (Jedis jedis = jedisPool.getResource()){
            scripts.script(jedis);
        }
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getNamespace() {
        return namespace;
    }
}