package br.com.nutriexpress.demo.dto.cliente;

import java.time.LocalDate;

import br.com.nutriexpress.demo.model.Cliente;

public record ClienteResponseDTO(
    Long id,
    String nome,
    String email,
    String cpf,
    String telefone,
    LocalDate dataNascimento,
    Boolean ativo,
    EnderecoDTO endereco
) {
    public ClienteResponseDTO(Cliente cliente) {
        this(
            cliente.getId(),
            cliente.getNome(),
            cliente.getEmail(),
            cliente.getCpf(),
            cliente.getTelefone(),
            cliente.getDataNascimento(),
            cliente.getAtivo(),
            cliente.getEndereco() != null ? new EnderecoDTO(cliente.getEndereco()) : null
        );
    }
}
