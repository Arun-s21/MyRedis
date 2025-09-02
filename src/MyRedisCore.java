import java.util.concurrent.ConcurrentHashMap;

//redis is an in memory data store which is checked before the api makes a request to the backend
//redis at its core uses (key,value) data structure 
//we require a fast lookup datastructure that can do GET,SET,DELETE operations thats why we use hashmaps because it 
//take O(1) TC for these operations

//we use concurrent hashmaps because in our project, multiple users will try to connect to our app at the same time
//normal hashmaps are not thread safe meaning race condition could arise 
//so we use a special concurrent hashmap(thread-safe)



public class MyRedisCore {
    private ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public String set(String key, String value) {
        store.put(key, value);
        return "OK";
    }

    public String get(String key) {
        return store.getOrDefault(key, "(nil)");
    }

    public String del(String key) {
        return store.remove(key) != null ? "1" : "0";
    }

    public static void main(String[] args) {
        MyRedisCore redis = new MyRedisCore();
        System.out.println(redis.set("name", "arun"));
        System.out.println(redis.get("name"));
        System.out.println(redis.del("name"));
        System.out.println(redis.get("name"));
    }
}
