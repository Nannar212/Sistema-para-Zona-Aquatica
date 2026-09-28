"""
Personal — gestión de usuarios del sistema (admin y recepcionista).
Solo el administrador accede a este módulo.

Seguridad de contraseñas:
- Al crear un usuario se genera una contraseña temporal aleatoria y encriptada (bcrypt).
- Esa contraseña se muestra UNA sola vez al admin para que se la entregue al usuario.
- El usuario la cambia obligatoriamente en su primer ingreso.
- Si un usuario la olvida, el admin la "restablece": se genera una nueva temporal.
  (No se puede "ver" la contraseña anterior porque bcrypt es irreversible; esa es
   justamente la propiedad que mantiene el sistema seguro.)
"""

from flask import Blueprint, request, jsonify
from flask_jwt_extended import get_jwt_identity
from app.extensions import db
from app.models.usuario import Usuario, ROLES_VALIDOS
from app.utils.decorators import rol_requerido
from app.utils.helpers import generar_password_temporal, registrar_actividad
from app.utils.validators import validar_username

personal_bp = Blueprint('personal', __name__)


@personal_bp.route('', methods=['GET'])
@rol_requerido('admin')
def listar():
    incluir_inactivos = request.args.get('inactivos', 'false').lower() == 'true'
    query = Usuario.query
    if not incluir_inactivos:
        query = query.filter_by(activo=True)
    usuarios = query.order_by(Usuario.created_at.desc()).all()
    return jsonify([u.to_dict() for u in usuarios]), 200


@personal_bp.route('/<int:id>', methods=['GET'])
@rol_requerido('admin')
def obtener(id):
    usuario = Usuario.query.get(id)
    if not usuario:
        return jsonify({'error': 'Usuario no encontrado'}), 404
    return jsonify(usuario.to_dict()), 200


@personal_bp.route('', methods=['POST'])
@rol_requerido('admin')
def crear():
    try:
        data = request.get_json() or {}
        nombres = (data.get('nombres') or '').strip()
        apellidos = (data.get('apellidos') or '').strip()
        ci = (data.get('ci') or '').strip()
        username = (data.get('username') or '').strip().lower()
        rol = (data.get('rol') or 'recepcionista').strip()
        edad = data.get('edad')

        if not nombres or not apellidos or not ci or not username:
            return jsonify({'error': 'Nombres, apellidos, CI y usuario son requeridos'}), 400

        ok, msg = validar_username(username)
        if not ok:
            return jsonify({'error': msg}), 400
        if rol not in ROLES_VALIDOS:
            return jsonify({'error': 'Rol inválido'}), 400
        if Usuario.query.filter_by(ci=ci).first():
            return jsonify({'error': 'Ya existe un usuario con ese CI'}), 409
        if Usuario.query.filter(db.func.lower(Usuario.username) == username).first():
            return jsonify({'error': 'Ese nombre de usuario ya está en uso'}), 409

        password_temporal = generar_password_temporal()
        usuario = Usuario(
            nombres=nombres,
            apellidos=apellidos,
            ci=ci,
            username=username,
            edad=edad,
            rol=rol,
            es_password_temporal=True,
            activo=True,
        )
        usuario.set_password(password_temporal)
        db.session.add(usuario)

        registrar_actividad('crear', 'personal', f'Creó al usuario {username} ({rol})')
        db.session.commit()

        # La contraseña temporal se devuelve UNA vez para entregarla al nuevo usuario.
        return jsonify({
            'mensaje': 'Usuario creado correctamente',
            'usuario': usuario.to_dict(),
            'password_temporal': password_temporal,
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al crear usuario: {str(e)}'}), 500


@personal_bp.route('/<int:id>', methods=['PUT'])
@rol_requerido('admin')
def actualizar(id):
    try:
        usuario = Usuario.query.get(id)
        if not usuario:
            return jsonify({'error': 'Usuario no encontrado'}), 404

        data = request.get_json() or {}

        if 'ci' in data and data['ci'] and data['ci'] != usuario.ci:
            if Usuario.query.filter(Usuario.ci == data['ci'], Usuario.id != id).first():
                return jsonify({'error': 'Ya existe un usuario con ese CI'}), 409
            usuario.ci = data['ci'].strip()

        if 'username' in data and data['username']:
            nuevo = data['username'].strip().lower()
            ok, msg = validar_username(nuevo)
            if not ok:
                return jsonify({'error': msg}), 400
            existe = Usuario.query.filter(
                db.func.lower(Usuario.username) == nuevo, Usuario.id != id
            ).first()
            if existe:
                return jsonify({'error': 'Ese nombre de usuario ya está en uso'}), 409
            usuario.username = nuevo

        if 'rol' in data and data['rol']:
            if data['rol'] not in ROLES_VALIDOS:
                return jsonify({'error': 'Rol inválido'}), 400
            usuario.rol = data['rol']

        for campo in ('nombres', 'apellidos'):
            if campo in data and data[campo]:
                setattr(usuario, campo, data[campo].strip())
        if 'edad' in data:
            usuario.edad = data['edad']

        registrar_actividad('editar', 'personal', f'Editó al usuario {usuario.username}')
        db.session.commit()
        return jsonify({'mensaje': 'Usuario actualizado', 'usuario': usuario.to_dict()}), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al actualizar usuario: {str(e)}'}), 500


@personal_bp.route('/<int:id>/reset-password', methods=['POST'])
@rol_requerido('admin')
def resetear_password(id):
    """Genera una nueva contraseña temporal y la devuelve una sola vez."""
    try:
        usuario = Usuario.query.get(id)
        if not usuario:
            return jsonify({'error': 'Usuario no encontrado'}), 404

        password_temporal = generar_password_temporal()
        usuario.set_password(password_temporal)
        usuario.es_password_temporal = True
        usuario.resetear_intentos()

        registrar_actividad('editar', 'personal', f'Restableció la contraseña de {usuario.username}')
        db.session.commit()

        return jsonify({
            'mensaje': 'Contraseña restablecida',
            'password_temporal': password_temporal,
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al restablecer contraseña: {str(e)}'}), 500


@personal_bp.route('/<int:id>/estado', methods=['PATCH'])
@rol_requerido('admin')
def cambiar_estado(id):
    """Activa o desactiva un usuario (sin eliminar el historial)."""
    try:
        usuario = Usuario.query.get(id)
        if not usuario:
            return jsonify({'error': 'Usuario no encontrado'}), 404
        if usuario.id == int(get_jwt_identity()):
            return jsonify({'error': 'No puedes desactivar tu propia cuenta'}), 400

        usuario.activo = not usuario.activo
        registrar_actividad(
            'editar', 'personal',
            f'{"Activó" if usuario.activo else "Desactivó"} al usuario {usuario.username}'
        )
        db.session.commit()
        return jsonify({'mensaje': 'Estado actualizado', 'usuario': usuario.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al cambiar estado: {str(e)}'}), 500


@personal_bp.route('/<int:id>', methods=['DELETE'])
@rol_requerido('admin')
def eliminar(id):
    """Elimina un usuario. Protege la cuenta propia y el último admin."""
    try:
        usuario = Usuario.query.get(id)
        if not usuario:
            return jsonify({'error': 'Usuario no encontrado'}), 404
        if usuario.id == int(get_jwt_identity()):
            return jsonify({'error': 'No puedes eliminar tu propia cuenta'}), 400
        if usuario.rol == 'admin':
            admins = Usuario.query.filter_by(rol='admin', activo=True).count()
            if admins <= 1:
                return jsonify({'error': 'Debe existir al menos un administrador'}), 400

        registrar_actividad('eliminar', 'personal', f'Eliminó al usuario {usuario.username}')
        db.session.delete(usuario)
        db.session.commit()
        return jsonify({'mensaje': 'Usuario eliminado'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al eliminar usuario: {str(e)}'}), 500
