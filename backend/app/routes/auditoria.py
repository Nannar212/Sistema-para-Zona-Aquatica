"""
Registro de actividad — historial de acciones del personal. Solo admin.
"""

from flask import Blueprint, request, jsonify
from app.models.auditoria import Auditoria
from app.utils.decorators import rol_requerido

auditoria_bp = Blueprint('auditoria', __name__)


@auditoria_bp.route('', methods=['GET'])
@rol_requerido('admin')
def listar():
    modulo = request.args.get('modulo')
    accion = request.args.get('accion')
    limit = min(request.args.get('limit', default=200, type=int), 1000)

    query = Auditoria.query
    if modulo:
        query = query.filter_by(modulo=modulo)
    if accion:
        query = query.filter_by(accion=accion)

    registros = query.order_by(Auditoria.created_at.desc()).limit(limit).all()
    return jsonify([r.to_dict() for r in registros]), 200
