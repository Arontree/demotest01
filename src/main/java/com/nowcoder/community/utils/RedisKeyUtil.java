package com.nowcoder.community.utils;

import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.common.protocol.types.Field;

public class RedisKeyUtil {
    private static final String SPLIT=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_User_LIKE="like:user";
    private static final String PREFIX__FOLLOWEE="like:followee";
    private static final String PREFIX_FOLLOWER="like:follower";
    private static final  String PREFIX_KAPTCHA="kaptcha";
    private static final String PREFIX_TICKET="ticket";
    private static final String PREFIX_USER="user";
    private static final String PREFIX_UV="uv";// 独立访客
    private static final String PREFIX_DAU="dau";// 日活跃用户
    private static final String PREFIX_POST = "post";

    // 某个实体的赞，这是从redis中取值的key
    // like:entity:entityTyoe:entityId->set(userId)
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+entityId;
    }

    //某个用户的赞
    //like:user:userId
    public static String getUserLikeKey(int userId){
        return PREFIX_User_LIKE+SPLIT+userId;
    }

    //某个用户userId关注的实体entityType,这样就知道自己关注了什么实体，包括类型和Id
    //followee:userId:entityType->zset(entityId,now) 用户：实体类型:实体编号
    public  static  String getFolloweeKey(int userId,int entityType){
        return PREFIX__FOLLOWEE+SPLIT+userId+SPLIT+entityType;
    }

    //某个实体拥有的粉丝，这样就知道实体被什么人关注了
    //follower:entityType:entityId->zset(userId,now) 实体种类：实体编号：（关注实体的人）
    public  static  String getFollowerKey(int entityType,int entityId){
        return PREFIX_FOLLOWER+SPLIT+entityType+SPLIT+entityId;
    }

    //登录验证码
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA+SPLIT+owner;
    }
    // 登录的凭证
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    // 用户
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    //单日UV
    public static String getUVKey(String date){
        return PREFIX_UV+SPLIT+date;
    }

    //区间UV
    public static String getUVKey(String startDate,String endDate){
        return PREFIX_UV+SPLIT+startDate+SPLIT+endDate;
    }

    // 单日活跃用户
    public static String getDAUVKey(String date){
        return PREFIX_DAU+SPLIT+date;
    }

    // 区间活跃用户
    public static String getDAUKey(String startDate,String endDate){
        return PREFIX_DAU+SPLIT+startDate+SPLIT+endDate;
    }

    // 帖子分数
    public static String getPostScoreKey(){
        return PREFIX_POST + SPLIT + "score";
    }

}

