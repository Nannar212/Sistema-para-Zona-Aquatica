"""
Horario — franja recurrente de la piscina (ej: "Mañana 07:00-09:00").
Se muestra en el calendario tipo Google. Un cliente se asigna a un horario.
dias_semana: lista de días en que se dicta (0=lunes ... 4=viernes).
"""

from datetime import datetime
from app.extensions import db


class Horario(db.Model):
    __tablename__ = 'horarios'

    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(120), nullable=False)
    hora_inicio = db.Column(db.Time, nullable=False)
    hora_fin = db.Column(db.Time, nullable=False)
    cupo_maximo = db.Column(db.Integer, nullable=False, default=30)
    dias_semana = db.Column(db.JSON, default=lambda: [0, 1, 2, 3, 4])
    color = db.Column(db.String(20), default='#185fa5')
    activo = db.Column(db.Boolean, default=True)

    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    clientes = db.relationship('Cliente', backref='horario', lazy=True)

    @property
    def cupos_ocupados(self):
        from app.models.cliente import Cliente
        return Cliente.query.filter_by(horario_id=self.id, activo=True).count()

    def to_dict(self, incluir_ocupacion=True):
        data = {
            'id': self.id,
            'nombre': self.nombre,
            'hora_inicio': self.hora_inicio.strftime('%H:%M') if self.hora_inicio else None,
            'hora_fin': self.hora_fin.strftime('%H:%M') if self.hora_fin else None,
            'cupo_maximo': self.cupo_maximo,
            'dias_semana': self.dias_semana or [0, 1, 2, 3, 4],
            'color': self.color or '#185fa5',
            'activo': self.activo,
        }
        if incluir_ocupacion:
            ocupados = self.cupos_ocupados
            data['cupos_ocupados'] = ocupados
            data['cupos_disponibles'] = max(0, (self.cupo_maximo or 0) - ocupados)
        return data

    def __repr__(self):
        return f'<Horario {self.nombre}>'
