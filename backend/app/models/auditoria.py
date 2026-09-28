"""
Auditoria — registro de actividad. Guarda cada acción del personal
(crear, editar, eliminar, login, logout) para el módulo de registro de actividad.
"""

from datetime import datetime
from app.extensions import db

ACCIONES_VALIDAS = ('crear', 'editar', 'eliminar', 'login', 'logout')


class Auditoria(db.Model):
    __tablename__ = 'auditoria'

    id = db.Column(db.Integer, primary_key=True)
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuarios.id'))
    nombre_usuario = db.Column(db.String(200))
    rol_usuario = db.Column(db.String(20))
    accion = db.Column(db.String(20), nullable=False)
    modulo = db.Column(db.String(50), nullable=False)
    descripcion = db.Column(db.Text, default='')
    ip_address = db.Column(db.String(50))

    created_at = db.Column(db.DateTime, default=datetime.utcnow, index=True)

    def to_dict(self):
        return {
            'id': self.id,
            'usuario_id': self.usuario_id,
            'nombre_usuario': self.nombre_usuario,
            'rol_usuario': self.rol_usuario,
            'accion': self.accion,
            'modulo': self.modulo,
            'descripcion': self.descripcion or '',
            'ip_address': self.ip_address,
            'fecha': self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self):
        return f'<Auditoria {self.accion} {self.modulo}>'
