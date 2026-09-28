"""Modelos del sistema Zona Aquática. Importar aquí para que db.create_all los detecte."""

from app.models.usuario import Usuario
from app.models.membresia import Membresia
from app.models.horario import Horario
from app.models.cliente import Cliente
from app.models.cliente_membresia import ClienteMembresia
from app.models.pago import Pago
from app.models.asistencia import Asistencia
from app.models.auditoria import Auditoria
from app.models.gasto import Gasto
from app.models.contenido_publico import ContenidoPublico

__all__ = [
    'Usuario',
    'Membresia',
    'Horario',
    'Cliente',
    'ClienteMembresia',
    'Pago',
    'Asistencia',
    'Auditoria',
    'Gasto',
    'ContenidoPublico',
]
