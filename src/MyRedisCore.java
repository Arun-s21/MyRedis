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



     // helper inner class
    private static class RedisValue {
        private String value;
        private final Long expiryTime; 

        public RedisValue(String value, Long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public void setValue(String newValue) {
            this.value = newValue;
        }


        public String getValue() {
            return value;
        }

         public Long getExpiryTime() {
            return expiryTime;
        }

        public boolean isExpired() {
            if (expiryTime == null) return false;
            return System.currentTimeMillis() > expiryTime;
        }
    }


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
    
    // TTL(this function returns -2 if the key doesnt exits, -1 if the key is epxired, and remaining time in seconds if key exists and havent expired)
    public String ttl(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null) return "-2"; // key doesn't exist
        if (redisValue.isExpired()) {
            store.remove(key);
            return "-2";
        }
        if (redisValue.getExpiryTime() == null) return "-1"; // no expiry
        long remainingMillis = redisValue.getExpiryTime() - System.currentTimeMillis();
        long remainingSeconds = remainingMillis / 1000;
        return String.valueOf(remainingSeconds >= 0 ? remainingSeconds : -2);
    }

    //adding counters for ratelimiting
    //here Redis.set("counter",10)--this sets the key counter with a value 10
    //redis.incr("counter") -- increases the value of this counter from 10 to 11
    //redis.decr("counter") -- decreases the value of this counter again from 11 to 10

    // INCR
    public String incr(String key) {
        RedisValue rv = store.get(key);
        if (rv == null || rv.isExpired()) {
            store.put(key, new RedisValue("1", null));
            return "1";
        }
        try {
            int intValue = Integer.parseInt(rv.getValue());
            intValue++;
            rv.setValue(String.valueOf(intValue));
            return rv.getValue();
        } catch (NumberFormatException e) {
            return "(error) value is not an integer";
        }
    }

    // DECR
    public String decr(String key) {
        RedisValue rv = store.get(key);
        if (rv == null || rv.isExpired()) {
            store.put(key, new RedisValue("-1", null));
            return "-1";
        }
        try {
            int intValue = Integer.parseInt(rv.getValue());
            intValue--;
            rv.setValue(String.valueOf(intValue));
            return rv.getValue();
        } catch (NumberFormatException e) {
            return "(error) value is not an integer";
        }
    }







   
    // Quick test
    public static void main(String[] args) throws InterruptedException {
        MyRedisCore redis = new MyRedisCore();

        // System.out.println(redis.set("name", "arun"));         // OK
        // System.out.println(redis.get("name"));                 // arun

        // System.out.println(redis.set("temp", "123", 2));       // OK (expires in 2s)
        // System.out.println(redis.get("temp"));                 // 123
        // Thread.sleep(3000);
        // System.out.println(redis.get("temp"));                 // (nil)

        // System.out.println(redis.del("name"));                 // 1
        // System.out.println(redis.get("name"));                 // (nil)

        // redis.set("name","arun",2);            //time to live 2 seconds
        // System.out.println(redis.exists("name"));               //returns 1

        //ttl test
        redis.set("name", "arun", 5); // expires in 5 seconds
        System.out.println(redis.ttl("name")); // ~5
        Thread.sleep(2000);
        System.out.println(redis.ttl("name")); // ~3
        Thread.sleep(6000);
        System.out.println(redis.ttl("name")); // -2 (expired)
        redis.set("permanent", "data");
        System.out.println(redis.ttl("permanent")); // -1 (no expiry)

         // Counter test
        redis.set("counter", "10");
        System.out.println(redis.incr("counter")); // 11
        System.out.println(redis.incr("counter")); // 12
        System.out.println(redis.decr("counter")); // 11
        System.out.println(redis.get("counter"));  // 11
        
    }
}
