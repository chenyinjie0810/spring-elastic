package com.zhaoxi.springelastic;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxi.springelastic.entity.Product;
import com.zhaoxi.springelastic.entity.Shopping;
import com.zhaoxi.springelastic.repository.ShoppingRepository;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.ParsedMissing;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
class SpringElasticApplicationTests {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ShoppingRepository shoppingRepository;

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

    @Test
    public void matchAll() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(sourceBuilder);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<String> collect = Arrays.stream(search.getHits().getHits()).map(SearchHit::getSourceAsString).collect(Collectors.toList());
        log.info("collect=>{}", collect);
    }


    @Test
    public void groupByPrice() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        sourceBuilder.aggregation(AggregationBuilders.terms("group_by_price").size(20).field("price.keyword"));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedStringTerms aggregation = response.getAggregations().get("group_by_price");
        for (ParsedStringTerms.ParsedBucket bucket : (List<ParsedStringTerms.ParsedBucket>) aggregation.getBuckets()) {
            System.out.println(bucket.getKey());
            System.out.println(bucket.getDocCount());
        }
    }

    @Test
    public void rangePrice() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        RangeAggregationBuilder rangePrice = AggregationBuilders.range("rangePrice").field("sell")
                .addRange(1000, 1500)
                .addRange(1500, 2000);
        sourceBuilder.aggregation(rangePrice);
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedRange aggregation = response.getAggregations().get("rangePrice");
        for (ParsedRange.ParsedBucket bucket : (List<ParsedRange.ParsedBucket>) aggregation.getBuckets()) {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        }
    }

    @Test
    public void countTitle() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        sourceBuilder.aggregation(AggregationBuilders.cardinality("countTitle").field("title.keyword"));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedCardinality aggregation = response.getAggregations().get("countTitle");
        System.out.println(aggregation.getValue());
    }


    @Test
    public void composite() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
        sources.add(new TermsValuesSourceBuilder("title").field("title.keyword"));
        sources.add(new TermsValuesSourceBuilder("price").field("price.keyword").order(SortOrder.DESC));
        CompositeAggregationBuilder composite = AggregationBuilders.composite("composite", sources).size(10);
        // 偏移量
        Map<String, Object> after = new HashMap<>(4);
        after.put("price", 1959);
        after.put("title", "oppo手机");
        composite.aggregateAfter(after);
        sourceBuilder.aggregation(composite);
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedComposite aggregation = response.getAggregations().get("composite");
        Map<String, Object> map = aggregation.afterKey();
        System.out.println(map);
        for (ParsedComposite.ParsedBucket bucket : aggregation.getBuckets()) {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getKey());
            System.out.println(bucket.getDocCount());
        }
    }

    @Test
    public void groupByMonth() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1);
        Script script = new Script(ScriptType.INLINE, DEFAULT_SCRIPT_LANG, "doc['updateDate.keyword'].value.substring(0,7)+params['id']", map);
        sourceBuilder.aggregation(AggregationBuilders.terms("month").script(script));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedStringTerms aggregation = response.getAggregations().get("month");
        for (Terms.Bucket bucket : aggregation.getBuckets()) {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        }
    }

    @Test
    public void save() {
        List<Shopping> shoppingList = new ArrayList<>();
        String[] titles = new String[]{"华为手机", "小米手机", "苹果手机", "oppo手机"};
        for (int i = 3001; i < 4000; i++) {
            DateTime offset = RandomUtil.randomDate(new Date(), DateField.DAY_OF_YEAR, -365, 0);
            String format = DateUtil.format(offset, "yyyy-MM-dd HH:mm:ss");
            int randomInt = RandomUtil.randomInt(1000, 2000);
            Shopping shopping = Shopping.builder()
                    .id(i + "")
                    .title(titles[randomInt % 4])
                    .price(randomInt + "")
                    .sell((long) randomInt)
                    .createDate(offset)
                    .updateDate(format)
                    .build();
            shoppingList.add(shopping);
        }
        shoppingRepository.saveAll(shoppingList);

    }

    @Test
    public void date() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);

        sourceBuilder.aggregation(AggregationBuilders.dateHistogram("date")
                .field("createDate")
                .calendarInterval(DateHistogramInterval.MONTH));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedDateHistogram aggregation = response.getAggregations().get("date");
        for (ParsedDateHistogram.ParsedBucket bucket : (List<ParsedDateHistogram.ParsedBucket>) aggregation.getBuckets()) {
            ZonedDateTime key = (ZonedDateTime) bucket.getKey();
            System.out.println(key);
            System.out.println(key.getMonthValue());
            System.out.println(bucket.getDocCount());
        }
    }


    @Test
    public void count() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);

        sourceBuilder.aggregation(AggregationBuilders.count("count").field("createDate"));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedValueCount aggregation = response.getAggregations().get("count");
        System.out.println(aggregation.getValue());
    }

    @Test
    public void histogram() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);

        sourceBuilder.aggregation(AggregationBuilders.histogram("histogram").field("sell").interval(50));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedHistogram aggregation = response.getAggregations().get("histogram");
        for (Histogram.Bucket bucket : aggregation.getBuckets()) {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        }
        System.out.println(aggregation);
    }

    @Test
    public void miss() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);

//        AggregationBuilders.topHits("");
//        AggregationBuilders.sampler("");
//        AggregationBuilders.nested("");
//        AggregationBuilders.weightedAvg("");
//        AggregationBuilders.extendedStats("");
//        AggregationBuilders.medianAbsoluteDeviation();
//        AggregationBuilders.diversifiedSampler();
//        AggregationBuilders.reverseNested();
//        AggregationBuilders.percentileRanks();
//        AggregationBuilders.percentiles();
        sourceBuilder.aggregation(AggregationBuilders.missing("miss").field("sell"));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedMissing aggregation = response.getAggregations().get("miss");
        System.out.println(aggregation.getDocCount());
    }


    @Test
    public void topHits() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);

        FilterAggregationBuilder filter = AggregationBuilders.filter("filter", QueryBuilders.termQuery("title.keyword", "小米手机"));
        filter.subAggregation(AggregationBuilders.terms("title").field("title.keyword"));

        sourceBuilder.aggregation(filter);
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedFilter aggregation = response.getAggregations().get("filter");
        System.out.println(aggregation.getDocCount());
        ParsedStringTerms agg = aggregation.getAggregations().get("title");
        for (ParsedStringTerms.ParsedBucket bucket : (List<ParsedStringTerms.ParsedBucket>) agg.getBuckets()) {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        }
    }


    @Test
    public void stats() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shopping");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);

        sourceBuilder.aggregation(AggregationBuilders.stats("stats").field("sell"));
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        ParsedStats aggregation = response.getAggregations().get("stats");
        System.out.println(aggregation.getAvg());
        System.out.println(aggregation.getCount());
        System.out.println(aggregation.getMax());
        System.out.println(aggregation.getSum());
        System.out.println(aggregation.getMin());
    }

    public static void main(String[] args) {
        String[] titles = new String[]{"华为手机", "小米手机", "苹果手机", "oppo手机"};
        for (int i = 2001; i < 3000; i++) {
            DateTime offset = RandomUtil.randomDate(new Date(), DateField.DAY_OF_YEAR, -365, 0);
            String format = DateUtil.format(offset, "yyyy-MM-dd HH:mm:ss");
            int randomInt = RandomUtil.randomInt(1000, 2000);

            String s = "{\"index\":{\"_index\":\"shopping\",\"_type\":\"_doc\",\"_id\":\"" + i + "\"}} \n" +
                    "{\"price\":\"" + randomInt + "\",\"sell\":" + randomInt + ", \"updateDate\":\"" + format + "\", \"title\":\"" + titles[randomInt % 4] + "\"}";
            System.out.println(s);
        }
    }

}
