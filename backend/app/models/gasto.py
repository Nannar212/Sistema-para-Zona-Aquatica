"""
Gasto — egresos del negocio (cloro, pago de cisterna, mantenimiento, etc.).
Los registra el administrador.
"""

from datetime import datetime, date
from app.extensions import db

CATEGORIAS_SUGERIDAS = ('Cloro', 'Cisterna', 'Mantenimiento', 'Servicios', 'Insumos', 'Salarios', 'Otros')
METODOS_GASTO = ('efectivo', 'qr', 'transferencia')


class Gasto(db.Model):
    __tablename__ = 'gastos'

    id = db.Column(db.Integer, primary_key=True)
    categoria = db.Column(db.String(60), nullable=False, default='Otros')
    descripcion = db.Column(db.Text, default='')
    monto = db.Column(db.Numeric(10, 2), nullable=False, default=0)
    metodo = db.Column(db.String(20), default='efectivo')
    fecha = db.Column(db.Date, nullable=False, default=date.today, index=True)
    registrado_por = db.Column(db.Integer, db.ForeignKey('usuarios.id'))

    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'categoria': self.categoria,
            'descripcion': self.descripcion or '',
            'monto': float(self.monto) if self.monto is not None else 0.0,
            'metodo': self.metodo,
            'fecha': self.fecha.isoformat() if self.fecha else None,
            'registrado_por': self.registrado_por,
        }

    def __repr__(self):
        return f'<Gasto {self.categoria} Bs {self.monto}>'
