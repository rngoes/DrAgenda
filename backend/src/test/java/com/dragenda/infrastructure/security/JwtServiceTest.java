package com.dragenda.infrastructure.security;

import com.dragenda.domain.entities.Empresa;
import com.dragenda.domain.entities.Usuario;
import com.dragenda.domain.enums.PerfilUsuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
        "dGVzdC1zZWNyZXQtZm9yLXRlc3RpbmctcHVycG9zZXMtb25seS0zMmJ5dGVz";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);
    }

    @Test
    void gerarToken_extrairClaims_retornaValoresCorretos() {
        Usuario usuario = criarUsuario();

        String token = jwtService.gerarToken(usuario);
        Claims claims = jwtService.extrairClaims(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(jwtService.extrairEmpresaId(token)).isEqualTo(10L);
        assertThat(jwtService.extrairPerfil(token)).isEqualTo("STAFF");
        assertThat(jwtService.extrairUserId(token)).isEqualTo(1L);
    }

    @Test
    void gerarToken_adminSistema_empresaIdNull() {
        Usuario admin = new Usuario();
        admin.setId(99L);
        admin.setNome("Admin");
        admin.setPerfil(PerfilUsuario.ADMIN_SISTEMA);
        admin.setEmpresa(null);
        admin.setSenhaTemporaria(false);

        String token = jwtService.gerarToken(admin);

        assertThat(jwtService.extrairEmpresaId(token)).isNull();
    }

    @Test
    void isTokenValido_tokenValido_retornaTrue() {
        String token = jwtService.gerarToken(criarUsuario());
        assertThat(jwtService.isTokenValido(token)).isTrue();
    }

    @Test
    void isTokenValido_tokenInvalido_retornaFalse() {
        assertThat(jwtService.isTokenValido("token.invalido.aqui")).isFalse();
    }

    @Test
    void isTokenValido_tokenExpirado_retornaFalse() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String tokenExpirado = jwtService.gerarToken(criarUsuario());
        assertThat(jwtService.isTokenValido(tokenExpirado)).isFalse();
    }

    private Usuario criarUsuario() {
        Empresa empresa = new Empresa();
        empresa.setId(10L);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Maria");
        usuario.setPerfil(PerfilUsuario.STAFF);
        usuario.setEmpresa(empresa);
        usuario.setSenhaTemporaria(false);
        return usuario;
    }
}
