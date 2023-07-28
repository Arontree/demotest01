package com.nowcoder.community.utils;


import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;


import java.util.Map;
import java.util.UUID;

public class CommunityUtils {
    //生成随机字符串
    public static  String generateUUId(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }
    //MD 5 加密
    //只能加密不能解密hello-》456123
    //hello+3e4a8->7897564 无规律无法获取密码了
    public static String md5(String key){
        if(StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());//这里是要返回十六进制的所以要对string类型进行转换getBytes(String charsetName): 使用指定的字符集将字符串编码为 byte 序列，并将结果存储到一个新的 byte 数组中。
    }

    //
    public static String getJSONString(int code, String msg, Map<String,Object> map){
        JSONObject json=new JSONObject();
        json.put("code",code);
        json.put("msg",msg);
        if(map!=null){
            for (String key:map.keySet()){
                json.put(key,map.get(key));
            }
        }
        return json.toJSONString();
    }
    public static String getJSONString(int code, String msg){
        return getJSONString(code,msg,null);
    }
    public static String getJSONString(int code){
        return getJSONString(code,null,null);
    }
}
