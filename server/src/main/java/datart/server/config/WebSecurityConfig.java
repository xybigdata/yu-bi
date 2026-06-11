package datart.server.config;

import datart.security.oauth2.ClientRegistrationRepositoryImpl;
import datart.security.oauth2.CustomOAuth2AuthorizationRequestRedirectFilter;
import datart.security.oauth2.CustomOauth2AuthenticationFilter;
import datart.server.config.filter.JwtRequestAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;

import static datart.core.common.Application.getApiPrefix;

@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig {

    private OAuth2ClientProperties oAuth2ClientProperties;

    private Oauth2AuthenticationSuccessHandler authenticationSuccessHandler;

    private Oauth2AuthenticationFailureHandler authenticationFailureHandler;

    private ClientRegistrationRepositoryImpl clientRegistrations;

    private JwtRequestAuthenticationFilter jwtRequestAuthenticationFilter;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(getApiPrefix() + "/tpa");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        http.addFilterBefore(jwtRequestAuthenticationFilter, OAuth2AuthorizationRequestRedirectFilter.class);
        if (this.oAuth2ClientProperties != null) {
            http.addFilterBefore(new CustomOAuth2AuthorizationRequestRedirectFilter(clientRegistrations), OAuth2AuthorizationRequestRedirectFilter.class);
            http.addFilterBefore(new CustomOauth2AuthenticationFilter(clientRegistrations, authenticationSuccessHandler), OAuth2LoginAuthenticationFilter.class);
            http.oauth2Login(oauth2Login -> oauth2Login
                    .failureHandler(authenticationFailureHandler)
                    .clientRegistrationRepository(clientRegistrations)
                    .successHandler(authenticationSuccessHandler)
                    .loginPage("/"));
            http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(getApiPrefix() + "/tpa").permitAll());
            http.logout(logout -> logout.logoutUrl("/tpa/oauth2/logout").permitAll());
        }
        return http.build();
    }

    @Autowired(required = false)
    public void setoAuth2ClientProperties(OAuth2ClientProperties properties) {
        this.oAuth2ClientProperties = properties;
    }

    @Autowired
    public void setAuthenticationSuccessHandler(Oauth2AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Autowired
    public void setClientRegistrations(ClientRegistrationRepositoryImpl clientRegistrations) {
        this.clientRegistrations = clientRegistrations;
    }

    @Autowired
    public void setAuthenticationFailureHandler(Oauth2AuthenticationFailureHandler authenticationFailureHandler) {
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    @Autowired
    public void setJwtRequestAuthenticationFilter(JwtRequestAuthenticationFilter jwtRequestAuthenticationFilter) {
        this.jwtRequestAuthenticationFilter = jwtRequestAuthenticationFilter;
    }
}
