package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtils;
import com.nowcoder.community.utils.HostHolder;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

//这是帖子相关的控制层
@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;
    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title ,String content){
        User user=hostHolder.getUser();
        if(user==null){
            return CommunityUtils.getJSONString(403,"你还没登陆");
        }
        DiscussPost post=new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        /**
         * 触发发帖事件
         */
        //触发发帖事件
        Event event = new Event()
                .setUserId(user.getId())
                .setTopic(TOPIC_PUBLISH)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityUserId(post.getId());
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,post.getId());

        // 报错的情况,将来统一处理.
        return CommunityUtils.getJSONString(0, "发布成功!");
    }

    @RequestMapping(path = "/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子  通过id找到帖子然后注入到model
        DiscussPost post=discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        //作者  通过帖子用户id查找用户信息注入到model
        User user=userService.findUserById(post.getUserId());
        model.addAttribute("user",user);
        // 点赞数量
        long likeCount = likeService.finEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount",likeCount);
        //点赞状态
        int likeStatus =hostHolder.getUser()==null?0:
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus",likeStatus);
        //评论区分页信息
        page.setLimit(5);//set 显示上限
        page.setPath("/discuss/detail/"+discussPostId);//设置查询路径
        page.setRows(post.getCommentCount());//获取分页数

        // 评论: 给帖子的评论comment
        // 回复: 给评论的评论reply
        // 评论列表
        //评论数据库获取
        //entity_type是评论的类型：1.给帖子的评论 2.给评论的评论  entity_id:是评论的标识符，标识这些评论是给哪些帖子或者评论的
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论VO列表 VO这是用来存储评论，包括评论的各项信息和回复信息在这个集合里
        List<Map<String,Object>> commentVoList=new ArrayList<>();
        if(commentList!=null){//这里进行循环遍历配置数据，放入model
            for (Comment comment:commentList){
                //评论VO
                Map<String,Object> commentVo=new HashMap<>();
                //评论
                commentVo.put("comment",comment);
                //作者
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                // 点赞数量
                likeCount = likeService.finEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                // 点赞状态
                likeStatus =hostHolder.getUser()==null ? 0:
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus",likeStatus);
                //回复列表 这里是每条评论的回复，根据评论的类型和评论的id去获取
                List<Comment> replyList=commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE
                );
                //回复VO列表
                List<Map<String,Object>> replyVoList=new ArrayList<>();
                if(replyList!=null){
                    for(Comment reply:replyList){
                        Map<String,Object> replyVo=new HashMap<>();
                        //回复
                        replyVo.put("reply",reply);
                        //作者
                        replyVo.put("user",userService.findUserById((reply.getUserId())));
                        //回复目标
                        User target=reply.getTargetId()==0?null:userService.findUserById(reply.getUserId());
                        replyVo.put("target",target);
                        // 点赞数量
                        likeCount = likeService.finEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount);
                        // 点赞状态
                        likeStatus =hostHolder.getUser()==null ? 0:
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus",likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                    commentVo.put("replys",replyVoList);//这是给某条评论注入它的回复

                //回复数量
                int replyCount=commentService.finCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("replyCount",replyCount);

                commentVoList.add(commentVo);

            }
        }
        model.addAttribute("comments",commentVoList);

        return "/site/discuss-detail";
    }

    // 置顶 传入帖子id
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);

        // 触发发帖事件
        // 因为这时帖子发生了改变需要把最新帖子发到elastic中
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtils.getJSONString(0);
    }

    // 加精 传入帖子id
    @RequestMapping(path = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);

        // 触发发帖事件
        // 因为这时帖子发生了改变需要把最新帖子发到elastic中
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey=RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtils.getJSONString(0);
    }

    // 删除 传入帖子id
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);

        // 触发发帖事件
        // 因为这时帖子发生了改变需要把最新帖子发到elastic中
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtils.getJSONString(0);
    }
}
