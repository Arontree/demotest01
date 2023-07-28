package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.utils.SensitiveFilter;
import org.apache.coyote.OutputBuffer;
import org.apache.logging.log4j.message.ReusableMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // caffeine核心接口：Cache,Loadingcache,AsyncLodingCache

    // 帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    private static final Logger logger= LoggerFactory.getLogger(DiscussPostService.class);
    @PostConstruct
    //它用于在依赖注入完成后执行初始化方法
    public void init(){
        //初始化帖子列表缓存
        postListCache= Caffeine.newBuilder()
                .maximumSize(maxSize)// 设置最大缓存数量
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)// 设置缓存更新时间
                .build(new CacheLoader<String, List<DiscussPost>>() { // 这里是加载缓存
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key==null || key.length()==0){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        // 因爲key是兩個黏在一起的要分開
                        String[] params=key.split(":");
                        if (params==null || params.length!=2){
                            throw new IllegalArgumentException("参数错误");
                        }

                        int offset=Integer.valueOf(params[0]);
                        int limit=Integer.valueOf(params[1]);

                        // 二级缓存：redis->mysql
                        // 查看redis内是否存在缓存数据,没有就从数据库中加载

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                    }
                });
        postRowsCache=Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer Key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(Key);
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId,int offet,int limit,int orderMode){
        if (userId==0 && orderMode==1){
            // 有限的场景才缓存,外人只能看熱門帖子
            return postListCache.get(offet+":"+limit);// 在這獲取加載的帖子範圍
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId,offet,limit,orderMode);
    }

    public int findDiscussRows(int userId){
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost discussPost){
        if(discussPost==null){
            throw new IllegalArgumentException("");
        }

        //转义html标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        //过滤敏感词
        discussPost.setTitle((sensitiveFilter.filter(discussPost.getTitle())));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    // 更新评论数
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    //更新帖子状态和是否置顶
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }

    public int updateStatus(int id,int type){
        return discussPostMapper.updateStatus(id,type);
    }

    // 更新帖子分数
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
