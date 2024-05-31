package io.github.agentsoz.bdimatsim;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public enum TotalLinkLengthSingleton {

        INSTANCE("Initial class info");

        private String info;

        private TotalLinkLengthSingleton(String info) {
            this.info = info;

        }

        public TotalLinkLengthSingleton getInstance() {
            return INSTANCE;
        }

        private final ConcurrentHashMap<String, Double> map = new ConcurrentHashMap<>();

        public void putValue(String key, Double value) {
           if( map.containsKey(key)){
              double temp = map.get(key) + value;
              map.put(key, temp);
           } else{
                map.put(key, value);
            }
         }

          public Double getValue(String key) {
            double value = 0.0;
              if( map.containsKey(key)){
                value = map.get(key);
                  map.remove(key);
              }
            return value;
         }

         public ConcurrentHashMap<String, Double> getMap() {
          return map;
         }

}
