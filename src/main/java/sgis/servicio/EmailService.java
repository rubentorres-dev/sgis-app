package sgis.servicio;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Servicio de envío de correo electrónico (RF-08).
 * Usa SMTP a través de Jakarta Mail.
 *
 * IMPORTANTE: debes completar tus propias credenciales SMTP abajo antes
 * de poder enviar correos reales. Con Gmail, necesitas generar una
 * "contraseña de aplicación" (no tu contraseña normal de Gmail) en:
 * https://myaccount.google.com/apppasswords
 */
public class EmailService {

    // --- Completa esto con tus propios datos ---
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String CORREO_REMITENTE = "tu_correo_aqui@gmail.com";
    private static final String CONTRASENA_APP = "tu_contraseña_de_aplicacion_aqui";

    /**
     * Envía un correo con el comprobante de compra.
     * @return true si el envío fue exitoso, false si falló (sin lanzar excepción,
     *         para que un problema de correo/red no tumbe el registro de la venta).
     */
    public boolean enviarComprobante(String destinatario, String asunto, String cuerpo) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        // Tiempos límite: sin ellos, un servidor que no responde deja el envío esperando para siempre.
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(CORREO_REMITENTE, CONTRASENA_APP);
            }
        });

        try {
            Message mensaje = new MimeMessage(session);
            mensaje.setFrom(new InternetAddress(CORREO_REMITENTE));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            Transport.send(mensaje);
            return true;
        } catch (MessagingException e) {
            System.out.println("No se pudo enviar el correo: " + e.getMessage());
            return false;
        }
    }
}
