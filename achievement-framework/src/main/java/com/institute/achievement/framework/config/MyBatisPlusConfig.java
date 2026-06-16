package com.institute.achievement.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.institute.achievement.framework.permission.DataPermissionInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration.
 * Configures pagination interceptor, data permission injection, and optimistic locking.
 */
@Configuration
@MapperScan({"com.institute.achievement.**.mapper"})
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus interceptor chain with pagination, data permission, and optimistic locking.
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Data permission interceptor: injects dept_id filter on @DataScope annotated methods
        interceptor.addInnerInterceptor(new DataPermissionInterceptor());

        // Pagination interceptor for MySQL
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);

        // Optimistic locker for concurrent update scenarios
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}
