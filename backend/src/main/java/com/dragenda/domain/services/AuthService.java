package com.dragenda.domain.services;

import com.dragenda.api.dtos.request.LoginRequest;
import com.dragenda.api.dtos.response.LoginResponse;
import com.dragenda.domain.repositories.UsuarioRepository;
import com.dragenda.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final String CREDENCIAIS_INVALIDAS = "Credenciais inválidas";

    public LoginResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException(CREDENCIAIS_INVALIDAS));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        if (!usuario.isAtivo()) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        String token = jwtService.gerarToken(usuario);

        return new LoginResponse(
            token,
            usuario.getPerfil().name(),
            usuario.getNome(),
            usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null,
            usuario.isSenhaTemporaria()
        );
    }
}
