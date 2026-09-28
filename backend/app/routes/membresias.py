"""
Membresías — CRUD de planes.
Listar: admin y recepcionista. Crear/editar/eliminar: solo admin.
"""

from flask import Blueprint, request, jsonify
from app.extensions import db
from app.models.membresia import Membresia
from app.models.cliente_membresia import ClienteMembresia
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad

membresias_bp = Blueprint('membresias', __name__)


def _dias_validos(dias):
    if not isinstance(dias, list):
        return [0, 1, 2, 3, 4]
    return [int(d) for d in dias if isinstance(d, (int, float)) and 0 <= int(d) <= 6]


@membresias_bp.route('', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def listar():
    solo_activas = request.args.get('activas', 'false').lower() == 'true'
    query = Membresia.query
    if solo_activas:
        query = query.filter_by(activa=True)
    membresias = query.order_by(Membresia.precio.asc()).all()
    return jsonify([m.to_dict() for m in membresias]), 200


@membresias_bp.route('/<int:id>', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def obtener(id):
    m = Membresia.query.get(id)
    if not m:
        return jsonify({'error': 'Membresía no encontrada'}), 404
    return jsonify(m.to_dict()), 200


@membresias_bp.route('', methods=['POST'])
@rol_requerido('admin')
def crear():
    try:
        data = request.get_json() or {}
        nombre = (data.get('nombre') or '').strip()
        if not nombre:
            return jsonify({'error': 'El nombre es requerido'}), 400

        m = Membresia(
            nombre=nombre,
            descripcion=(data.get('descripcion') or '').strip(),
            duracion_dias=int(data.get('duracion_dias') or 30),
            numero_clases=(int(data['numero_clases']) if data.get('numero_clases') else None),
            precio=data.get('precio') or 0,
            dias_acceso=_dias_validos(data.get('dias_acceso')),
            activa=data.get('activa', True),
        )
        db.session.add(m)
        registrar_actividad('crear', 'membresias', f'Creó la membresía "{nombre}"')
        db.session.commit()
        return jsonify({'mensaje': 'Membresía creada', 'membresia': m.to_dict()}), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al crear membresía: {str(e)}'}), 500


@membresias_bp.route('/<int:id>', methods=['PUT'])
@rol_requerido('admin')
def actualizar(id):
    try:
        m = Membresia.query.get(id)
        if not m:
            return jsonify({'error': 'Membresía no encontrada'}), 404

        data = request.get_json() or {}
        if 'nombre' in data and data['nombre']:
            m.nombre = data['nombre'].strip()
        if 'descripcion' in data:
            m.descripcion = (data['descripcion'] or '').strip()
        if 'duracion_dias' in data:
            m.duracion_dias = int(data['duracion_dias'] or 30)
        if 'numero_clases' in data:
            m.numero_clases = int(data['numero_clases']) if data['numero_clases'] else None
        if 'precio' in data:
            m.precio = data['precio'] or 0
        if 'dias_acceso' in data:
            m.dias_acceso = _dias_validos(data['dias_acceso'])
        if 'activa' in data:
            m.activa = bool(data['activa'])

        registrar_actividad('editar', 'membresias', f'Editó la membresía "{m.nombre}"')
        db.session.commit()
        return jsonify({'mensaje': 'Membresía actualizada', 'membresia': m.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al actualizar membresía: {str(e)}'}), 500


@membresias_bp.route('/<int:id>', methods=['DELETE'])
@rol_requerido('admin')
def eliminar(id):
    try:
        m = Membresia.query.get(id)
        if not m:
            return jsonify({'error': 'Membresía no encontrada'}), 404

        # Si tiene asignaciones, se desactiva en vez de borrar (preserva historial).
        en_uso = ClienteMembresia.query.filter_by(membresia_id=id).count()
        if en_uso > 0:
            m.activa = False
            registrar_actividad('editar', 'membresias', f'Desactivó la membresía "{m.nombre}" (en uso)')
            db.session.commit()
            return jsonify({'mensaje': 'Membresía en uso: se desactivó en lugar de eliminarse'}), 200

        registrar_actividad('eliminar', 'membresias', f'Eliminó la membresía "{m.nombre}"')
        db.session.delete(m)
        db.session.commit()
        return jsonify({'mensaje': 'Membresía eliminada'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al eliminar membresía: {str(e)}'}), 500
