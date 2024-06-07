package main.modelView;

import java.util.HashMap;

public class ModelView {
    private String url;
    private HashMap <String, Object> data = new HashMap<String, Object>();

    public ModelView() {
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    public void add(String key, Object newObject){
        data.put(key, newObject);
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public String getUrl() {
        return url;
    }
    
}
