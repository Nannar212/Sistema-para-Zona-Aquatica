"""
Panel — resumen de indicadores del núcleo (clientes, membresías, ingresos del día/mes).
Los gráficos completos se amplían en la fase de extras.
"""

from datetime import date
from flask import Blueprint, jsonify
from app.extensions import db
from app.models.cliente import Cliente
from app.models.pago import Pago
from app.models.horario import Horario
from app.models.membresia import Membresia
from app.models.cliente_membresia import ClienteMembresia
from app.services.avisos_vencimiento import membresias_por_vencer
from app.utils.decorators import rol_requerido

dashboard_bp = Blueprint('dashboard', __name__)


@dashboard_bp.route('/resumen', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def resumen():
    hoy = date.today()

    clientes_activos = Cliente.query.filter_by(activo=True).count()

    # Membresías vigentes (activas y no vencidas, considerando extensiones).
    vigentes = sum(1 for cm in ClienteMembresia.query.filter_by(activa=True).all() if cm.vigente)
    por_vencer = len(membresias_por_vencer())

    pagos_hoy = Pago.query.filter(db.func.date(Pago.fecha) == hoy).all()
    ingresos_hoy = round(sum(float(p.monto) for p in pagos_hoy), 2)

    pagos_mes = Pago.query.filter(
        db.extract('year', Pago.fecha) == hoy.year,
        db.extract('month', Pago.fecha) == hoy.month,
    ).all()
    ingresos_mes = round(sum(float(p.monto) for p in pagos_mes), 2)

    # Distribución de clientes por membresía (para gráfico).
    distribucion = []
    for m in Membresia.query.all():
        cuenta = ClienteMembresia.query.filter_by(membresia_id=m.id, activa=True).count()
        if cuenta > 0:
            distribucion.append({'membresia': m.nombre, 'cantidad': cuenta})

    # Ocupación por horario.
    ocupacion = [
        {'horario': h.nombre, 'ocupados': h.cupos_ocupados, 'cupo': h.cupo_maximo}
        for h in Horario.query.filter_by(activo=True).order_by(Horario.hora_inicio).all()
    ]

    return jsonify({
        'clientes_activos': clientes_activos,
        'membresias_vigentes': vigentes,
        'membresias_por_vencer': por_vencer,
        'ingresos_hoy': ingresos_hoy,
        'ingresos_mes': ingresos_mes,
        'distribucion_membresias': distribucion,
        'ocupacion_horarios': ocupacion,
    }), 200
