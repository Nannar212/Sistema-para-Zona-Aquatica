"""
Asistencia — marca diaria de un cliente.
estado: presente | falta | justificada.
dias_justificados: si es justificada, cuántos días se extiende la membresía.
"""

from datetime import datetime, date
from app.extensions import db

ESTADOS_VALIDOS = ('presente', 'falta', 'justificada')


class Asistencia(db.Model):
    __tablename__ = 'asistencias'

    id = db.Column(db.Integer, primary_key=True)
    cliente_id = db.Column(db.Integer, db.ForeignKey('clientes.id'), nullable=False)
    horario_id = db.Column(db.Integer, db.ForeignKey('horarios.id'))
    fecha = db.Column(db.Date, nullable=False, default=date.today, index=True)
    estado = db.Column(db.String(20), nullable=False, default='presente')
    dias_justificados = db.Column(db.Integer, default=0)
    observacion = db.Column(db.Text, default='')
    registrado_por = db.Column(db.Integer, db.ForeignKey('usuarios.id'))

    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    __table_args__ = (
        db.UniqueConstraint('cliente_id', 'fecha', name='uq_asistencia_cliente_fecha'),
    )

    def to_dict(self):
        return {
            'id': self.id,
            'cliente_id': self.cliente_id,
            'cliente_nombre': self.cliente.nombre_completo if self.cliente else None,
            'horario_id': self.horario_id,
            'fecha': self.fecha.isoformat() if self.fecha else None,
            'estado': self.estado,
            'dias_justificados': self.dias_justificados or 0,
            'observacion': self.observacion or '',
            'registrado_por': self.registrado_por,
        }

    def __repr__(self):
        return f'<Asistencia cliente={self.cliente_id} {self.fecha} {self.estado}>'
