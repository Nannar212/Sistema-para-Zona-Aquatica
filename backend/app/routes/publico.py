"""
Público — datos de la página pública. Sin autenticación (acceso libre).
Las membresías se exponen como "planes" (solo el nombre visual cambia en la web).
"""

from flask import Blueprint, jsonify
from app.models.contenido_publico import ContenidoPublico
from app.models.membresia import Membresia
from app.models.horario import Horario

publico_bp = Blueprint('publico', __name__)


@publico_bp.route('/contenido', methods=['GET'])
def contenido():
    cont = ContenidoPublico.query.first()
    planes = [m.to_dict() for m in Membresia.query.filter_by(activa=True).order_by(Membresia.precio.asc()).all()]
    horarios = [h.to_dict(incluir_ocupacion=False) for h in
                Horario.query.filter_by(activo=True).order_by(Horario.hora_inicio.asc()).all()]
    return jsonify({
        'contenido': cont.to_dict() if cont else {},
        'planes': planes,
        'horarios': horarios,
    }), 200
