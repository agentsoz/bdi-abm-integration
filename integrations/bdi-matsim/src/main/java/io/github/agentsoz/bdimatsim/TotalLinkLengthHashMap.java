package io.github.agentsoz.bdimatsim;

import java.util.concurrent.ConcurrentHashMap;

public class TotalLinkLengthHashMap {

    private ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();

    public void addValue(Integer key, String value) {
        map.put(key, value);
    }

    public String getValue(Integer key) {
           return map.get(key);
       }

       public static void main(String[] args) {
          // Example example = new Example();
          TotalLinkLengthHashMap example = new TotalLinkLengthHashMap();

           // Simulate threads adding and accessing the map
           new Thread(() -> example.addValue(1, "value1")).start();
           new Thread(() -> {
               String value = example.getValue(1);
               if (value != null) {
                   System.out.println("Retrieved value: " + value);
               }
           }).start();
    }
}
