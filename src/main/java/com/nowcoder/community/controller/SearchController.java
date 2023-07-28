package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    // 参数传递：GET请求中的参数通常通过URL的查询字符串传递。参数会附加在URL的末尾，以键值对的形式出现，多个参数之间使用"&"符号分隔。
    // 参数"keyword"和"current"是通过查询字符串的形式传递给后端程序的。查询字符串是URL中的一部分，用于传递键值对参数。
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model){
        // 搜索帖子给页面
        org.springframework.data.domain.Page<DiscussPost> searchResult=elasticSearchService.
                searchDiscussPost(keyword,page.getCurrent()-1,page.getLimit());// 这里注意current-1

        // 分页信息
        page.setPath("/search?keyword="+keyword);//这里不能少了等于号
        page.setRows(searchResult==null?0:(int)searchResult.getTotalElements());

        // 聚合数据
        List<Map<String,Object>> discussPosts=new ArrayList<>();// 弄个list装帖子和其他数据的集合
        if (searchResult!=null){
            for (DiscussPost post:searchResult){
                Map<String,Object> map=new HashMap<>();
                // 帖子
                map.put("post",post);
                // 作者
                map.put("user",userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount",likeService.finEntityLikeCount(ENTITY_TYPE_POST,post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);

        return "/site/search";

    }
}
