package br.com.hacerfak.coreWMS.modules.seguranca.controller;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.AuthenticationDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.LoginResponseDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.RegisterDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.SelecaoEmpresaDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor // Usando Lombok para injeção limpa
public class AuthenticationController {

    private final AuthService authService;
    private final UsuarioRepository repository; // Mantive para o register simples

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var response = authService.login(data);
        return ResponseEntity.ok(response);
    }

    // --- NOVO ENDPOINT ---
    @PostMapping("/selecionar-empresa")
    public ResponseEntity<LoginResponseDTO> selecionarEmpresa(@RequestBody @Valid SelecaoEmpresaDTO data) {
        var response = authService.selecionarEmpresa(data.tenantId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        if (this.repository.findByLogin(data.login()).isPresent()) // Mudou para isPresent() por causa do Optional
            return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        // Nota: O register simples não cria vínculos com empresas ainda.
        // Isso será feito no Onboarding (Upload de Certificado).
        Usuario newUser = new Usuario();
        newUser.setLogin(data.login());
        newUser.setSenha(encryptedPassword);
        // newUser.setRole(data.role()); // Se tiver mantido o role simples

        this.repository.save(newUser);

        return ResponseEntity.ok().build();
    }
}