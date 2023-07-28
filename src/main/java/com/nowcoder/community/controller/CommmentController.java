package com.nowcoder.community.controller;


import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtils;
import com.nowcoder.community.utils.HostHolder;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;


//这是评论相关的
@Controller
@RequestMapping("/comment")
public class CommmentController implements CommunityConstant {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}",method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setCreateTime(new Date());
        comment.setStatus(0);
        commentService.addComment(comment);

        //触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())//当前用户
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId",discussPostId);
        //这里这个if只是为了获取不同评论时的实体作者id
        if (comment.getEntityType()==ENTITY_TYPE_POST){//如果是评论帖子
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());//通过评论实体id获取评论的目标
            event.setEntityUserId(target.getUserId());//评论帖子的发布者
        }else if (comment.getEntityType()==ENTITY_TYPE_COMMENT)//如果是回复评论
            {
                Comment target = commentService.findCommentById(comment.getEntityId());
                event.setEntityUserId(target.getUserId());//评论帖子的发布者
        }

        eventProducer.fireEvent(event);

        if (comment.getEntityType()==ENTITY_TYPE_POST){
            event = new Event()
                    .setUserId(hostHolder.getUser().getId())
                    .setTopic(TOPIC_PUBLISH)
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityUserId(discussPostId);
            eventProducer.fireEvent(event);
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }
        return "redirect:/discuss/detail/"+discussPostId;
    }





}
