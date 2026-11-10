package com.zhanhe.workforce.config;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把上传目录映射成 /uploads/** 静态资源，头像才能直接以 URL 访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer
{
    private final LocalFileStorage storage;

    public WebConfig(LocalFileStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        try
        {
            Path root = storage.rootPath();
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + root.toUri().getPath());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("无法初始化上传目录", e);
        }
    }
}
