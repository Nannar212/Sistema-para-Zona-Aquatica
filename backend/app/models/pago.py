"""
Pago — registro de dinero recibido por una membresía.
metodo: efectivo | qr | mixto. Si es mixto se detalla monto_efectivo + monto_qr.
Los ingresos del negocio se calculan a partir de esta tabla.
"""

from datetime import datetime
from app.extensions import db

METODOS_VALIDOS = ('efectivo', 'qr', 'mixto')


class Pago(db.Model):
    __tablename__ = 'pagos'

    id = db.Column(db.Integer, primary_key=True)
    cliente_id = db.Column(db.Integer, db.ForeignKey('clientes.id'), nullable=False)
    cliente_membresia_id = db.Column(db.Integer, db.ForeignKey('clientes_membresias.id'))
    monto = db.Column(db.Numeric(10, 2), nullable=False, default=0)
    metodo = db.Column(db.String(20), nullable=False, default='efectivo')
    monto_efectivo = db.Column(db.Numeric(10, 2), default=0)
    monto_qr = db.Column(db.Numeric(10, 2), default=0)
    concepto = db.Column(db.String(30), default='pago')  # pago | abono (pago parcial)
    fecha = db.Column(db.DateTime, default=datetime.utcnow)
    registrado_por = db.Column(db.Integer, db.ForeignKey('usuarios.id'))

    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'cliente_id': self.cliente_id,
            'cliente_nombre': self.cliente.nombre_completo if self.cliente else None,
            'cliente_membresia_id': self.cliente_membresia_id,
            'monto': float(self.monto) if self.monto is not None else 0.0,
            'metodo': self.metodo,
            'monto_efectivo': float(self.monto_efectivo) if self.monto_efectivo is not None else 0.0,
            'monto_qr': float(self.monto_qr) if self.monto_qr is not None else 0.0,
            'concepto': self.concepto or 'pago',
            'fecha': self.fecha.isoformat() if self.fecha else None,
            'registrado_por': self.registrado_por,
        }

    def __repr__(self):
        return f'<Pago {self.monto} ({self.metodo})>'
