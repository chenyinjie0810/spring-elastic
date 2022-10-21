package com.zhaoxi.springelastic.repository;

import com.zhaoxi.springelastic.entity.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @author chenyj
 * @desc
 * @date 2022/10/21 16:14
 */
@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, Long> {
}
