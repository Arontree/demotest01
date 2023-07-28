package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticSearchTests {
    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testInsert(){
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList(){
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133,0,100,0));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134,0,100,0));
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人,使劲灌水.");
        discussPostRepository.save(post);
    }

    @Test
    public void testDelete() {
        // discussRepository.deleteById(231);
        discussPostRepository.deleteAll();
    }

    @Test
    // repositopry可以但是不完善
    public void tsetSearchByRepository(){
        // SearchQuery可以是一个类或结构体，用于封装搜索查询的相关信息。

        // NativeSearchQueryBuilder()是一个用于构建搜索查询的类。
        // 它是Spring Data Elasticsearch库中的一个类，用于构建Elasticsearch的原生搜索查询。

        // QueryBuilders.multiMatchQuery是一个Elasticsearch的查询构建器，用于构建多字段的匹配查询。
        // 它可以在多个字段中搜索指定的关键词，并返回匹配的文档。

        // withPageable(PageRequest.of(0,10))是Spring Data JPA中用于分页查询的方法。
        // 它用于指定查询结果的页码和每页的条目数。
        SearchQuery searchQuery=new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // elasticsearchTemplate.queryForPage(searchQuery, class,SearchResultMapper)
        // 底层获取得到了高亮显示的值, 但是没有返回.

        //把获取的值传给页面
        // page实现了iterable接口所以可以遍历
        Page<DiscussPost> page=discussPostRepository.search(searchQuery);
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post:page){
            System.out.println(post);//从es服务器里面搜索出来的
        }
    }

    // 通过template进行查询
    @Test
    public void testSearchByTemplate(){
        SearchQuery searchQuery=new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // 前后都一样和之前就是中间处理这里不一致，这里需要把返回的高亮page返回回来

        // searchQuery表示查询条件，DiscussPost.class表示查询结果的类型，SearchResultMapper表示结果映射器。
        // 在SearchResultMapper的mapResults方法中，可以对查询结果进行处理和转换。
        Page<DiscussPost> page=elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                // 通过searchResponse来获取查询结果的Hits信息，就是标红信息，然后根据需要进行处理和转换。
                SearchHits hits = searchResponse.getHits();
                if (hits.getTotalHits()<=0){
                    return null;
                }

                List<DiscussPost> list=new ArrayList<>();
                for (SearchHit hit:hits){
                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));// valueof 转换为Integer对象：

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果,放入post中
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField!=null){
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField!=null){
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                //AggregatedPageImpl 是 Spring Data Elasticsearch 中的一个类，用于表示聚合分页的结果。

                //AggregatedPageImpl 的构造函数参数如下：
                //list：聚合分页的结果列表。它是一个包含实际聚合结果的列表。
                //pageable：分页信息。它指定了当前页码、每页大小等分页相关的信息。
                //totalHits：总命中数。它表示与查询条件匹配的文档的总数。
                //aggregations：聚合结果。它是一个包含聚合信息的对象，可以从中获取各种聚合的结果。
                //scrollId：滚动查询的 ID。它是一个用于继续滚动查询的标识符。
                //maxScore：最大分数。它表示与查询条件匹配的文档中的最高分数。
                //这些参数用于创建 AggregatedPageImpl 对象，以便在聚合分页的结果中包含必要的信息。
                return new AggregatedPageImpl(list,pageable,hits.getTotalHits(),
                        searchResponse.getAggregations(),searchResponse.getScrollId(),
                        hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post:page){
            System.out.println(post);//从es服务器里面搜索出来的
        }
    }
}
