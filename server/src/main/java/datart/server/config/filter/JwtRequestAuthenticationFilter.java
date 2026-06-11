package datart.server.config.filter;

import datart.core.base.consts.Const;
import datart.security.manager.DatartSecurityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestAuthenticationFilter extends OncePerRequestFilter {

    private final DatartSecurityManager securityManager;

    public JwtRequestAuthenticationFilter(DatartSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(Const.TOKEN);
        if (token != null && !securityManager.isAuthenticated()) {
            token = securityManager.login(token);
            response.setHeader(Const.TOKEN, token);
        }
        filterChain.doFilter(request, response);
    }
}
