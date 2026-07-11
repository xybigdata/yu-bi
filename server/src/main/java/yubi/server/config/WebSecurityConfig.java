package yubi.server.config;

import yubi.security.manager.AuthenticationAssembler;
import yubi.security.manager.springsecurity.YuBiAuthenticationProvider;
import yubi.security.oauth2.ClientRegistrationRepositoryImpl;
import yubi.security.oauth2.CustomOAuth2AuthorizationRequestRedirectFilter;
import yubi.security.oauth2.CustomOauth2AuthenticationFilter;
import yubi.server.config.filter.JwtRequestAuthenticationFilter;
import yubi.server.config.security.YuBiAccessDeniedHandler;
import yubi.server.config.security.YuBiAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;

import static yubi.core.common.Application.getApiPrefix;

@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig {

    private OAuth2ClientProperties oAuth2ClientProperties;

    private Oauth2AuthenticationSuccessHandler authenticationSuccessHandler;

    private Oauth2AuthenticationFailureHandler authenticationFailureHandler;

    private ClientRegistrationRepositoryImpl clientRegistrations;

    private JwtRequestAuthenticationFilter jwtRequestAuthenticationFilter;

    private YuBiAuthenticationEntryPoint yubiAuthenticationEntryPoint;

    private YuBiAccessDeniedHandler yubiAccessDeniedHandler;

    private AuthenticationAssembler authenticationAssembler;

    @Bean
    public AuthenticationProvider yubiAuthenticationProvider() {
        return new YuBiAuthenticationProvider(authenticationAssembler);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authenticationProvider(yubiAuthenticationProvider());
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(getApiPrefix() + "/tpa/**").permitAll()
                .requestMatchers(getApiPrefix() + "/sys/info", getApiPrefix() + "/sys/setup").permitAll()
                .requestMatchers(getApiPrefix() + "/users/login", getApiPrefix() + "/users/register", getApiPrefix() + "/users/active").permitAll()
                .requestMatchers(getApiPrefix() + "/users/sendmail", getApiPrefix() + "/users/reset/password").permitAll()
                .requestMatchers(getApiPrefix() + "/users/forget/password").permitAll()
                .requestMatchers(getApiPrefix() + "/shares/**").permitAll()
                .requestMatchers("/shareChart/**", "/shareDashboard/**", "/shareStoryPlayer/**").permitAll()
                .requestMatchers("/confirminvite", "/organizations/**").permitAll()
                .requestMatchers("/", "/login", "/setup", "/register", "/activation", "/forgetPassword", "/authorization").permitAll()
                .requestMatchers("/static/**", "/custom-chart-plugins/**", "/antd/**").permitAll()
                // 浏览器 img 请求无法携带 Authorization 请求头，头像资源需要允许匿名读取。
                .requestMatchers("/resources/org/avatar/**", "/resources/user/avatar/**").permitAll()
                .requestMatchers("/**/*.html", "/**/*.js", "/**/*.css", "/**/*.ico", "/**/*.png", "/**/*.svg", "/**/*.json").permitAll()
                .anyRequest().authenticated());
        http.addFilterBefore(jwtRequestAuthenticationFilter, OAuth2AuthorizationRequestRedirectFilter.class);
        if (this.oAuth2ClientProperties != null) {
            http.addFilterBefore(new CustomOAuth2AuthorizationRequestRedirectFilter(clientRegistrations), OAuth2AuthorizationRequestRedirectFilter.class);
            http.addFilterBefore(new CustomOauth2AuthenticationFilter(clientRegistrations, authenticationSuccessHandler), OAuth2LoginAuthenticationFilter.class);
            http.oauth2Login(oauth2Login -> oauth2Login
                    .failureHandler(authenticationFailureHandler)
                    .clientRegistrationRepository(clientRegistrations)
                    .successHandler(authenticationSuccessHandler)
                    .loginPage("/"));
            http.logout(logout -> logout.logoutUrl("/tpa/oauth2/logout").permitAll());
        }
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(yubiAuthenticationEntryPoint)
                .accessDeniedHandler(yubiAccessDeniedHandler));
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

    @Autowired
    public void setYuBiAuthenticationEntryPoint(YuBiAuthenticationEntryPoint yubiAuthenticationEntryPoint) {
        this.yubiAuthenticationEntryPoint = yubiAuthenticationEntryPoint;
    }

    @Autowired
    public void setYuBiAccessDeniedHandler(YuBiAccessDeniedHandler yubiAccessDeniedHandler) {
        this.yubiAccessDeniedHandler = yubiAccessDeniedHandler;
    }

    @Autowired
    public void setAuthenticationAssembler(AuthenticationAssembler authenticationAssembler) {
        this.authenticationAssembler = authenticationAssembler;
    }
}
