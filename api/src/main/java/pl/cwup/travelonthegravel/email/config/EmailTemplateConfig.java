package pl.cwup.travelonthegravel.email.config;

import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class EmailTemplateConfig {

    @Bean
    public ClassLoaderTemplateResolver textTemplateResolver() {
        final var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setResolvablePatterns(Set.of("*.txt"));
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);
        return resolver;
    }
}
