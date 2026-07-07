package com.dragenda.infrastructure;

import com.dragenda.domain.entities.Usuario;
import com.dragenda.domain.enums.PerfilUsuario;
import com.dragenda.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdminSeedConfigIT {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedCriaAdminComCamposCorretos() {
        Optional<Usuario> adminOpt = usuarioRepository.findByEmail("admin@agenda.com");

        assertThat(adminOpt).isPresent();
        Usuario admin = adminOpt.get();

        assertThat(admin.getNome()).isEqualTo("Admin Sistema");
        assertThat(admin.getPerfil()).isEqualTo(PerfilUsuario.ADMIN_SISTEMA);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.isSenhaTemporaria()).isTrue();  // AC-3: força troca no primeiro login
        assertThat(admin.getEmpresa()).isNull();          // AC-3: Admin Sistema sem empresa
    }

    @Test
    void seedArmazenaSenhaComHashBCrypt() {
        Usuario admin = usuarioRepository.findByEmail("admin@agenda.com").orElseThrow();

        // AC-3: senha nunca em plain text
        assertThat(admin.getSenhaHash()).isNotEqualTo("TestPassword123!");
        // AC-3: hash verificável via BCrypt
        assertThat(passwordEncoder.matches("TestPassword123!", admin.getSenhaHash())).isTrue();
    }

    @Test
    void seedEIdempotente() {
        long countAntes = usuarioRepository.count();

        // Re-execução simulada: tentar criar novamente
        boolean jaExiste = usuarioRepository.existsByEmail("admin@agenda.com");
        assertThat(jaExiste).isTrue();

        long countDepois = usuarioRepository.count();
        // AC-3: segunda execução não cria duplicata
        assertThat(countDepois).isEqualTo(countAntes);
    }
}
