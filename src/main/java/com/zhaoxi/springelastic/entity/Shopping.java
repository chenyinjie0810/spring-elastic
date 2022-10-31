package com.zhaoxi.springelastic.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Date;

/**
 * @author chenyj
 * @desc
 * @date 2022/10/30 19:29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(indexName = "shopping", shards = 3, replicas = 1)
public class Shopping {

    /**
     * 商品唯一标识
     */
    @Id
    private String id;
    private String price;
    private String updateDate;
    private String title;
    private Long sell;
    private Date createDate;
}
