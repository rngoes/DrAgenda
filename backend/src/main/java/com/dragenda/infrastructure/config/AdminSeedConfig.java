package com.dragenda.infrastructure.config;

import com.dragenda.domain.entities.Usuario;
import com.dragenda.domain.enums.PerfilUsuario;
import com.dragenda.domain.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminSeedConfig implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.password}") // sem default — falha explícita se ausente (AC-4)
    private String adminSeedPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByEmail("admin@agenda.com")) {
            log.debug("Admin seed: admin@agenda.com já existe, pulando criação.");
            return; // idempotente
        }

        Usuario admin = new Usuario();
        admin.setEmail("admin@agenda.com");
        admin.setNome("Admin Sistema");
        admin.setSenhaHash(passwordEncoder.encode(adminSeedPassword));
        admin.setPerfil(PerfilUsuario.ADMIN_SISTEMA);
        admin.setAtivo(true);
        admin.setSenhaTemporaria(true);  // força troca no primeiro login (FR-044)
        admin.setEmpresa(null);          // Admin Sistema não pertence a nenhuma empresa

        usuarioRepository.save(admin);
        log.info("Admin seed: admin@agenda.com criado com senha temporária.");
    }
}
