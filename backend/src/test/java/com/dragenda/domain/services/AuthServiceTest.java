package com.dragenda.domain.services;

import com.dragenda.api.dtos.request.LoginRequest;
import com.dragenda.domain.entities.Usuario;
import com.dragenda.domain.enums.PerfilUsuario;
import com.dragenda.domain.repositories.UsuarioRepository;
import com.dragenda.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private Usuario usuarioAtivo;

    @BeforeEach
    void setUp() {
        usuarioAtivo = new Usuario();
        usuarioAtivo.setId(1L);
        usuarioAtivo.setEmail("staff@clinica.com");
        usuarioAtivo.setNome("Maria");
        usuarioAtivo.setSenhaHash("$bcrypt$hash");
        usuarioAtivo.setPerfil(PerfilUsuario.STAFF);
        usuarioAtivo.setAtivo(true);
        usuarioAtivo.setSenhaTemporaria(false);
    }

    @Test
    void login_credenciaisValidas_retornaToken() {
        when(usuarioRepository.findByEmail("staff@clinica.com")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("senha123", "$bcrypt$hash")).thenReturn(true);
        when(jwtService.gerarToken(any())).thenReturn("jwt.token.aqui");

        var response = authService.login(new LoginRequest("staff@clinica.com", "senha123"));

        assertThat(response.token()).isEqualTo("jwt.token.aqui");
        assertThat(response.perfil()).isEqualTo("STAFF");
        assertThat(response.nome()).isEqualTo("Maria");
        assertThat(response.senhaTemporaria()).isFalse();
    }

    @Test
    void login_emailInexistente_lancaCredenciaisInvalidas() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nao@existe.com", "senha")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Credenciais inválidas");
    }

    @Test
    void login_senhaErrada_lancaCredenciaisInvalidas() {
        when(usuarioRepository.findByEmail("staff@clinica.com")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("senhaErrada", "$bcrypt$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("staff@clinica.com", "senhaErrada")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Credenciais inválidas");
    }

    @Test
    void login_contaInativa_lancaCredenciaisInvalidas() {
        usuarioAtivo.setAtivo(false);
        when(usuarioRepository.findByEmail("staff@clinica.com")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("staff@clinica.com", "senha123")))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Credenciais inválidas");
    }

    @Test
    void login_mensagemIdentica_emailInexistenteVsSenhaErrada() {
        // Proteção contra user enumeration — mesma mensagem em ambos os casos
        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("staff@clinica.com")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("senhaErrada", "$bcrypt$hash")).thenReturn(false);

        String msgEmailInexistente = null;
        String msgSenhaErrada = null;

        try { authService.login(new LoginRequest("nao@existe.com", "qualquer")); }
        catch (BadCredentialsException e) { msgEmailInexistente = e.getMessage(); }

        try { authService.login(new LoginRequest("staff@clinica.com", "senhaErrada")); }
        catch (BadCredentialsException e) { msgSenhaErrada = e.getMessage(); }

        assertThat(msgEmailInexistente).isEqualTo(msgSenhaErrada);
    }
}
