"""Validadores simples reutilizables."""

import re


def validar_password(password):
    """
    Reglas mínimas para una contraseña definida por el usuario.
    Devuelve (es_valida: bool, mensaje: str).
    """
    if not password or len(password) < 6:
        return False, 'Debe tener al menos 6 caracteres'
    if not any(c.isalpha() for c in password):
        return False, 'Debe incluir al menos una letra'
    if not any(c.isdigit() for c in password):
        return False, 'Debe incluir al menos un número'
    return True, 'OK'


def validar_username(username):
    if not username or len(username) < 3:
        return False, 'El usuario debe tener al menos 3 caracteres'
    if not re.match(r'^[a-zA-Z0-9_.]+$', username):
        return False, 'Solo letras, números, punto y guion bajo'
    return True, 'OK'


def limpiar_correo(correo):
    """Normaliza un correo. Devuelve '' si viene vacío y None si no es válido."""
    correo = (correo or '').strip().lower()
    if not correo:
        return ''
    if len(correo) > 160 or not re.fullmatch(r'[^@\s]+@[^@\s]+\.[^@\s]+', correo):
        return None
    return correo


def limpiar_celular(celular):
    """Deja solo dígitos y el signo +."""
    if not celular:
        return ''
    return re.sub(r'[^\d+]', '', celular)
