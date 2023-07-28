package com.nowcoder.community.entity;



import java.util.HashMap;
import java.util.Map;


public class Event {
    private String topic;//事件类型
    private int userId;//事件的用户，发出的来源
    private int entityId;//实体Id
    private int entityType;//事件的实体类类型，是评论还是点赞
    private int entityUserId;//实体的作者
    private Map<String,Object> data=new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic){
        this.topic=topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key,Object value) {
        this.data.put(key,value);
        return this;
    }




}
