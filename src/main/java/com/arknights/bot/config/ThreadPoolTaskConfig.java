package com.arknights.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by wangzhen on 2023/2/14 11:56
 */
@Slf4j
@Configuration
public class ThreadPoolTaskConfig {
    /**
     * 线程池配置信息，线程池主要用于发送消息。大图发送容易造成消息阻塞
     *
     * @return
     */
    @Bean(name = "taskModuleExecutor")
    public Executor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        executor.setThreadGroupName("arknights");
        //  线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setThreadNamePrefix("arknights");
        // 线程池基本大小
        executor.setCorePoolSize(2);
        // 设置线程池最大数量
        // Runtime.getRuntime().availableProcessors()返回的是可用的计算资源,我的拯救者为6核，此处打印输出为12
        // executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        log.info("打印可用计算资源数{}", Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(5);
        // 线程空闲后的存活时间
        executor.setQueueCapacity(Integer.MAX_VALUE);
        // 当队列和最大线程池都满了之后的饱和处理策略:CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

}
