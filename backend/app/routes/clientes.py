"""
Clientes — registro de personas que asisten a la piscina.
Estados: activo | inactivo | eliminado (con motivo). Los vencidos aparecen como
inactivos automáticamente. Listar / crear / editar: admin y recepcionista.
Inactivar / eliminar / restaurar: solo admin.
"""

from datetime import date, datetime, timedelta
from flask import Blueprint, request, jsonify
from app.extensions import db
from app.models.cliente import Cliente, SEXOS_VALIDOS
from app.models.cliente_membresia import ClienteMembresia
from app.models.membresia import Membresia
from app.models.horario import Horario
from app.models.pago import Pago
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad
from app.utils.validators import limpiar_celular

clientes_bp = Blueprint('clientes', __name__)


def _parse_fecha(valor):
    if not valor:
        return None
    try:
        return date.fromisoformat(valor)
    except (ValueError, TypeError):
        return None


def _asignar_membresia(cliente, membresia_id, continuar=False, fecha_inicio=None):
    membresia = Membresia.query.get(membresia_id)
    if not membresia:
        return None

    inicio = _parse_fecha(fecha_inicio) if fecha_inicio else None
    if inicio is None:
        inicio = date.today()
        if continuar:
            activa = cliente.membresia_activa
            if activa and activa.vigente:
                inicio = activa.fecha_fin_efectiva + timedelta(days=1)

    for cm in cliente.membresias:
        cm.activa = False

    cm = ClienteMembresia(
        cliente_id=cliente.id,
        membresia_id=membresia.id,
        fecha_inicio=inicio,
        fecha_fin=inicio + timedelta(days=membresia.duracion_dias or 30),
        dias_extension=0,
        monto_total=membresia.precio,
        activa=True,
    )
    db.session.add(cm)
    return cm


@clientes_bp.route('', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def listar():
    q = (request.args.get('q') or '').strip().lower()
    horario_id = request.args.get('horario_id', type=int)
    estado = request.args.get('estado', 'activo')  # activo | inactivo | eliminado

    query = Cliente.query
    if horario_id:
        query = query.filter_by(horario_id=horario_id)
    if q:
        like = f'%{q}%'
        query = query.filter(
            db.or_(
                db.func.lower(Cliente.nombres).like(like),
                db.func.lower(Cliente.apellidos).like(like),
                Cliente.celular.like(like),
            )
        )
    clientes = query.order_by(Cliente.created_at.desc()).all()
    # El "bucket" se calcula (vencidos -> inactivo), así que filtramos en Python.
    filtrados = [c for c in clientes if c.bucket == estado]
    return jsonify([c.to_dict() for c in filtrados]), 200


@clientes_bp.route('/contadores', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def contadores():
    """Cantidad de clientes por pestaña (para mostrar en los tabs)."""
    conteo = {'activo': 0, 'inactivo': 0, 'eliminado': 0}
    for c in Cliente.query.all():
        conteo[c.bucket] = conteo.get(c.bucket, 0) + 1
    return jsonify(conteo), 200


@clientes_bp.route('/<int:id>', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def obtener(id):
    c = Cliente.query.get(id)
    if not c:
        return jsonify({'error': 'Cliente no encontrado'}), 404
    data = c.to_dict()
    data['membresias'] = [m.to_dict() for m in c.membresias]
    data['pagos'] = [p.to_dict() for p in sorted(c.pagos, key=lambda p: p.fecha or datetime.min, reverse=True)]
    return jsonify(data), 200


@clientes_bp.route('', methods=['POST'])
@rol_requerido('admin', 'recepcionista')
def crear():
    try:
        data = request.get_json() or {}
        nombres = (data.get('nombres') or '').strip()
        apellidos = (data.get('apellidos') or '').strip()
        if not nombres or not apellidos:
            return jsonify({'error': 'Nombres y apellidos son requeridos'}), 400

        sexo = data.get('sexo') or 'M'
        if sexo not in SEXOS_VALIDOS:
            sexo = 'M'

        horario_id = data.get('horario_id')
        if horario_id and not Horario.query.get(horario_id):
            return jsonify({'error': 'El horario seleccionado no existe'}), 400

        cliente = Cliente(
            nombres=nombres,
            apellidos=apellidos,
            sexo=sexo,
            edad=data.get('edad'),
            celular=limpiar_celular(data.get('celular')),
            horario_id=horario_id or None,
            activo=True,
            estado='activo',
        )
        db.session.add(cliente)
        db.session.flush()

        membresia_id = data.get('membresia_id')
        if membresia_id:
            cm = _asignar_membresia(cliente, membresia_id, fecha_inicio=data.get('fecha_inicio'))
            if cm is None:
                db.session.rollback()
                return jsonify({'error': 'La membresía seleccionada no existe'}), 400

        registrar_actividad('crear', 'clientes', f'Registró al cliente {cliente.nombre_completo}')
        db.session.commit()
        return jsonify({'mensaje': 'Cliente creado', 'cliente': cliente.to_dict()}), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al crear cliente: {str(e)}'}), 500


@clientes_bp.route('/<int:id>', methods=['PUT'])
@rol_requerido('admin', 'recepcionista')
def actualizar(id):
    try:
        c = Cliente.query.get(id)
        if not c:
            return jsonify({'error': 'Cliente no encontrado'}), 404

        data = request.get_json() or {}
        for campo in ('nombres', 'apellidos'):
            if campo in data and data[campo]:
                setattr(c, campo, data[campo].strip())
        if 'sexo' in data and data['sexo'] in SEXOS_VALIDOS:
            c.sexo = data['sexo']
        if 'edad' in data:
            c.edad = data['edad']
        if 'celular' in data:
            c.celular = limpiar_celular(data['celular'])
        if 'horario_id' in data:
            if data['horario_id'] and not Horario.query.get(data['horario_id']):
                return jsonify({'error': 'El horario seleccionado no existe'}), 400
            c.horario_id = data['horario_id'] or None

        registrar_actividad('editar', 'clientes', f'Editó al cliente {c.nombre_completo}')
        db.session.commit()
        return jsonify({'mensaje': 'Cliente actualizado', 'cliente': c.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al actualizar cliente: {str(e)}'}), 500


@clientes_bp.route('/<int:id>/membresia', methods=['POST'])
@rol_requerido('admin', 'recepcionista')
def asignar_membresia(id):
    """Asigna o renueva la membresía. Renovar reactiva a un cliente inactivo/eliminado."""
    try:
        c = Cliente.query.get(id)
        if not c:
            return jsonify({'error': 'Cliente no encontrado'}), 404

        data = request.get_json() or {}
        membresia_id = data.get('membresia_id')
        if not membresia_id:
            return jsonify({'error': 'Debe indicar la membresía'}), 400

        continuar = bool(data.get('continuar', False))
        cm = _asignar_membresia(c, membresia_id, continuar=continuar, fecha_inicio=data.get('fecha_inicio'))
        if cm is None:
            return jsonify({'error': 'La membresía seleccionada no existe'}), 400

        # Al renovar, el cliente vuelve a estar activo
        if c.estado != 'activo':
            c.set_estado('activo', '')

        registrar_actividad('editar', 'clientes', f'Asignó/renovó membresía a {c.nombre_completo}')
        db.session.commit()
        return jsonify({'mensaje': 'Membresía asignada', 'membresia': cm.to_dict()}), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al asignar membresía: {str(e)}'}), 500


@clientes_bp.route('/<int:id>/inactivar', methods=['PATCH'])
@rol_requerido('admin')
def inactivar(id):
    try:
        c = Cliente.query.get(id)
        if not c:
            return jsonify({'error': 'Cliente no encontrado'}), 404
        motivo = (request.get_json() or {}).get('motivo', '').strip()
        c.set_estado('inactivo', motivo)
        registrar_actividad('editar', 'clientes', f'Marcó como inactivo a {c.nombre_completo}')
        db.session.commit()
        return jsonify({'mensaje': 'Cliente marcado como inactivo', 'cliente': c.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error: {str(e)}'}), 500


@clientes_bp.route('/<int:id>/restaurar', methods=['PATCH'])
@rol_requerido('admin')
def restaurar(id):
    try:
        c = Cliente.query.get(id)
        if not c:
            return jsonify({'error': 'Cliente no encontrado'}), 404
        c.set_estado('activo', '')
        registrar_actividad('editar', 'clientes', f'Recuperó al cliente {c.nombre_completo}')
        db.session.commit()
        return jsonify({'mensaje': 'Cliente recuperado', 'cliente': c.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error: {str(e)}'}), 500


@clientes_bp.route('/<int:id>', methods=['DELETE'])
@rol_requerido('admin')
def eliminar(id):
    """
    Elimina (mueve a la pestaña Eliminados) con un motivo.
    Si eliminar_pagos=true, borra también los pagos del cliente (devolución):
    así el dinero deja de contar como ingreso.
    """
    try:
        c = Cliente.query.get(id)
        if not c:
            return jsonify({'error': 'Cliente no encontrado'}), 404

        data = request.get_json(silent=True) or {}
        motivo = (data.get('motivo') or '').strip()
        eliminar_pagos = bool(data.get('eliminar_pagos', False))

        pagos_borrados = 0
        if eliminar_pagos:
            pagos_borrados = Pago.query.filter_by(cliente_id=c.id).delete()

        c.set_estado('eliminado', motivo)
        detalle = f'Eliminó al cliente {c.nombre_completo}'
        if eliminar_pagos:
            detalle += f' y borró {pagos_borrados} pago(s) (devolución)'
        registrar_actividad('eliminar', 'clientes', detalle)
        db.session.commit()
        return jsonify({'mensaje': 'Cliente eliminado', 'pagos_borrados': pagos_borrados}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al eliminar cliente: {str(e)}'}), 500
