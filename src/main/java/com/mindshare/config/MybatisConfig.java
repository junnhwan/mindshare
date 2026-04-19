package com.mindshare.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@Profile("!bootstrap-test")
public class MybatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.mindshare");
        factoryBean.setMapperLocations(resolveMapperLocations());

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    private Resource[] resolveMapperLocations() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Map<String, Resource> resources = new LinkedHashMap<>();
        for (Resource resource : resolver.getResources("classpath*:mapper/*.xml")) {
            resources.put(resource.getURL().toExternalForm(), resource);
        }
        for (Resource resource : resolver.getResources("classpath*:mapper/**/*.xml")) {
            resources.put(resource.getURL().toExternalForm(), resource);
        }
        return resources.values().toArray(Resource[]::new);
    }
}
