package com.zhaoxi.springelastic.entity;

import lombok.Data;

/**
 * @author chenyj
 * @desc
 * @date 2022/10/24 14:02
 */
@Data
public class ResponseModel {
    private Long code;
    private String data;
    private String user_ip;

}
