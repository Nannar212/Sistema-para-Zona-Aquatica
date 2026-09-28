"""Decoradores de autorización."""

from functools import wraps
from flask import jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app.models.usuario import Usuario


def rol_requerido(*roles):
    """
    Verifica el JWT y que el rol del usuario esté entre los permitidos.
    Uso: @rol_requerido('admin') o @rol_requerido('admin', 'recepcionista')
    """
    def decorator(fn):
        @wraps(fn)
        @jwt_required()
        def wrapper(*args, **kwargs):
            uid = get_jwt_identity()
            usuario = Usuario.query.get(int(uid)) if uid is not None else None

            if not usuario:
                return jsonify({'error': 'Usuario no encontrado'}), 404
            if not usuario.activo:
                return jsonify({'error': 'Usuario inactivo'}), 403
            if roles and usuario.rol not in roles:
                return jsonify({'error': 'Permiso denegado'}), 403

            return fn(*args, **kwargs)
        return wrapper
    return decorator
