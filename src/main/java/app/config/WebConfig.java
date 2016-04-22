package app.config;

import app.supports.converter.LocalDateTimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.format.DateTimeFormatter;

/**
 * @author ngnmhieu
 */
@Configuration
@EnableWebMvc // use spring-mvc default configurations
@ComponentScan(basePackages = "app") // search for beans in package app.web
public class WebConfig extends WebMvcConfigurerAdapter
{
    @Bean
    public ViewResolver viewResolver()
    {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    //@Override
    //public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer)
    //{
    //    configurer.enable();
    //}

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        boolean cacheResources = false;

        registry.addResourceHandler("/**")
                .addResourceLocations("/public/")
                .resourceChain(cacheResources);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer)
    {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void addFormatters(FormatterRegistry registry)
    {
        registry.addConverter(new LocalDateTimeConverter(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
