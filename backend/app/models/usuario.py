"""
Usuario — personal del sistema (solo roles admin y recepcionista).
Son los únicos que inician sesión. Login por nombre de usuario.
"""

from datetime import datetime, timedelta
import bcrypt
from app.extensions import db

ROLES_VALIDOS = ('admin', 'recepcionista')


class Usuario(db.Model):
    __tablename__ = 'usuarios'

    id = db.Column(db.Integer, primary_key=True)
    nombres = db.Column(db.String(120), nullable=False)
    apellidos = db.Column(db.String(120), nullable=False)
    ci = db.Column(db.String(30), unique=True, nullable=False, index=True)
    username = db.Column(db.String(60), unique=True, nullable=False, index=True)
    password_hash = db.Column(db.String(255), nullable=False)
    edad = db.Column(db.Integer)
    rol = db.Column(db.String(20), nullable=False)  # admin | recepcionista

    es_password_temporal = db.Column(db.Boolean, default=True)
    activo = db.Column(db.Boolean, default=True)
    intentos_fallidos = db.Column(db.Integer, default=0)
    bloqueado_hasta = db.Column(db.DateTime)
    ultimo_login = db.Column(db.DateTime)

    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # ─── Contraseña (bcrypt, una sola vía) ───
    def set_password(self, password):
        self.password_hash = bcrypt.hashpw(
            password.encode('utf-8'), bcrypt.gensalt()
        ).decode('utf-8')

    def check_password(self, password):
        try:
            return bcrypt.checkpw(password.encode('utf-8'), self.password_hash.encode('utf-8'))
        except (ValueError, AttributeError):
            return False

    # ─── Bloqueo por intentos fallidos ───
    def esta_bloqueado(self):
        return bool(self.bloqueado_hasta and self.bloqueado_hasta > datetime.utcnow())

    def registrar_intento_fallido(self):
        self.intentos_fallidos = (self.intentos_fallidos or 0) + 1
        if self.intentos_fallidos >= 5:
            self.bloqueado_hasta = datetime.utcnow() + timedelta(minutes=2)

    def resetear_intentos(self):
        self.intentos_fallidos = 0
        self.bloqueado_hasta = None

    @property
    def nombre_completo(self):
        return f'{self.nombres} {self.apellidos}'.strip()

    def to_dict(self):
        return {
            'id': self.id,
            'nombres': self.nombres,
            'apellidos': self.apellidos,
            'nombre_completo': self.nombre_completo,
            'ci': self.ci,
            'username': self.username,
            'edad': self.edad,
            'rol': self.rol,
            'activo': self.activo,
            'es_password_temporal': self.es_password_temporal,
            'ultimo_login': self.ultimo_login.isoformat() if self.ultimo_login else None,
            'created_at': self.created_at.isoformat() if self.created_at else None,
        }

    def __repr__(self):
        return f'<Usuario {self.username} ({self.rol})>'
