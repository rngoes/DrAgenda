package com.dragenda.infrastructure.security;

import com.dragenda.domain.repositories.EmpresaRepository;
import com.dragenda.domain.repositories.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValido(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Long userId = jwtService.extrairUserId(token);
        Long empresaId = jwtService.extrairEmpresaId(token);
        String perfil = jwtService.extrairPerfil(token);

        // Verificar empresa ativa (se não for ADMIN_SISTEMA)
        if (empresaId != null) {
            var empresa = empresaRepository.findById(empresaId).orElse(null);
            if (empresa == null || !empresa.isAtivo()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // Verificar usuário ativo
        var usuario = usuarioRepository.findById(userId).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Verificar senha temporária — bloquear todas as rotas exceto auth/** e trocar-senha
        if (usuario.isSenhaTemporaria()
                && !request.getRequestURI().startsWith("/api/v1/auth/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"title\":\"Troca de senha obrigatória antes de prosseguir\",\"status\":403}"
            );
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
            userId, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + perfil))
        );
        auth.setDetails(Map.of(
            "empresaId", empresaId != null ? empresaId : 0L,
            "perfil", perfil
        ));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
