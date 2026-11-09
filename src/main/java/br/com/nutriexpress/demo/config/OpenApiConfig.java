package br.com.nutriexpress.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Configuração do Swagger / OpenAPI para documentação interativa da API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nutri Express API")
                        .version("1.0.0")
                        .description("API REST para Delivery de Alimentação Saudável - Cardápio, Clientes, Pedidos e Estoque.")
                        .contact(new Contact()
                                .name("Bia - Nutri Express")
                                .email("suporte@nutriexpress.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
