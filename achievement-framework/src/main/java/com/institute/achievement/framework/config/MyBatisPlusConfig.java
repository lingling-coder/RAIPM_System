package com.institute.achievement.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.institute.achievement.framework.api.ApiConfigMapper;
import com.institute.achievement.framework.audit.AuditLogMapper;
import com.institute.achievement.framework.file.FileRecordMapper;
import com.institute.achievement.framework.permission.DataPermissionInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration.
 * Configures pagination interceptor, data permission injection, and optimistic locking.
 */
@Configuration
@MapperScan({"com.institute.achievement.module.system.mapper",
    "com.institute.achievement.fee.mapper",
    "com.institute.achievement.paper.mapper",
    "com.institute.achievement.attachment.mapper",
    "com.institute.achievement.patent.mapper",
    "com.institute.achievement.copyright.mapper",
    "com.institute.achievement.reminder.mapper",
    "com.institute.achievement.framework.dashboard.mapper",
    "com.institute.achievement.framework.search.mapper"})
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus interceptor chain with pagination, data permission, and optimistic locking.
     */
    /**
     * Register ApiConfigMapper as a MyBatis mapper bean.
     * This mapper is in the .api package (not covered by @MapperScan packages).
     */
    @Bean
    public MapperFactoryBean<ApiConfigMapper> apiConfigMapper(SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<ApiConfigMapper> factory = new MapperFactoryBean<>(ApiConfigMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    /**
     * Register AuditLogMapper as a MyBatis mapper bean.
     * This mapper is in the .audit package (not covered by @MapperScan packages).
     */
    @Bean
    public MapperFactoryBean<AuditLogMapper> auditLogMapper(SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<AuditLogMapper> factory = new MapperFactoryBean<>(AuditLogMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    /**
     * Register FileRecordMapper as a MyBatis mapper bean.
     * This mapper is in the .file package (not covered by @MapperScan packages).
     */
    @Bean
    public MapperFactoryBean<FileRecordMapper> fileRecordMapper(SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<FileRecordMapper> factory = new MapperFactoryBean<>(FileRecordMapper.class);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

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
