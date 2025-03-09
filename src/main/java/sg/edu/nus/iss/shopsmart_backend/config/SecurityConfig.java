package sg.edu.nus.iss.shopsmart_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sg.edu.nus.iss.shopsmart_backend.filter.GcipAuthenticationFilter;
import sg.edu.nus.iss.shopsmart_backend.service.AuthService;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

import java.util.Arrays;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService authService,
                                                   CommonService commonService, RedisManager redisManager) throws Exception{
        http
                .authorizeHttpRequests(request -> {
                    request.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    request.requestMatchers("/auth/google/**", "/auth/native/**", "/profile/**", "/redis-api/**", "/", "/home").permitAll();
                    request.anyRequest().authenticated();
                })
                .csrf(AbstractHttpConfigurer::disable)
                .cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))
                .addFilterBefore(new GcipAuthenticationFilter(authService, commonService, redisManager), SecurityContextHolderAwareRequestFilter.class);
        return http.build();
    }

//    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Setting up CORS configuration");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://app.shopsmartsg.com", "https://shopsmartsg.com", "https://accounts.google.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("DNT","User-Agent","X-Requested-With","If-Modified-Since","Cache-Control","Content-Type","Range","Authorization"));
        // Optionally set allow credentials, max age, etc.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
