package app.config;

import app.supports.converter.LocalDateTimeConverter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.time.format.DateTimeFormatter;

/**
 * @author ngnmhieu
 */
@Configuration
@EnableWebMvc // use spring-mvc default configurations
@ComponentScan(basePackages = "app") // search for beans in package app.web
public class WebConfig extends WebMvcConfigurerAdapter
{
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer)
    {
        configurer.enable();
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
