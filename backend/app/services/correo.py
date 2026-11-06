"""
Envío de correo. Dos vías, según lo que esté configurado:
- BREVO_API_KEY: API de Brevo por HTTPS. Funciona en cualquier hosting.
- SMTP_HOST: SMTP clásico. Puerto 465 con SSL; cualquier otro con STARTTLS.
"""

import json
import smtplib
import ssl
import urllib.request
from email.message import EmailMessage
from flask import current_app

NOMBRE_REMITENTE = 'Zona Aquática'
TIMEOUT_SEGUNDOS = 20


def correo_configurado():
    cfg = current_app.config
    return bool(cfg['CORREO_REMITENTE'] and (cfg['BREVO_API_KEY'] or cfg['SMTP_HOST']))


def enviar_correo(destino, asunto, texto):
    """Envía un correo de texto. Lanza una excepción si no se pudo entregar al proveedor."""
    if current_app.config['BREVO_API_KEY']:
        _enviar_brevo(destino, asunto, texto)
    else:
        _enviar_smtp(destino, asunto, texto)


def _enviar_brevo(destino, asunto, texto):
    cfg = current_app.config
    cuerpo = {
        'sender': {'name': NOMBRE_REMITENTE, 'email': cfg['CORREO_REMITENTE']},
        'to': [{'email': destino}],
        'subject': asunto,
        'textContent': texto,
    }
    peticion = urllib.request.Request(
        'https://api.brevo.com/v3/smtp/email',
        data=json.dumps(cuerpo).encode('utf-8'),
        headers={
            'api-key': cfg['BREVO_API_KEY'],
            'content-type': 'application/json',
            'accept': 'application/json',
        },
        method='POST',
    )
    # urlopen lanza HTTPError ante cualquier respuesta 4xx/5xx.
    with urllib.request.urlopen(peticion, timeout=TIMEOUT_SEGUNDOS):
        pass


def _enviar_smtp(destino, asunto, texto):
    cfg = current_app.config
    mensaje = EmailMessage()
    mensaje['From'] = f"{NOMBRE_REMITENTE} <{cfg['CORREO_REMITENTE']}>"
    mensaje['To'] = destino
    mensaje['Subject'] = asunto
    mensaje.set_content(texto)

    contexto = ssl.create_default_context()
    if cfg['SMTP_PORT'] == 465:
        servidor = smtplib.SMTP_SSL(cfg['SMTP_HOST'], 465, timeout=TIMEOUT_SEGUNDOS, context=contexto)
    else:
        servidor = smtplib.SMTP(cfg['SMTP_HOST'], cfg['SMTP_PORT'], timeout=TIMEOUT_SEGUNDOS)
        servidor.starttls(context=contexto)
    with servidor:
        if cfg['SMTP_USUARIO']:
            servidor.login(cfg['SMTP_USUARIO'], cfg['SMTP_PASSWORD'])
        servidor.send_message(mensaje)
