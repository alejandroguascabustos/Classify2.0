package com.classify20.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final UploadStorageResolver uploadStorageResolver;

    public WebConfig(AuthInterceptor authInterceptor, UploadStorageResolver uploadStorageResolver) {
        this.authInterceptor = authInterceptor;
        this.uploadStorageResolver = uploadStorageResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/agenda", "/agenda/**",
                        "/api/agendas", "/api/agendas/**",
                        // /api/chatbot NO va aquí: el chatbot vive también en las
                        // páginas públicas (login, inicio, nosotros...). Sin sesión
                        // el ChatbotService responde solo con la guía, sin agenda.
                        "/aprende", "/aprende/**",
                        "/clases-agendadas", "/clases-agendadas/**",
                        "/contacta", "/contacta/**",
                        "/materiales", "/materiales/**",
                        "/menu", "/menu/**",
                        "/mismateriales", "/mismateriales/**",
                        "/noticias", "/noticias/**",
                        "/gestion-registros", "/gestion-registros/**",
                        "/gestion-permisos", "/gestion-permisos/**",
                        "/izada", "/izada/**",
                        // Cuelga de la raiz, no de /agenda, por lo que los patrones
                        // anteriores no lo cubrian: hasta ahora aceptaba escrituras
                        // sin sesion iniciada.
                        "/guardar-agenda"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadStorageResolver.toResourceLocation());
    }
}
