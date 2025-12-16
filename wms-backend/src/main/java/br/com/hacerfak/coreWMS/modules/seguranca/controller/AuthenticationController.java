package br.com.hacerfak.coreWMS.modules.seguranca.controller;

//import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole; // Importante se for validar role manualmente
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.AuthenticationDTO; // <--- Import Novo
import br.com.hacerfak.coreWMS.modules.seguranca.dto.LoginResponseDTO; // <--- Import Novo
import br.com.hacerfak.coreWMS.modules.seguranca.dto.RegisterDTO; // <--- Import Novo
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((Usuario) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        if (this.repository.findByLogin(data.login()) != null)
            return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        Usuario newUser = Usuario.builder()
                .login(data.login())
                .senha(encryptedPassword)
                .role(data.role())
                .build();

        this.repository.save(newUser);

        return ResponseEntity.ok().build();
    }
}