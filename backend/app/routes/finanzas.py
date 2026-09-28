"""
Finanzas — ingresos (pagos), egresos (gastos), balance y cierre de caja.
Solo administrador.
"""

from datetime import date, datetime, timedelta
from collections import defaultdict
from flask import Blueprint, request, jsonify
from app.extensions import db
from app.models.pago import Pago
from app.models.gasto import Gasto
from app.utils.decorators import rol_requerido

finanzas_bp = Blueprint('finanzas', __name__)


def _rango(desde, hasta):
    hoy = date.today()
    if not desde:
        desde = hoy.replace(day=1)
    else:
        desde = date.fromisoformat(desde)
    hasta = date.fromisoformat(hasta) if hasta else hoy
    return desde, hasta


@finanzas_bp.route('/resumen', methods=['GET'])
@rol_requerido('admin')
def resumen():
    desde, hasta = _rango(request.args.get('desde'), request.args.get('hasta'))

    pagos = Pago.query.filter(
        db.func.date(Pago.fecha) >= desde,
        db.func.date(Pago.fecha) <= hasta,
    ).all()
    gastos = Gasto.query.filter(Gasto.fecha >= desde, Gasto.fecha <= hasta).all()

    ingresos_total = round(sum(float(p.monto) for p in pagos), 2)
    ingresos_efectivo = round(sum(float(p.monto_efectivo or 0) for p in pagos), 2)
    ingresos_qr = round(sum(float(p.monto_qr or 0) for p in pagos), 2)
    egresos_total = round(sum(float(g.monto) for g in gastos), 2)

    # Serie por día para gráfico
    por_dia = defaultdict(lambda: {'ingresos': 0.0, 'egresos': 0.0})
    for p in pagos:
        por_dia[p.fecha.date().isoformat()]['ingresos'] += float(p.monto)
    for g in gastos:
        por_dia[g.fecha.isoformat()]['egresos'] += float(g.monto)

    serie = []
    dia = desde
    while dia <= hasta:
        k = dia.isoformat()
        serie.append({
            'fecha': k,
            'ingresos': round(por_dia[k]['ingresos'], 2),
            'egresos': round(por_dia[k]['egresos'], 2),
        })
        dia += timedelta(days=1)

    # Egresos por categoría
    por_categoria = defaultdict(float)
    for g in gastos:
        por_categoria[g.categoria] += float(g.monto)
    egresos_categoria = [{'categoria': k, 'monto': round(v, 2)} for k, v in por_categoria.items()]

    # Ingresos por cliente
    por_cliente = {}
    for p in pagos:
        info = por_cliente.setdefault(p.cliente_id, {'cliente': None, 'total': 0.0, 'cantidad': 0})
        info['total'] += float(p.monto)
        info['cantidad'] += 1
        if info['cliente'] is None:
            info['cliente'] = p.cliente.nombre_completo if p.cliente else 'Cliente eliminado'
    ingresos_cliente = sorted(
        [{'cliente_id': k, 'cliente': v['cliente'], 'total': round(v['total'], 2), 'cantidad': v['cantidad']}
         for k, v in por_cliente.items()],
        key=lambda x: x['total'], reverse=True
    )

    return jsonify({
        'desde': desde.isoformat(),
        'hasta': hasta.isoformat(),
        'ingresos_total': ingresos_total,
        'ingresos_efectivo': ingresos_efectivo,
        'ingresos_qr': ingresos_qr,
        'egresos_total': egresos_total,
        'balance': round(ingresos_total - egresos_total, 2),
        'serie': serie if len(serie) <= 62 else serie[-62:],
        'egresos_categoria': egresos_categoria,
        'ingresos_cliente': ingresos_cliente,
    }), 200


@finanzas_bp.route('/cierre-caja', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def cierre_caja():
    """Cierre de caja de un día (por defecto hoy): totales por método."""
    dia = date.fromisoformat(request.args.get('fecha')) if request.args.get('fecha') else date.today()
    pagos = Pago.query.filter(db.func.date(Pago.fecha) == dia).all()
    return jsonify({
        'fecha': dia.isoformat(),
        'cantidad': len(pagos),
        'total': round(sum(float(p.monto) for p in pagos), 2),
        'total_efectivo': round(sum(float(p.monto_efectivo or 0) for p in pagos), 2),
        'total_qr': round(sum(float(p.monto_qr or 0) for p in pagos), 2),
        'pagos': [p.to_dict() for p in sorted(pagos, key=lambda p: p.fecha or datetime.min, reverse=True)],
    }), 200
