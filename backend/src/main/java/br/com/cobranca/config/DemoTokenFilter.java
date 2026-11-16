package br.com.cobranca.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * "Login" da demo: um token único no header X-Demo-Token. Não é autenticação de usuário
 * (não existe cadastro); só evita que qualquer pessoa na internet mexa nos dados da demo.
 * O webhook do Asaas e o health ficam de fora (o webhook tem o próprio token).
 */
@Component
public class DemoTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DemoTokenFilter.class);
    private final byte[] esperado;

    public DemoTokenFilter(AppProperties props) {
        String token = props.demoToken() == null ? "" : props.demoToken();
        this.esperado = token.getBytes(StandardCharsets.UTF_8);
        if (token.isBlank()) {
            log.warn("DEMO_TOKEN vazio: API ABERTA. Use apenas em desenvolvimento local.");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return esperado.length == 0
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !path.startsWith("/api/")
                || path.equals("/api/health")
                || path.startsWith("/api/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader("X-Demo-Token");
        byte[] bytes = recebido == null ? new byte[0] : recebido.getBytes(StandardCharsets.UTF_8);
        // Comparação em tempo constante (evita descobrir o token por diferença de tempo).
        if (MessageDigest.isEqual(esperado, bytes)) {
            chain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token de demo inválido");
        }
    }
}
