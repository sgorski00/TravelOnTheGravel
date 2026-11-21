package pl.cwup.travelonthegravel.auth.event;

public record PasswordResetRequestedEvent(String email, String token) {}
