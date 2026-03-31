package com.classify20.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    // Lee el path desde application.properties
    @Value("${classify.upload.path:C:/classify-uploads}")
    private String uploadPath;

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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Normaliza la ruta: agrega / al final si no lo tiene
        String normalizedPath = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";

        // Sirve /uploads/** desde el directorio absoluto configurado
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + normalizedPath);
    }
}