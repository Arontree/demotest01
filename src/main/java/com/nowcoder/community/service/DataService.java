package com.nowcoder.community.service;

import com.nowcoder.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;


    // 设立时间格式
    private SimpleDateFormat df=new SimpleDateFormat("yyyyMMdd");

    // 将指定的IP计入UV
    public void recordUv(String ip){
        String redisKey= RedisKeyUtil.getUVKey(df.format(new Date()));
        if (redisTemplate.hasKey(redisKey)){
            return;
        }
        redisTemplate.opsForHyperLogLog().add(redisKey,ip);
    }

    // 统计指定日期范围内的UV
    public long calculateUV (Date start,Date end){
        if (start==null || end==null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        // 整理该日期范围内的key
         List<String> keyList = new ArrayList<>();
        // 用于获取默认时区和语言环境的 Calendar 对象。
        // 它返回一个 Calendar 实例，该实例根据当前的时区和语言环境进行初始化。
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE,1);// 增加一天
        }
        // 合并这些数据
        String redisKey=RedisKeyUtil.getUVKey(df.format(start),df.format(end));// 合并后的key
        //union() 方法用于计算多个 HyperLogLog 集合的并集，并返回一个近似的基数估计结果。它接受两个参数：
        //redisKey：表示要进行并集操作的 HyperLogLog 集合的键名。
        //keyList：表示要与 redisKey 进行并集操作的其他 HyperLogLog 集合的键名列表。
        redisTemplate.opsForHyperLogLog().union(redisKey,keyList.toArray());//

        //返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    // 将指定用户计入DAU
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        //setBit(redisKey, userId, true)：将指定位置的位设置为1。
        //redisKey：Redis数据库中的键。
        //userId：要设置的位所在的位置。
        //true：要设置的值，这里设置为1。
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    // 统计指定日期范围内的DAU
    public long calculateDAU (Date start,Date end){
        if (start==null || end==null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        // 整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        // 用于获取默认时区和语言环境的 Calendar 对象。
        // 它返回一个 Calendar 实例，该实例根据当前的时区和语言环境进行初始化。
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getDAUVKey(df.format(calendar.getTime()));
            // 使用平台的默认字符集将字符串编码为 byte 序列，并将结果存储到一个新的 byte 数组中。
            // bitmap的key参数要求是一个一维byte数组类型
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE,1);// 增加一天
        }
        // 进行OR运算
        // RedisCallback是一个函数式接口，它定义了一个doInRedis方法，该方法在执行Redis操作时被调用。
       return (long) redisTemplate.execute(new RedisCallback() {
           @Override
           public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
               String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));// or运算后的key
               redisConnection.bitOp(
                       RedisStringCommands.BitOperation.OR,//操作
                       redisKey.getBytes(),//
                       keyList.toArray(new byte[0][0]));// 声明长度是0


               return redisConnection.bitCount(redisKey.getBytes());
           }
       });


    }

}
