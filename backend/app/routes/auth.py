"""Autenticación — login por nombre de usuario, cambio de contraseña, sesión."""

from datetime import datetime
from flask import Blueprint, request, jsonify
from flask_jwt_extended import (
    create_access_token,
    create_refresh_token,
    jwt_required,
    get_jwt_identity,
    get_jwt,
)

from app.extensions import db
from app.models.usuario import Usuario
from app.utils.validators import validar_password
from app.utils.helpers import registrar_actividad

auth_bp = Blueprint('auth', __name__)


@auth_bp.route('/login', methods=['POST'])
def login():
    """Login con nombre de usuario y contraseña."""
    try:
        data = request.get_json() or {}
        username = (data.get('username') or '').strip().lower()
        password = data.get('password') or ''

        if not username or not password:
            return jsonify({'error': 'Usuario y contraseña son requeridos'}), 400

        usuario = Usuario.query.filter(db.func.lower(Usuario.username) == username).first()
        if not usuario:
            return jsonify({'error': 'Credenciales inválidas'}), 401
        if not usuario.activo:
            return jsonify({'error': 'Usuario inactivo. Contacta al administrador'}), 403
        if usuario.esta_bloqueado():
            return jsonify({'error': 'Cuenta bloqueada temporalmente por intentos fallidos'}), 403

        if not usuario.check_password(password):
            usuario.registrar_intento_fallido()
            db.session.commit()
            return jsonify({'error': 'Credenciales inválidas'}), 401

        usuario.resetear_intentos()
        usuario.ultimo_login = datetime.utcnow()

        claims = {'rol': usuario.rol}
        access_token = create_access_token(identity=str(usuario.id), additional_claims=claims)
        refresh_token = create_refresh_token(identity=str(usuario.id), additional_claims=claims)

        registrar_actividad('login', 'auth', f'Inició sesión: {usuario.username}', usuario=usuario)
        db.session.commit()

        return jsonify({
            'access_token': access_token,
            'refresh_token': refresh_token,
            'usuario': usuario.to_dict(),
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error en login: {str(e)}'}), 500


@auth_bp.route('/refresh', methods=['POST'])
@jwt_required(refresh=True)
def refresh():
    """Renueva el access token usando el refresh token."""
    try:
        identity = get_jwt_identity()
        claims = get_jwt()
        access_token = create_access_token(
            identity=identity,
            additional_claims={'rol': claims.get('rol')},
        )
        return jsonify({'access_token': access_token}), 200
    except Exception as e:
        return jsonify({'error': f'Error al renovar token: {str(e)}'}), 500


@auth_bp.route('/me', methods=['GET'])
@jwt_required()
def me():
    """Datos del usuario autenticado."""
    uid = get_jwt_identity()
    usuario = Usuario.query.get(int(uid)) if uid is not None else None
    if not usuario:
        return jsonify({'error': 'Usuario no encontrado'}), 404
    return jsonify(usuario.to_dict()), 200


@auth_bp.route('/cambiar-password', methods=['POST'])
@jwt_required()
def cambiar_password():
    """Cambia la contraseña del usuario actual (incluye la primera vez tras el reset)."""
    try:
        data = request.get_json() or {}
        password_actual = data.get('password_actual') or ''
        password_nueva = data.get('password_nueva') or ''

        if not password_actual or not password_nueva:
            return jsonify({'error': 'Contraseña actual y nueva son requeridas'}), 400

        uid = get_jwt_identity()
        usuario = Usuario.query.get(int(uid))
        if not usuario:
            return jsonify({'error': 'Usuario no encontrado'}), 404

        if not usuario.check_password(password_actual):
            return jsonify({'error': 'La contraseña actual es incorrecta'}), 401

        es_valida, mensaje = validar_password(password_nueva)
        if not es_valida:
            return jsonify({'error': f'Contraseña inválida: {mensaje}'}), 400

        usuario.set_password(password_nueva)
        usuario.es_password_temporal = False

        registrar_actividad('editar', 'auth', 'Cambió su contraseña', usuario=usuario)
        db.session.commit()

        return jsonify({'mensaje': 'Contraseña actualizada correctamente'}), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al cambiar contraseña: {str(e)}'}), 500


@auth_bp.route('/logout', methods=['POST'])
@jwt_required()
def logout():
    """Cierre de sesión (registro de actividad; el token se descarta en el cliente)."""
    try:
        uid = get_jwt_identity()
        usuario = Usuario.query.get(int(uid)) if uid is not None else None
        registrar_actividad('logout', 'auth', 'Cerró sesión', usuario=usuario)
        db.session.commit()
        return jsonify({'mensaje': 'Sesión cerrada'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error en logout: {str(e)}'}), 500
