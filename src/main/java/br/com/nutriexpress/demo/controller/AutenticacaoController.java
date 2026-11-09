package br.com.nutriexpress.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.nutriexpress.demo.dto.auth.LoginRequestDTO;
import br.com.nutriexpress.demo.dto.auth.LoginResponseDTO;
import br.com.nutriexpress.demo.dto.auth.RegistroRequestDTO;
import br.com.nutriexpress.demo.dto.auth.UsuarioResponseDTO;
import br.com.nutriexpress.demo.exception.RegraNegocioException;
import br.com.nutriexpress.demo.model.UserRole;
import br.com.nutriexpress.demo.model.Usuario;
import br.com.nutriexpress.demo.repository.UsuarioRepository;
import br.com.nutriexpress.demo.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de login e registro de usuários com segurança JWT")
public class AutenticacaoController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoController(AuthenticationManager authenticationManager,
                                  UsuarioRepository usuarioRepository,
                                  TokenService tokenService,
                                  PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar login e obter token JWT")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        var authToken = new UsernamePasswordAuthenticationToken(dto.login(), dto.senha());
        Authentication authentication = authenticationManager.authenticate(authToken);

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String token = tokenService.gerarToken(usuario);

        return ResponseEntity.ok(new LoginResponseDTO(token, usuario.getLogin(), usuario.getRole().name()));
    }

    @PostMapping("/registrar")
    @Operation(summary = "Registrar um novo usuário no sistema")
    public ResponseEntity<UsuarioResponseDTO> registrar(@RequestBody @Valid RegistroRequestDTO dto) {
        if (usuarioRepository.existsByLogin(dto.login())) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o login: " + dto.login());
        }

        // Criptografa a senha com BCrypt antes de persistir
        String senhaCriptografada = passwordEncoder.encode(dto.senha());
        UserRole role = dto.role() != null ? dto.role() : UserRole.CLIENTE;

        Usuario novoUsuario = new Usuario(dto.login(), senhaCriptografada, role);
        Usuario salvo = usuarioRepository.save(novoUsuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioResponseDTO(salvo));
    }
}
