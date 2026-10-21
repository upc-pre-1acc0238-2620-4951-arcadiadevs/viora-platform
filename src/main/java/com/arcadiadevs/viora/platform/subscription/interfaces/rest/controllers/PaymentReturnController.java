package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

/** Navigation-only landing page. Provider return parameters never authorize entitlements. */
@RestController
public class PaymentReturnController {
    /** @return safe static page that opens the Android status screen */
    @GetMapping(value = "/payments/return", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> paymentReturn() {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'")
            .body("""
                <!doctype html><html lang="es"><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Volver a Viora</title>
                <style>body{font:1.1rem system-ui;background:#faf9f4;color:#24382e;max-width:34rem;margin:15vh auto;padding:1.5rem}a{display:inline-block;padding:1rem;background:#24382e;color:white;border-radius:2rem}</style>
                <h1>Consulta tu pago en Viora</h1>
                <p>El regreso desde Mercado Pago no confirma por sí solo el pago. Viora consultará el resultado al abrirse.</p>
                <a href="viora://payments/return">Abrir Viora</a>
                <p>También puedes volver manualmente a la aplicación y consultar el estado.</p></html>
                """);
    }
}
