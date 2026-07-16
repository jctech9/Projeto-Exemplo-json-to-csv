package com.example.demo.config;

public class InvalidApiDestinationException extends RuntimeException {

    public InvalidApiDestinationException() {
        super("Destino da API invalido.");
    }

    public InvalidApiDestinationException(Throwable cause) {
        super("Destino da API invalido.", cause);
    }
}
