"""
Cliente — persona que asiste a la piscina. No inicia sesión (solo es un registro).
estado: activo | inactivo | eliminado.
- El "bucket" (pestaña) se deriva: si la membresía venció, aparece en inactivo
  con motivo "Vencimiento de membresía" aunque su estado guardado sea 'activo'.
"""

from datetime import datetime, date
from app.extensions import db

SEXOS_VALIDOS = ('M', 'F', 'Otro')
ESTADOS_CLIENTE = ('activo', 'inactivo', 'eliminado')


class Cliente(db.Model):
    __tablename__ = 'clientes'

    id = db.Column(db.Integer, primary_key=True)
    nombres = db.Column(db.String(120), nullable=False)
    apellidos = db.Column(db.String(120), nullable=False)
    sexo = db.Column(db.String(10), default='M')  # M | F | Otro
    edad = db.Column(db.Integer)
    celular = db.Column(db.String(30), default='')
    horario_id = db.Column(db.Integer, db.ForeignKey('horarios.id'))
    activo = db.Column(db.Boolean, default=True)  # se mantiene sincronizado: True si estado=='activo'
    estado = db.Column(db.String(20), default='activo')      # activo | inactivo | eliminado
    motivo_estado = db.Column(db.Text, default='')

    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    membresias = db.relationship(
        'ClienteMembresia', backref='cliente', lazy=True,
        cascade='all, delete-orphan', order_by='ClienteMembresia.fecha_inicio.desc()'
    )
    pagos = db.relationship('Pago', backref='cliente', lazy=True)
    asistencias = db.relationship('Asistencia', backref='cliente', lazy=True)

    @property
    def nombre_completo(self):
        return f'{self.nombres} {self.apellidos}'.strip()

    @property
    def membresia_activa(self):
        hoy = date.today()
        vigentes = [m for m in self.membresias if m.activa and m.fecha_fin_efectiva >= hoy]
        if vigentes:
            return max(vigentes, key=lambda m: m.fecha_fin_efectiva)
        return self.membresias[0] if self.membresias else None

    def set_estado(self, estado, motivo=''):
        self.estado = estado
        self.motivo_estado = motivo or ''
        self.activo = (estado == 'activo')

    @property
    def bucket(self):
        """Pestaña efectiva: activo | inactivo | eliminado."""
        if self.estado == 'eliminado':
            return 'eliminado'
        if self.estado == 'inactivo':
            return 'inactivo'
        m = self.membresia_activa
        if m and not m.vigente:
            return 'inactivo'  # membresía vencida
        return 'activo'

    @property
    def motivo(self):
        if self.estado == 'eliminado':
            return self.motivo_estado or 'Eliminado'
        if self.estado == 'inactivo':
            return self.motivo_estado or 'Marcado como inactivo'
        m = self.membresia_activa
        if m and not m.vigente:
            return 'Vencimiento de membresía'
        return ''

    def to_dict(self, incluir_membresia=True):
        data = {
            'id': self.id,
            'nombres': self.nombres,
            'apellidos': self.apellidos,
            'nombre_completo': self.nombre_completo,
            'sexo': self.sexo,
            'edad': self.edad,
            'celular': self.celular or '',
            'horario_id': self.horario_id,
            'horario': self.horario.nombre if self.horario else None,
            'activo': self.activo,
            'estado': self.estado,
            'bucket': self.bucket,
            'motivo': self.motivo,
            'created_at': self.created_at.isoformat() if self.created_at else None,
        }
        if incluir_membresia:
            m = self.membresia_activa
            data['membresia_activa'] = m.to_dict() if m else None
        return data

    def __repr__(self):
        return f'<Cliente {self.nombre_completo} ({self.estado})>'
