package com.test.xn.kmslib;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Created by Administrator on 2018/10/10.
 */

public class RoomResponse {
    private int id;
    private String sessionId=null;
    private Method method;
    private String sdpAnswer= null;
    private HashMap<String ,Boolean> users=null;
    private List<HashMap<String,String>> values=null;

    public String toString(){
        return "RoomResponse: "+id+" - "+sessionId+" - "+valuesToString();
    }

    public List<HashMap<String,String>> getValues(){
        return values;
    }
    public List<String> getValue(String key) {
        List<String> result = new Vector<>();
        for (HashMap<String, String> aMap : values) {
            result.add(aMap.get(key));
        }
        return result;
    }

    public Method getMethod() {
        return method;
    }

    public int getId() {
        return id;
    }


    public String getSessionId() {
        return sessionId;
    }

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public RoomResponse(String id, JSONObject jsonObject){
        this.id=Integer.parseInt(id);
        this.sessionId=getSessionIdFromJsonObject(jsonObject);
        this.values=getValuesFromJsonObject(jsonObject);
    }
    /**
     * 从jsonObject中获取sessionId
     * @param jsonObject
     * @return
     */
    private String getSessionIdFromJsonObject(JSONObject jsonObject) {
        if (jsonObject.containsKey("sessionId")){
            return jsonObject.get("sessionId").toString();
        }
        return null;
    }
    /**
     * 从jsonObject中获取values
     * @param jsonObject
     * @return
     */
    private List<HashMap<String, String>> getValuesFromJsonObject(JSONObject jsonObject) {
        List<HashMap<String,String>> result = new Vector<>();
        if (jsonObject.containsKey("value")){
            JSONArray jsonArray= (JSONArray) jsonObject.get("value");
            method=Method.JOIN_ROOM;
            users=new HashMap<>();
            for (int i = 0; i <jsonArray.size() ; i++) {//遍历jsonArray，将jsonArray中的数据存入hashMap中
                HashMap<String, String> arrElement = new HashMap<>();
                JSONObject object= (JSONObject) jsonArray.get(i);//每一个jsonObject中含有一个id就是用户名，stream就是是否传递视频流
                Set<String> keys = object.keySet();//得到所有键的集合
                for (String key:keys){
                    arrElement.put(key,object.get(key).toString());
                }
                result.add(arrElement);

                if (object.containsKey("id")){
                    String userName = object.get("id").toString();
                    boolean webCamPublished = object.containsKey("streams");
                    users.put(userName,webCamPublished);
                }
            }
        }
        if (jsonObject.containsKey("sdpAnswer")){
            sdpAnswer = (String) jsonObject.get("sdpAnswer");
            HashMap<String, String> vArrayElement = new HashMap<>();
            vArrayElement.put("sdpAnswer",sdpAnswer);
            result.add(vArrayElement);
        }
        if (result.isEmpty()){
            result=null;
        }
        return result;
    }

    public String valuesToString(){
        StringBuilder sb = new StringBuilder();
        if (this.values!=null){
            for (HashMap<String,String> aValueMap:values){
                sb.append("{");
                for (Map.Entry<String,String> entry:aValueMap.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    sb.append(key).append("=").append(value).append(", ");
                }
                sb.append("},");
            }
        }
        return sb.toString();
    }

}
