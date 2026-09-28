"""
Gastos (egresos) — compras y pagos del negocio (cloro, cisterna, etc.).
Registra el administrador.
"""

from datetime import datetime, date
from flask import Blueprint, request, jsonify
from flask_jwt_extended import get_jwt_identity
from app.extensions import db
from app.models.gasto import Gasto, CATEGORIAS_SUGERIDAS
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad

gastos_bp = Blueprint('gastos', __name__)


def _num(v, d=0.0):
    try:
        return round(float(v), 2)
    except (TypeError, ValueError):
        return d


@gastos_bp.route('/categorias', methods=['GET'])
@rol_requerido('admin')
def categorias():
    return jsonify(list(CATEGORIAS_SUGERIDAS)), 200


@gastos_bp.route('', methods=['GET'])
@rol_requerido('admin')
def listar():
    desde = request.args.get('desde')
    hasta = request.args.get('hasta')
    categoria = request.args.get('categoria')

    query = Gasto.query
    if desde:
        query = query.filter(Gasto.fecha >= date.fromisoformat(desde))
    if hasta:
        query = query.filter(Gasto.fecha <= date.fromisoformat(hasta))
    if categoria:
        query = query.filter_by(categoria=categoria)

    gastos = query.order_by(Gasto.fecha.desc(), Gasto.id.desc()).limit(500).all()
    total = round(sum(float(g.monto) for g in gastos), 2)
    return jsonify({'gastos': [g.to_dict() for g in gastos], 'total': total}), 200


@gastos_bp.route('', methods=['POST'])
@rol_requerido('admin')
def crear():
    try:
        data = request.get_json() or {}
        categoria = (data.get('categoria') or 'Otros').strip()
        monto = _num(data.get('monto'))
        if monto <= 0:
            return jsonify({'error': 'El monto debe ser mayor a 0'}), 400

        gasto = Gasto(
            categoria=categoria,
            descripcion=(data.get('descripcion') or '').strip(),
            monto=monto,
            metodo=(data.get('metodo') or 'efectivo'),
            fecha=date.fromisoformat(data['fecha']) if data.get('fecha') else date.today(),
            registrado_por=int(get_jwt_identity()),
        )
        db.session.add(gasto)
        registrar_actividad('crear', 'gastos', f'Registró gasto {categoria}: Bs {monto}')
        db.session.commit()
        return jsonify({'mensaje': 'Gasto registrado', 'gasto': gasto.to_dict()}), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al registrar gasto: {str(e)}'}), 500


@gastos_bp.route('/<int:id>', methods=['PUT'])
@rol_requerido('admin')
def actualizar(id):
    try:
        g = Gasto.query.get(id)
        if not g:
            return jsonify({'error': 'Gasto no encontrado'}), 404
        data = request.get_json() or {}
        if 'categoria' in data and data['categoria']:
            g.categoria = data['categoria'].strip()
        if 'descripcion' in data:
            g.descripcion = (data['descripcion'] or '').strip()
        if 'monto' in data:
            g.monto = _num(data['monto'])
        if 'metodo' in data and data['metodo']:
            g.metodo = data['metodo']
        if 'fecha' in data and data['fecha']:
            g.fecha = date.fromisoformat(data['fecha'])

        registrar_actividad('editar', 'gastos', f'Editó gasto #{g.id}')
        db.session.commit()
        return jsonify({'mensaje': 'Gasto actualizado', 'gasto': g.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al actualizar gasto: {str(e)}'}), 500


@gastos_bp.route('/<int:id>', methods=['DELETE'])
@rol_requerido('admin')
def eliminar(id):
    try:
        g = Gasto.query.get(id)
        if not g:
            return jsonify({'error': 'Gasto no encontrado'}), 404
        registrar_actividad('eliminar', 'gastos', f'Eliminó gasto #{g.id} ({g.categoria})')
        db.session.delete(g)
        db.session.commit()
        return jsonify({'mensaje': 'Gasto eliminado'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al eliminar gasto: {str(e)}'}), 500
