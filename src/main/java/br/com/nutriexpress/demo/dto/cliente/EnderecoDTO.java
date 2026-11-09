package br.com.nutriexpress.demo.dto.cliente;

import br.com.nutriexpress.demo.model.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EnderecoDTO(
    @NotBlank(message = "O logradouro é obrigatório")
    String logradouro,

    @NotBlank(message = "O número é obrigatório")
    String numero,

    String complemento,

    @NotBlank(message = "O bairro é obrigatório")
    String bairro,

    @NotBlank(message = "A cidade é obrigatória")
    String cidade,

    @NotBlank(message = "O CEP é obrigatório")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "O CEP deve estar no formato 00000-000 ou 00000000")
    String cep
) {
    public EnderecoDTO(Endereco endereco) {
        this(
            endereco != null ? endereco.getLogradouro() : null,
            endereco != null ? endereco.getNumero() : null,
            endereco != null ? endereco.getComplemento() : null,
            endereco != null ? endereco.getBairro() : null,
            endereco != null ? endereco.getCidade() : null,
            endereco != null ? endereco.getCep() : null
        );
    }

    public Endereco toEntity() {
        return new Endereco(logradouro, numero, complemento, bairro, cidade, cep);
    }
}
