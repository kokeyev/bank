package org.author.demo.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "org.author.demo.controllers")
public class WebConfig implements WebMvcConfigurer {

  private final ApplicationContext applicationContext;
  private final LoginRequiredInterceptor loginRequiredInterceptor;

  public WebConfig(ApplicationContext applicationContext, LoginRequiredInterceptor loginRequiredInterceptor) {
    this.applicationContext = applicationContext;
    this.loginRequiredInterceptor = loginRequiredInterceptor;
  }

  @Bean
  public SpringResourceTemplateResolver templateResolver() {

    SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();

    resolver.setApplicationContext(applicationContext);
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html");
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    return resolver;
  }

  @Bean
  public SpringTemplateEngine templateEngine() {

    SpringTemplateEngine engine = new SpringTemplateEngine();

    engine.setTemplateResolver(templateResolver());

    return engine;
  }

  @Bean
  public ViewResolver viewResolver() {

    ThymeleafViewResolver resolver = new ThymeleafViewResolver();

    resolver.setTemplateEngine(templateEngine());

    resolver.setCharacterEncoding("UTF-8");

    return resolver;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {

    registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");

    registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");

    registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginRequiredInterceptor);
  }

  @Override
  public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    configurer.enable();
  }
}
