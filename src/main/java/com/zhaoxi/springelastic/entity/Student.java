package com.zhaoxi.springelastic.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chenyj
 * @desc
 * @date 2022/10/24 13:50
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private String name;
    private String nickname;
    private String sex;
    private Integer age;
    private String ip;
}
