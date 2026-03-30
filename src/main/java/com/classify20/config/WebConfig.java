package com.classify20.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/agenda", "/agenda/**",
                        "/aprende", "/aprende/**",
                        "/califica", "/califica/**",
                        "/contacta", "/contacta/**",
                        "/materiales", "/materiales/**",
                        "/menu", "/menu/**",
                        "/mismateriales", "/mismateriales/**",
                        "/notas", "/notas/**",
                        "/noticias", "/noticias/**",
                        "/programacion", "/programacion/**",
                        "/izada", "/izada/**"
                );
    }
}
