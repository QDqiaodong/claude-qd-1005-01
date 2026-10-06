package com.utility.tunnel.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 本仓数据访问以 MyBatis XML 为主：Mapper 接口 + resources/mapper/*.xml。
 * 关联映射用嵌套 &lt;collection&gt; resultMap，动态条件由 Mapper 接口方法驱动。
 */
@Configuration
@MapperScan("com.utility.tunnel.mapper")
public class MyBatisConfig {
}
