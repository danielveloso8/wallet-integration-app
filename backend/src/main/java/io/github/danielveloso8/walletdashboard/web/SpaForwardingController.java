package io.github.danielveloso8.walletdashboard.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SpaForwardingController {
    @GetMapping(value = "/{path:[^\\.]*}")
    public String forwardRoot(HttpServletRequest request) { return forward(request); }

    @GetMapping(value = "/**/{path:[^\\.]*}")
    public String forwardNested(HttpServletRequest request) { return forward(request); }

    private String forward(HttpServletRequest request) {
        if (request.getRequestURI().equals("/api") || request.getRequestURI().startsWith("/api/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
