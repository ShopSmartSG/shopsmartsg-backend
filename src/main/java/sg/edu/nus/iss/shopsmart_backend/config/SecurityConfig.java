package sg.edu.nus.iss.shopsmart_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import sg.edu.nus.iss.shopsmart_backend.filter.GcipAuthenticationFilter;
import sg.edu.nus.iss.shopsmart_backend.service.AuthService;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService authService,
                                                   CommonService commonService, RedisManager redisManager) throws Exception{
        http
                .authorizeHttpRequests(request -> {
                    request.requestMatchers("/auth/google/**", "/auth/native/**", "/profile/**", "/redis-api/**").permitAll();
                    request.anyRequest().authenticated();
                })
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new GcipAuthenticationFilter(authService, commonService, redisManager), SecurityContextHolderAwareRequestFilter.class);
        return http.build();
    }
}
