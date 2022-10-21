package com.zhaoxi.springelastic;

import com.zhaoxi.springelastic.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
class SpringElasticApplicationTests {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void createIndex() {
        //创建索引，系统初始化会自动创建索引
        System.out.println("创建索引");
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", " 小米");
        NativeSearchQuery query = new NativeSearchQuery(termQueryBuilder);
        SearchHits<Product> search = elasticsearchRestTemplate.search(query, Product.class);

    }

    @Test
    public void deleteIndex() {
        //删除索引，系统初始化会自动创建索引
        boolean delete = elasticsearchRestTemplate.indexOps(Product.class).delete();
        System.out.println("删除索引 = " + delete);
    }
}
