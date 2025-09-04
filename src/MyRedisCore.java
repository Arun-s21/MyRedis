import java.util.concurrent.ConcurrentHashMap;

//redis is an in memory data store which is checked before the api makes a request to the backend IT IS USED FOR RATE LIMITING
//redis at its core uses (key,value) data structure 
//we require a fast lookup datastructure that can do GET,SET,DELETE operations thats why we use hashmaps because it 
//take O(1) TC for these operations

//we use concurrent hashmaps because in our project, multiple users will try to connect to our app at the same time
//normal hashmaps are not thread safe meaning race condition could arise 
//so we use a special concurrent hashmap(thread-safe)


public class MyRedisCore {
    // store now maps key -> RedisValue (value + expiry)
    private ConcurrentHashMap<String, RedisValue> store = new ConcurrentHashMap<>();

    // SET without expiry
    public String set(String key, String value) {
        store.put(key, new RedisValue(value, null));
        return "OK";
    }

    // SET with expiry (ttl in seconds)
    public String set(String key, String value, long ttlSeconds) {
        long expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        store.put(key, new RedisValue(value, expiryTime));
        return "OK";
    }

    // GET
    public String get(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) {
            return "(nil)";
        }

        if (redisValue.isExpired()) {
            store.remove(key);
            return "(nil)";
        }

        return redisValue.getValue();
    }

    // DEL
    public String del(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired()) {
            store.remove(key);
            return "0";
        }
        store.remove(key);
        return "1";
    }

    // EXISTS
    public String exists(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) return "0";
        if (redisValue.isExpired()) {
            store.remove(key);
            return "0";
        }
        return "1";
    }


    // helper inner class
    private static class RedisValue {
        private final String value;
        private final Long expiryTime;

        public RedisValue(String value, Long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            if (expiryTime == null) return false;
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // Quick test
    public static void main(String[] args) throws InterruptedException {
        MyRedisCore redis = new MyRedisCore();

        System.out.println(redis.set("name", "arun"));         // OK
        System.out.println(redis.get("name"));                 // arun

        System.out.println(redis.set("temp", "123", 2));       // OK (expires in 2s)
        System.out.println(redis.get("temp"));                 // 123
        Thread.sleep(3000);
        System.out.println(redis.get("temp"));                 // (nil)

        System.out.println(redis.del("name"));                 // 1
        System.out.println(redis.get("name"));                 // (nil)

        redis.set("name","arun",2);            //time to live 2 seconds
        System.out.println(redis.exists("name"));               //returns 1
        
    }
}
