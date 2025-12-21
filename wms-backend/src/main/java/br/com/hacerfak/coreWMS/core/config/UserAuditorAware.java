package br.com.hacerfak.coreWMS.core.config;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.of("SISTEMA"); // Ou return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        // Se o principal for sua classe de Usuario customizada
        if (principal instanceof Usuario) {
            return Optional.of(((Usuario) principal).getLogin());
        }

        // Fallback para string (ex: token JWT puro)
        return Optional.of(authentication.getName());
    }
}