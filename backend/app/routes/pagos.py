"""
Pagos — registro de dinero recibido. Métodos: efectivo, qr o mixto.
Admite pagos parciales (fianza/abono): si el monto no cubre el total de la
membresía, queda un saldo pendiente que se completa en pagos posteriores.
Registrar y listar: admin y recepcionista.
"""

import io
from datetime import datetime, date
from flask import Blueprint, request, jsonify, send_file
from flask_jwt_extended import get_jwt_identity
from app.extensions import db
from app.models.pago import Pago, METODOS_VALIDOS
from app.models.cliente import Cliente
from app.models.cliente_membresia import ClienteMembresia
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad

pagos_bp = Blueprint('pagos', __name__)


def _num(valor, defecto=0.0):
    try:
        return round(float(valor), 2)
    except (TypeError, ValueError):
        return defecto


@pagos_bp.route('', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def listar():
    cliente_id = request.args.get('cliente_id', type=int)
    desde = request.args.get('desde')
    hasta = request.args.get('hasta')

    query = Pago.query
    if cliente_id:
        query = query.filter_by(cliente_id=cliente_id)
    if desde:
        query = query.filter(Pago.fecha >= datetime.fromisoformat(desde))
    if hasta:
        query = query.filter(Pago.fecha <= datetime.fromisoformat(hasta + 'T23:59:59'))

    pagos = query.order_by(Pago.fecha.desc()).limit(500).all()
    return jsonify([p.to_dict() for p in pagos]), 200


@pagos_bp.route('', methods=['POST'])
@rol_requerido('admin', 'recepcionista')
def registrar():
    try:
        data = request.get_json() or {}
        cliente = Cliente.query.get(data.get('cliente_id')) if data.get('cliente_id') else None
        if not cliente:
            return jsonify({'error': 'Cliente no encontrado'}), 404

        metodo = (data.get('metodo') or 'efectivo').lower()
        if metodo not in METODOS_VALIDOS:
            return jsonify({'error': 'Método de pago inválido'}), 400

        monto = _num(data.get('monto'))
        monto_efectivo = _num(data.get('monto_efectivo'))
        monto_qr = _num(data.get('monto_qr'))

        if metodo == 'efectivo':
            monto_efectivo, monto_qr = monto, 0.0
        elif metodo == 'qr':
            monto_qr, monto_efectivo = monto, 0.0
        else:  # mixto
            if monto_efectivo <= 0 or monto_qr <= 0:
                return jsonify({'error': 'En pago mixto, efectivo y QR deben ser mayores a 0'}), 400
            monto = round(monto_efectivo + monto_qr, 2)

        if monto <= 0:
            return jsonify({'error': 'El monto debe ser mayor a 0'}), 400

        # Determinar la membresía a la que se aplica el pago
        cm = None
        if data.get('cliente_membresia_id'):
            cm = ClienteMembresia.query.get(data['cliente_membresia_id'])
            if not cm:
                return jsonify({'error': 'La membresía indicada no existe'}), 400
        else:
            cm = cliente.membresia_activa

        # Calcular saldo para decidir si es abono (pago parcial) o pago completo
        saldo_restante = None
        concepto = 'pago'
        if cm:
            saldo_previo = cm.saldo_pendiente
            saldo_restante = round(max(0.0, saldo_previo - monto), 2)
            concepto = 'abono' if saldo_restante > 0.009 else 'pago'

        # Fecha del pago: se puede registrar con fecha pasada (retroactivo)
        fecha_pago = datetime.utcnow()
        if data.get('fecha'):
            try:
                fecha_pago = datetime.fromisoformat(data['fecha'])
            except ValueError:
                pass

        pago = Pago(
            cliente_id=cliente.id,
            cliente_membresia_id=cm.id if cm else None,
            monto=monto,
            metodo=metodo,
            monto_efectivo=monto_efectivo,
            monto_qr=monto_qr,
            concepto=concepto,
            fecha=fecha_pago,
            registrado_por=int(get_jwt_identity()),
        )
        db.session.add(pago)
        registrar_actividad('crear', 'pagos',
                            f'Registró {concepto} de Bs {monto} ({metodo}) de {cliente.nombre_completo}')
        db.session.commit()

        return jsonify({
            'mensaje': 'Pago registrado',
            'pago': pago.to_dict(),
            'saldo_pendiente': saldo_restante,
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al registrar pago: {str(e)}'}), 500


@pagos_bp.route('/<int:id>/recibo', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def recibo(id):
    """Genera un recibo en PDF para el pago."""
    from reportlab.lib.pagesizes import A5
    from reportlab.lib.units import mm
    from reportlab.pdfgen import canvas

    pago = Pago.query.get(id)
    if not pago:
        return jsonify({'error': 'Pago no encontrado'}), 404

    cliente = pago.cliente
    cm = pago.cliente_membresia

    buffer = io.BytesIO()
    ancho, alto = A5
    c = canvas.Canvas(buffer, pagesize=A5)

    y = alto - 20 * mm
    c.setFillColorRGB(0.09, 0.35, 0.65)
    c.rect(0, alto - 15 * mm, ancho, 15 * mm, fill=1, stroke=0)
    c.setFillColorRGB(1, 1, 1)
    c.setFont('Helvetica-Bold', 14)
    c.drawString(15 * mm, alto - 10 * mm, 'Zona Aquática — Recibo de pago')

    c.setFillColorRGB(0, 0, 0)
    c.setFont('Helvetica', 10)
    def linea(etiqueta, valor):
        nonlocal y
        c.setFont('Helvetica-Bold', 10)
        c.drawString(15 * mm, y, f'{etiqueta}:')
        c.setFont('Helvetica', 10)
        c.drawString(55 * mm, y, str(valor))
        y -= 8 * mm

    linea('Recibo N°', pago.id)
    linea('Fecha', pago.fecha.strftime('%d/%m/%Y %H:%M') if pago.fecha else '-')
    linea('Cliente', cliente.nombre_completo if cliente else '-')
    linea('Concepto', (pago.concepto or 'pago').capitalize())
    if cm:
        linea('Membresía', cm.membresia.nombre if cm.membresia else '-')
    linea('Método', pago.metodo.capitalize())
    if pago.metodo == 'mixto':
        linea('  Efectivo', f'Bs {float(pago.monto_efectivo or 0):.2f}')
        linea('  QR', f'Bs {float(pago.monto_qr or 0):.2f}')

    y -= 3 * mm
    c.setFont('Helvetica-Bold', 13)
    c.drawString(15 * mm, y, f'Total pagado: Bs {float(pago.monto):.2f}')
    y -= 9 * mm
    if cm:
        c.setFont('Helvetica', 10)
        c.drawString(15 * mm, y, f'Saldo pendiente: Bs {cm.saldo_pendiente:.2f}')

    c.setFont('Helvetica-Oblique', 8)
    c.drawString(15 * mm, 12 * mm, 'Gracias por su preferencia — Quillacollo, Cochabamba')
    c.showPage()
    c.save()
    buffer.seek(0)

    return send_file(buffer, mimetype='application/pdf',
                     download_name=f'recibo_{pago.id}.pdf', as_attachment=False)


@pagos_bp.route('/resumen-hoy', methods=['GET'])
@rol_requerido('admin', 'recepcionista')
def resumen_hoy():
    """Cierre de caja del día: totales por método."""
    hoy = date.today()
    pagos = Pago.query.filter(db.func.date(Pago.fecha) == hoy).all()
    return jsonify({
        'fecha': hoy.isoformat(),
        'cantidad': len(pagos),
        'total': round(sum(float(p.monto) for p in pagos), 2),
        'total_efectivo': round(sum(float(p.monto_efectivo or 0) for p in pagos), 2),
        'total_qr': round(sum(float(p.monto_qr or 0) for p in pagos), 2),
    }), 200
