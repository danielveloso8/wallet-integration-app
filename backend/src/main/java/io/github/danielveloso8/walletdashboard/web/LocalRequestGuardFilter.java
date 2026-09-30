package io.github.danielveloso8.walletdashboard.web;

import io.github.danielveloso8.walletdashboard.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalRequestGuardFilter extends OncePerRequestFilter {
    private final AppProperties properties;

    public LocalRequestGuardFilter(AppProperties properties) { this.properties = properties; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        List<String> hosts = properties.security().allowedHosts();
        if (hosts == null || !hosts.contains(request.getHeader("Host"))) {
            response.sendError(421);
            return;
        }
        String origin = request.getHeader("Origin");
        List<String> origins = properties.security().allowedOrigins();
        if (origin != null && (origins == null || !origins.contains(origin))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String method = request.getMethod();
        if (request.getRequestURI().startsWith("/api/")
                && List.of("POST", "PUT", "PATCH", "DELETE").contains(method)
                && !"wallet-dashboard".equals(request.getHeader("X-Requested-With"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
