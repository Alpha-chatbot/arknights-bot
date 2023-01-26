package com.arknights.bot.config;

/**
 * Created by wangzhen on 2023/1/20 19:05
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


/**
 * @Configuration 相当于 spring中的 application.xml
 */
@Configuration
public class ConfigBean {

    @Bean
    RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
