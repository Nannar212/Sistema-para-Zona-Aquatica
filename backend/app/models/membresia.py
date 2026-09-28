"""
Membresia — planes que se le asignan a los clientes.
dias_acceso: lista de días de la semana permitidos (0=lunes ... 6=domingo).
numero_clases: cantidad total de clases del paquete (ej: 9, 15). Si está vacío,
    las clases se cuentan por días de calendario (modo mensual/por días).
"""

from datetime import datetime
from app.extensions import db


class Membresia(db.Model):
    __tablename__ = 'membresias'

    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(120), nullable=False)
    descripcion = db.Column(db.Text, default='')
    duracion_dias = db.Column(db.Integer, nullable=False, default=30)
    numero_clases = db.Column(db.Integer)  # None = por días de calendario
    precio = db.Column(db.Numeric(10, 2), nullable=False, default=0)
    # Días de la semana con acceso. Por defecto lunes a viernes [0,1,2,3,4]
    dias_acceso = db.Column(db.JSON, default=lambda: [0, 1, 2, 3, 4])
    activa = db.Column(db.Boolean, default=True)

    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'nombre': self.nombre,
            'descripcion': self.descripcion or '',
            'duracion_dias': self.duracion_dias,
            'numero_clases': self.numero_clases,
            'precio': float(self.precio) if self.precio is not None else 0.0,
            'dias_acceso': self.dias_acceso or [0, 1, 2, 3, 4],
            'activa': self.activa,
        }

    def __repr__(self):
        return f'<Membresia {self.nombre} ({self.duracion_dias}d)>'
