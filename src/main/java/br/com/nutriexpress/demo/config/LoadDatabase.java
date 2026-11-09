package br.com.nutriexpress.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.nutriexpress.demo.model.Categoria;
import br.com.nutriexpress.demo.model.UserRole;
import br.com.nutriexpress.demo.model.Usuario;
import br.com.nutriexpress.demo.repository.CategoriaRepository;
import br.com.nutriexpress.demo.repository.UsuarioRepository;

@Configuration
public class LoadDatabase {
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(CategoriaRepository categoriaRepository,
                                  UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed de categorias iniciais
            if (categoriaRepository.count() == 0) {
                Categoria categoria1 = new Categoria();
                categoria1.setNome("Pratos Executivos");
                categoria1.setDescricao("Refeicoes completas e balanceadas para o dia a dia");

                Categoria categoria2 = new Categoria();
                categoria2.setNome("Sucos Naturais");
                categoria2.setDescricao("Bebidas funcionais prensadas a frio");

                log.info("Preloading " + categoriaRepository.save(categoria1));
                log.info("Preloading " + categoriaRepository.save(categoria2));
            }

            // Seed de usuario administrador padrao
            if (!usuarioRepository.existsByLogin("admin")) {
                Usuario admin = new Usuario("admin", passwordEncoder.encode("admin123"), UserRole.ADMIN);
                usuarioRepository.save(admin);
                log.info("Usuario ADMIN padrao criado: login='admin', senha='admin123'");
            }
        };
    }
}
