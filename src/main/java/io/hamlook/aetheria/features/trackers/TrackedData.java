package io.hamlook.aetheria.features.trackers;

import lombok.AllArgsConstructor;

import java.util.HashMap;

@AllArgsConstructor
public class TrackedData {

    public void add(String key, String value, Object baseValue) {
        Data data1 = new Data(key,value,baseValue);
        data.put(key,data1);
        keyOrder.put(keyOrder.size(),key);
    }

    public boolean has(String key){
        return data.containsKey(key) && keyOrder.containsValue(key);
    }

    public boolean update(String key, Object newVal) {
        Data data1 = data.get(key);
        if(data1 == null) return false;
        data1.value = newVal;
        return true;
    }

    public boolean isEmpty() {
        return data.isEmpty() || keyOrder.isEmpty();
    }

    @AllArgsConstructor
    public static class Data {
        public String key;
        public String line;
        public Object value;
    }
    public HashMap<String,Data> data;
    public HashMap<Integer,String> keyOrder;
}
