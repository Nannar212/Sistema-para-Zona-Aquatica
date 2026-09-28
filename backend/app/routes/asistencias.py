"""
Asistencias — marca diaria de clientes: presente, falta o justificada.
En ausencias justificadas se pueden sumar días de extensión a la membresía
(corren la fecha de fin). Registrar: admin y recepcionista.
"""

import calendar
from datetime import date, datetime
from collections import defaultdict
from flask import Blueprint, request, jsonify
from flask_jwt_extended import get_jwt_identity
from app.extensions import db
from app.models.asistencia import Asistencia, ESTADOS_VALIDOS
from app.models.cliente import Cliente
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad

asistencias_bp = Blueprint('asistencias', __name__)


def _rango_mes(desde_str, hasta_str):
    """Devuelve (desde, hasta). Por defecto, el mes actual completo."""
    hoy = date.today()
    if desde_str:
        desde = date.fromisoformat(desde_str)
    else:
        desde = hoy.replace(day=1)
    if hasta_str:
        hasta = date.fromisoformat(hasta_str)
    else:
        ultimo = calendar.monthrange(desde.year, desde.month)[1]
        hasta = desde.replace(day=ultimo)
    return desde, hasta


def _parse_fecha(valor, defecto=None):
    if not valor:
        return defecto or date.today()
    return datetime.fromisoformat(valor).date()


def _aplicar_extension(cliente, delta_dias):
    """Suma (o resta) días de extensión a la membresía activa del cliente."""
    if not delta_dias:
        return
    activa = cliente.membresia_activa
    if activa:
        activa.dias_extension = max(0, (activa.dias_extension or 0) + delta_dias)


@asistencias_bp.route('/dia', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def por_dia():
    """
    Devuelve los clientes (de un horario o todos) con su asistencia del día indicado.
    Ideal para la pantalla de "pasar lista".
    """
    fecha = _parse_fecha(request.args.get('fecha'))
    horario_id = request.args.get('horario_id', type=int)

    query = Cliente.query.filter_by(activo=True)
    if horario_id:
        query = query.filter_by(horario_id=horario_id)
    clientes = query.order_by(Cliente.apellidos, Cliente.nombres).all()

    marcas = {
        a.cliente_id: a for a in
        Asistencia.query.filter_by(fecha=fecha).all()
    }

    filas = []
    for c in clientes:
        a = marcas.get(c.id)
        filas.append({
            'cliente_id': c.id,
            'nombre_completo': c.nombre_completo,
            'horario_id': c.horario_id,
            'horario': c.horario.nombre if c.horario else None,
            'estado': a.estado if a else None,
            'dias_justificados': a.dias_justificados if a else 0,
            'observacion': a.observacion if a else '',
        })

    return jsonify({'fecha': fecha.isoformat(), 'clientes': filas}), 200


@asistencias_bp.route('/cliente/<int:cliente_id>', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def historial_cliente(cliente_id):
    asistencias = (Asistencia.query
                   .filter_by(cliente_id=cliente_id)
                   .order_by(Asistencia.fecha.desc())
                   .limit(120).all())
    return jsonify([a.to_dict() for a in asistencias]), 200


@asistencias_bp.route('/cliente/<int:cliente_id>/rango', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def calendario_cliente(cliente_id):
    """Asistencias de un cliente en un rango (por defecto el mes actual) + totales."""
    cliente = Cliente.query.get(cliente_id)
    if not cliente:
        return jsonify({'error': 'Cliente no encontrado'}), 404

    desde, hasta = _rango_mes(request.args.get('desde'), request.args.get('hasta'))
    asis = (Asistencia.query
            .filter(Asistencia.cliente_id == cliente_id,
                    Asistencia.fecha >= desde, Asistencia.fecha <= hasta)
            .all())
    mapa = {a.fecha.isoformat(): a.estado for a in asis}
    totales = {'presente': 0, 'falta': 0, 'justificada': 0}
    for est in mapa.values():
        if est in totales:
            totales[est] += 1

    return jsonify({
        'cliente': {'id': cliente.id, 'nombre_completo': cliente.nombre_completo, 'edad': cliente.edad},
        'desde': desde.isoformat(),
        'hasta': hasta.isoformat(),
        'asistencias': mapa,
        'totales': totales,
    }), 200


@asistencias_bp.route('/general', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def general():
    """
    Grilla tipo Excel: todos los clientes activos con su asistencia por día
    dentro del rango (por defecto el mes actual).
    """
    desde, hasta = _rango_mes(request.args.get('desde'), request.args.get('hasta'))

    clientes = (Cliente.query.filter_by(estado='activo')
                .order_by(Cliente.apellidos, Cliente.nombres).all())

    asis = Asistencia.query.filter(Asistencia.fecha >= desde, Asistencia.fecha <= hasta).all()
    por_cliente = defaultdict(dict)
    for a in asis:
        por_cliente[a.cliente_id][a.fecha.isoformat()] = a.estado

    filas = []
    for c in clientes:
        m = c.membresia_activa
        mapa = por_cliente.get(c.id, {})
        totales = {'presente': 0, 'falta': 0, 'justificada': 0}
        for est in mapa.values():
            if est in totales:
                totales[est] += 1
        filas.append({
            'cliente_id': c.id,
            'nombre': c.nombre_completo,
            'edad': c.edad,
            'horario': c.horario.nombre if c.horario else None,
            'fecha_inicio': m.fecha_inicio.isoformat() if m else None,
            'fecha_fin': m.fecha_fin_efectiva.isoformat() if m else None,
            'asistencias': mapa,
            'totales': totales,
        })

    return jsonify({'desde': desde.isoformat(), 'hasta': hasta.isoformat(), 'clientes': filas}), 200


@asistencias_bp.route('', methods=['POST'])
@rol_requerido('admin', 'recepcionista')
def marcar():
    """
    Marca (o actualiza) la asistencia de un cliente en una fecha.
    Body: {cliente_id, fecha?, estado, dias_justificados?, observacion?, horario_id?}
    """
    try:
        data = request.get_json() or {}
        cliente = Cliente.query.get(data.get('cliente_id'))
        if not cliente:
            return jsonify({'error': 'Cliente no encontrado'}), 404

        estado = (data.get('estado') or '').lower()
        if estado not in ESTADOS_VALIDOS:
            return jsonify({'error': 'Estado inválido (presente, falta o justificada)'}), 400

        fecha = _parse_fecha(data.get('fecha'))
        dias_just = int(data.get('dias_justificados') or 0) if estado == 'justificada' else 0
        observacion = (data.get('observacion') or '').strip()
        horario_id = data.get('horario_id') or cliente.horario_id

        asistencia = Asistencia.query.filter_by(cliente_id=cliente.id, fecha=fecha).first()

        if asistencia:
            # Ajustar extensión por la diferencia de días justificados.
            delta = dias_just - (asistencia.dias_justificados or 0)
            _aplicar_extension(cliente, delta)
            asistencia.estado = estado
            asistencia.dias_justificados = dias_just
            asistencia.observacion = observacion
            asistencia.horario_id = horario_id
            asistencia.registrado_por = int(get_jwt_identity())
            accion_txt = 'Actualizó'
        else:
            _aplicar_extension(cliente, dias_just)
            asistencia = Asistencia(
                cliente_id=cliente.id,
                horario_id=horario_id,
                fecha=fecha,
                estado=estado,
                dias_justificados=dias_just,
                observacion=observacion,
                registrado_por=int(get_jwt_identity()),
            )
            db.session.add(asistencia)
            accion_txt = 'Marcó'

        registrar_actividad('editar', 'asistencias',
                            f'{accion_txt} asistencia ({estado}) de {cliente.nombre_completo}')
        db.session.commit()
        return jsonify({'mensaje': 'Asistencia registrada', 'asistencia': asistencia.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al marcar asistencia: {str(e)}'}), 500
