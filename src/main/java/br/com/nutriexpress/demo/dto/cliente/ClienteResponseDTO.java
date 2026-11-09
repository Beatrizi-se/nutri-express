package br.com.nutriexpress.demo.dto.cliente;

import br.com.nutriexpress.demo.model.Cliente;

public record ClienteResponseDTO(
    Long id,
    String nome,
    String email,
    String cpf,
    String telefone,
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
            cliente.getAtivo(),
            cliente.getEndereco() != null ? new EnderecoDTO(cliente.getEndereco()) : null
        );
    }
}
