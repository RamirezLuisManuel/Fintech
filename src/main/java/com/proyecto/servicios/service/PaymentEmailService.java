package com.proyecto.servicios.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@fintech.local}")
    private String from;

    @Value("${payments.otp.expiration-minutes:5}")
    private int expirationMinutes;

    public void sendOtp(String recipient, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Codigo de autorizacion de pago");
        message.setText("Tu codigo para autorizar el pago es: " + code
                + "\n\nEl codigo expira en " + expirationMinutes
                + " minutos. Si no solicitaste este pago, no lo compartas.");
        mailSender.send(message);
    }
}
