package pl.cwup.travelonthegravel.common.handler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cwup.travelonthegravel.common.exception.ConflictException;
import pl.cwup.travelonthegravel.common.exception.ExternalServiceException;
import pl.cwup.travelonthegravel.common.exception.NotFoundException;

/** Kontroler tylko do testów — każdy endpoint rzuca inny błąd. */
@RestController
@RequestMapping("/test/errors")
final class ErrorTestController {

    record TestRequest(@NotBlank String name, @Email String email) {}

    @GetMapping("/not-found")
    void notFound() {
        throw new NotFoundException("Nie znaleziono podróży");
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new ConflictException("E-mail jest już zajęty");
    }

    @GetMapping("/external")
    void external() {
        throw new ExternalServiceException("Usługa map jest niedostępna");
    }

    @GetMapping("/forbidden")
    void forbidden() {
        throw new AccessDeniedException("Brak dostępu");
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("Szczegóły techniczne, których nie wolno pokazać");
    }

    @GetMapping("/number/{id}")
    void number(@PathVariable Long id) {}

    @PostMapping("/validation")
    void validation(@Valid @RequestBody TestRequest request) {}
}
