package com.nowcoder.community.controller;


import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtils;
import com.nowcoder.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.rmi.MarshalledObject;
import java.util.*;


//这是消息相关的
@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @RequestMapping(path = "/letter/list",method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();
        //分页信息
        page.setLimit(5);//设置每页展示的会话数量
        page.setRows(messageService.findConversationCount(user.getId()));//获取总行数
        page.setPath("/letter/list");//设置页码跳转路径

        //会话列表
        List<Message> conversationList=messageService.
                findConversations(user.getId(),page.getOffset(),page.getLimit());//获取所有会话
        List<Map<String,Object>> conversations=new ArrayList<>();//会话信息传入
        if (conversationList!=null){
            for (Message message:conversationList){
                Map<String,Object> map=new HashMap<>();
                map.put("conversation",message);
                map.put("letterCount",messageService.findLettersCount(message.getConversationId()));
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(),message.getConversationId()));
                int targetId=user.getId()==message.getFromId()?message.getToId():message.getFromId();
                map.put("target",userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);

        //查询未被消息数量
        int letterUnreadCount =messageService.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);

        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId,Page page,Model model){
        //Integer.valueOf("abc"); 异常bug 跳转erro页面
        //分页信息
        //前端会存储部分信息，对分页内容进行更新，例如当前页信息之类的
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);//设置分页跳转路径
        page.setRows(messageService.findLettersCount(conversationId));

        //私信列表
        List<Message> letterList=messageService.findLetters(conversationId,page.getOffset(),page.getLimit());//获取对于某人的所有私信
        List<Map<String,Object>> letters=new ArrayList<>();
        if(letterList!=null){
            for (Message message:letterList){
                Map<String,Object> map=new HashMap<>();
                map.put("letter",message);//单一私信传入
                map.put("fromUser",userService.findUserById(message.getFromId()));//单一私信的发送者获取
                letters.add(map);
            }
        }
        model.addAttribute("letters",letters);//所有私信注入

        //私信页面内我的私信目标
        model.addAttribute("target",getLetterTarget(conversationId));

        //设置已读
        List<Integer> ids = getLetterIds(letterList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "/site/letter-detail";
    }

    //获取私信的目标
    private User getLetterTarget(String conversationId){
        String[] ids=conversationId.split("_");
        int id0=Integer.parseInt(ids[0]);
        int id1=Integer.parseInt(ids[1]);

        if(hostHolder.getUser().getId()==id0){
            return userService.findUserById(id1);
        }else {
            return  userService.findUserById(id0);
        }
    }
     //获取未读私信的id
    private List<Integer> getLetterIds(List<Message> letterList){
        List<Integer> ids=new ArrayList<>();

        if (letterList!=null){
            for (Message message:letterList){
                if (hostHolder.getUser().getId()==message.getToId() && message.getStatus()==0){
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    /**
     * @description:  发送私信
     * @param:  发送目标  发送内容
     * @return:
     * @author
     * @date:
     */
    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        //Integer.valueOf("abc"); 异常bug
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtils.getJSONString(1, "目标用户不存在!");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtils.getJSONString(0);
    }
    
    /** 
     * @description:  通知列表显示,视图数据获取
     * @param:  
     * @return:  
     * @author 
     * @date:  
     */
    @RequestMapping(path = "notice/list",method = RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();

        //查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String,Object> messageVO=new HashMap<>();
        if (message !=null){
            messageVO.put("message",message);//把通知信息传入

            String content= HtmlUtils.htmlUnescape(message.getContent());//获取message里的内容，但是不要转义
            Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);//把内容转为hashmap格式

            messageVO.put("user",userService.findUserById( (Integer)data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            //统计主题消息:通过当前用户id，和消息通知类型获取，全部评论的通知总数
            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count",count);

            //统计未读通知
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unread",unread);
            model.addAttribute("commentNotice",messageVO);
        }


        //查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageVO=new HashMap<>();
        if (message!=null){
            messageVO.put("message",message);//把通知信息传入

            String content= HtmlUtils.htmlUnescape(message.getContent());//获取message里的内容，但是不要转义
            Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);//把内容转为hashmap格式

            messageVO.put("user",userService.findUserById( (Integer)data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            //统计主题消息:通过当前用户id，和消息通知类型获取，全部评论的通知总数
            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count",count);

            //统计未读通知
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unread",unread);
            model.addAttribute("likeNotice",messageVO);
        }


        //查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageVO=new HashMap<>();
        if (message!=null){
            messageVO.put("message",message);//把通知信息传入

            String content= HtmlUtils.htmlUnescape(message.getContent());//获取message里的内容，但是不要转义
            Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);//把内容转为hashmap格式

            messageVO.put("user",userService.findUserById( (Integer)data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            /*messageVO.put("postId",data.get("postId"));*///因为是关注用户，没有关注帖子所以不用进行帖子的展示

            //统计主题消息:通过当前用户id，和消息通知类型获取，全部评论的通知总数
            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count",count);

            //统计未读通知
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread",unread);
            model.addAttribute("followNotice",messageVO);
        }


        // 查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}",method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic,Page page,Model model){
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));

        List<Message> noticeList=messageService.findNotices(user.getId(),topic,page.getOffset(),page.getLimit());
        List<Map<String,Object>> noticeVoList=new ArrayList<>();
        if (noticeList!=null){
            for (Message notice:noticeList){
                Map<String,Object> map=new HashMap<>();
                //通知获取
                map.put("notice",notice);
                //内容加载
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object> data=JSONObject.parseObject(content,HashMap.class);
                map.put("user",userService.findUserById((Integer) data.get("userId")));//这里获取的是实体的创建用户，对你点赞评论关注的用户
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));
                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }

}

