package com.zhaoxi.springelastic.repository;

import com.zhaoxi.springelastic.entity.Product;
import com.zhaoxi.springelastic.entity.Shopping;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @author chenyj
 * @desc
 * @date 2022/10/30 19:35
 */
@Repository
public interface ShoppingRepository extends ElasticsearchRepository<Shopping, Long> {
}
