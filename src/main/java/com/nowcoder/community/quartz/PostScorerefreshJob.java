package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScorerefreshJob implements Job , CommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(PostScorerefreshJob.class);
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticSearchService elasticSearchService;

    // 网站纪元
    // 这里的epoch是一个固定量需要去初始化
    private static final Date epoch;


    static {
        try {
            // SimpleDateFormat是Java中用于格式化和解析日期的类。
            // parse方法将字符串"2014-08-01 00:00:00"按照指定的格式解析为一个Date对象，表示2014年8月1日零点。
            epoch=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-06-30 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        // 查看是否有更新的操作，operation里面有数据
        // boundSetOps(redisKey)方法是RedisTemplate类中的一个方法，它返回一个BoundSetOperations对象。
        // BoundSetOperations是一个用于操作Set类型数据的接口，它提供了一系列的方法用于对Set进行添加、删除、查询等操作。
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size()==0){
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数: " + operations.size());
        while (operations.size()>0){
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    // 这里是计算帖子分数用的在更新的时候
    private void refresh(int postId){
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post==null){
            logger.error("该帖子不存在: id = " + postId);
            return;
        }

        // 是否精华
        boolean wonderful=post.getStatus()==1;

        // 评论数量
        int commentCount=post.getCommentCount();

        // 点赞数量
        int likeCount=likeService.finUserLikeCount(postId);

        // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;

        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);//天数

        // 更新帖子分数
        discussPostService.updateScore(postId,score);

        // 同步搜索数据
        post.setScore(score);
        elasticSearchService.saveDiscussPost(post);
    }
}
