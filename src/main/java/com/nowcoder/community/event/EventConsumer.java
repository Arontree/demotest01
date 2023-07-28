package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.utils.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static  final Logger logger= LoggerFactory.getLogger(EventConsumer.class);


    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticSearchService elasticSearchService;

    @KafkaListener(topics = {TOPIC_LIKE,TOPIC_FOLLOW,TOPIC_COMMENT})
    //参数用来接受相关数据
    public void handleComment(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息内容为空");
            return;
        }
        System.out.println(record.value().toString());
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);//将JSON字符串转换为对应的Java对象，并且可以指定要转换的Java对象类型。
        if(event==null){
            logger.error("消息格式错误");
            return;
        }
        //获取的原始信息，要进行处理，数据转化为message
        Message message = new Message();
        message.setFromId(SYSTEM_USERID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());

        if(!event.getData().isEmpty()){
            //它返回一个包含键值对（Entry）的Set集合。每个Entry对象都包含一个键和对应的值
            //entrySet()方法常用于遍历Map集合中的键值对，可以通过迭代器或者增强for循环来遍历entrySet()返回的Set集合。
            // 通过遍历entrySet()可以获取每个键值对的键和值，进而进行相应的操作。
            //Map.Entry<String, Object> 表示一个具体的键值对，可以通过遍历 Map 的 entrySet() 方法获取。
            //Map<String, Object> 表示一个整个映射表，包含了多个键值对。
            for (Map.Entry<String,Object> entry:event.getData().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        //
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event=JSONObject.parseObject(record.value().toString(),Event.class);

        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getUserId());
        elasticSearchService.saveDiscussPost(post);
    }

    // 消费删除帖子事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event=JSONObject.parseObject(record.value().toString(),Event.class);

        if (event==null){
            logger.error("消息格式错误");
            return;
        }

        elasticSearchService.deleteDiscussPost(event.getEntityId());
    }
}
