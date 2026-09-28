"""
Horarios — franjas de la piscina para el calendario tipo Google.
Listar y ver personas: admin y recepcionista. Crear/editar/eliminar: solo admin.
"""

from datetime import datetime
from flask import Blueprint, request, jsonify
from app.extensions import db
from app.models.horario import Horario
from app.models.cliente import Cliente
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad

horarios_bp = Blueprint('horarios', __name__)


def _parse_hora(valor, defecto=None):
    if not valor:
        return defecto
    try:
        return datetime.strptime(valor, '%H:%M').time()
    except ValueError:
        return datetime.strptime(valor, '%H:%M:%S').time()


def _dias_validos(dias):
    if not isinstance(dias, list):
        return [0, 1, 2, 3, 4]
    return [int(d) for d in dias if isinstance(d, (int, float)) and 0 <= int(d) <= 6]


@horarios_bp.route('', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def listar():
    solo_activos = request.args.get('activos', 'false').lower() == 'true'
    query = Horario.query
    if solo_activos:
        query = query.filter_by(activo=True)
    horarios = query.order_by(Horario.hora_inicio.asc()).all()
    return jsonify([h.to_dict() for h in horarios]), 200


@horarios_bp.route('/<int:id>', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def obtener(id):
    h = Horario.query.get(id)
    if not h:
        return jsonify({'error': 'Horario no encontrado'}), 404
    return jsonify(h.to_dict()), 200


@horarios_bp.route('/<int:id>/personas', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def personas(id):
    """Lista de clientes asignados a este horario."""
    h = Horario.query.get(id)
    if not h:
        return jsonify({'error': 'Horario no encontrado'}), 404
    clientes = Cliente.query.filter_by(horario_id=id, activo=True).order_by(Cliente.apellidos).all()
    return jsonify({
        'horario': h.to_dict(),
        'personas': [c.to_dict() for c in clientes],
    }), 200


@horarios_bp.route('', methods=['POST'])
@rol_requerido('admin')
def crear():
    try:
        data = request.get_json() or {}
        nombre = (data.get('nombre') or '').strip()
        hora_inicio = _parse_hora(data.get('hora_inicio'))
        hora_fin = _parse_hora(data.get('hora_fin'))

        if not nombre or not hora_inicio or not hora_fin:
            return jsonify({'error': 'Nombre, hora de inicio y fin son requeridos'}), 400
        if hora_fin <= hora_inicio:
            return jsonify({'error': 'La hora de fin debe ser mayor a la de inicio'}), 400

        h = Horario(
            nombre=nombre,
            hora_inicio=hora_inicio,
            hora_fin=hora_fin,
            cupo_maximo=int(data.get('cupo_maximo') or 30),
            dias_semana=_dias_validos(data.get('dias_semana')),
            color=(data.get('color') or '#185fa5'),
            activo=data.get('activo', True),
        )
        db.session.add(h)
        registrar_actividad('crear', 'horarios', f'Creó el horario "{nombre}"')
        db.session.commit()
        return jsonify({'mensaje': 'Horario creado', 'horario': h.to_dict()}), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al crear horario: {str(e)}'}), 500


@horarios_bp.route('/<int:id>', methods=['PUT'])
@rol_requerido('admin')
def actualizar(id):
    try:
        h = Horario.query.get(id)
        if not h:
            return jsonify({'error': 'Horario no encontrado'}), 404

        data = request.get_json() or {}
        if 'nombre' in data and data['nombre']:
            h.nombre = data['nombre'].strip()
        if 'hora_inicio' in data and data['hora_inicio']:
            h.hora_inicio = _parse_hora(data['hora_inicio'])
        if 'hora_fin' in data and data['hora_fin']:
            h.hora_fin = _parse_hora(data['hora_fin'])
        if h.hora_fin <= h.hora_inicio:
            return jsonify({'error': 'La hora de fin debe ser mayor a la de inicio'}), 400
        if 'cupo_maximo' in data:
            h.cupo_maximo = int(data['cupo_maximo'] or 30)
        if 'dias_semana' in data:
            h.dias_semana = _dias_validos(data['dias_semana'])
        if 'color' in data and data['color']:
            h.color = data['color']
        if 'activo' in data:
            h.activo = bool(data['activo'])

        registrar_actividad('editar', 'horarios', f'Editó el horario "{h.nombre}"')
        db.session.commit()
        return jsonify({'mensaje': 'Horario actualizado', 'horario': h.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al actualizar horario: {str(e)}'}), 500


@horarios_bp.route('/<int:id>', methods=['DELETE'])
@rol_requerido('admin')
def eliminar(id):
    try:
        h = Horario.query.get(id)
        if not h:
            return jsonify({'error': 'Horario no encontrado'}), 404

        asignados = Cliente.query.filter_by(horario_id=id, activo=True).count()
        if asignados > 0:
            h.activo = False
            registrar_actividad('editar', 'horarios', f'Desactivó el horario "{h.nombre}" (con clientes)')
            db.session.commit()
            return jsonify({'mensaje': 'Horario con clientes: se desactivó en lugar de eliminarse'}), 200

        registrar_actividad('eliminar', 'horarios', f'Eliminó el horario "{h.nombre}"')
        db.session.delete(h)
        db.session.commit()
        return jsonify({'mensaje': 'Horario eliminado'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al eliminar horario: {str(e)}'}), 500
