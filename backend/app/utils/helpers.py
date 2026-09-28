"""Utilidades compartidas del backend."""

import secrets
import string
from flask import request
from flask_jwt_extended import get_jwt_identity
from app.extensions import db
from app.models.usuario import Usuario
from app.models.auditoria import Auditoria


def generar_password_temporal(longitud=10):
    """
    Genera una contraseña temporal aleatoria y legible.
    Garantiza al menos una mayúscula, una minúscula, un dígito y un símbolo.
    """
    alfabeto = string.ascii_letters + string.digits + '!@#$%'
    while True:
        pwd = ''.join(secrets.choice(alfabeto) for _ in range(longitud))
        if (any(c.islower() for c in pwd)
                and any(c.isupper() for c in pwd)
                and any(c.isdigit() for c in pwd)):
            return pwd


def obtener_usuario_actual():
    """Devuelve el objeto Usuario correspondiente al JWT actual (o None)."""
    try:
        uid = get_jwt_identity()
        if uid is None:
            return None
        return Usuario.query.get(int(uid))
    except (ValueError, TypeError):
        return None


def registrar_actividad(accion, modulo, descripcion='', usuario=None, commit=False):
    """
    Registra una acción en la tabla de auditoría (registro de actividad).
    No hace commit por defecto: se guarda junto con la transacción de la ruta.
    """
    if usuario is None:
        usuario = obtener_usuario_actual()

    registro = Auditoria(
        usuario_id=usuario.id if usuario else None,
        nombre_usuario=usuario.nombre_completo if usuario else 'Sistema',
        rol_usuario=usuario.rol if usuario else None,
        accion=accion,
        modulo=modulo,
        descripcion=descripcion,
        ip_address=request.remote_addr if request else None,
    )
    db.session.add(registro)
    if commit:
        db.session.commit()
    return registro
