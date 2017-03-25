package org.cocos2dx.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public final class PSJNIHelper {
    static HashMap<String, String> mHashMap = null;
    static Vector<String> mVector = null;
    static ArrayList<String> mArrayList = null;
    
    public static void createHashMap(){
        mHashMap = new HashMap<>();
    }
    
    public static HashMap<String, String> getHashMap(){
        return mHashMap;
    }
    
    public static void pushHashMapElement(String key, String value){
        if(mHashMap == null)
            return;
        
        mHashMap.put(key, value);
    }
    
    public static void createVector(){
        mVector = new Vector<>();
    }
    
    public static Vector<String> getVector(){
        return mVector;
    }
    
    public static void pushVectorElement(String value){
        if(mVector == null)
            return;
        
        mVector.add(value);
    }
    
    public static void createArrayList(){
        mArrayList = new ArrayList<>();
    }
    
    public static ArrayList<String> getArrayList(){
        return mArrayList;
    }
    
    public static void pushArrayListElement(String value){
        if(mArrayList == null)
            return;
        
        mArrayList.add(value);
    }
}
