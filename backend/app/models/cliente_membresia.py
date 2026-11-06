"""
ClienteMembresia — asignación de una membresía a un cliente, con vigencia.
- fecha_inicio: la elige el usuario (a veces pagan antes de empezar).
- Las clases se manejan según el PLAN:
    * Si el plan tiene numero_clases (ej: 9), ese es el total, y la fecha de fin
      se calcula como la fecha de la última clase (el n-ésimo día habilitado).
    * Si no, se cuenta por días de calendario (duracion_dias).
  Se descuenta una clase por cada día habilitado del plan que ya pasó (sin contar
  hoy hasta que termine el día).
- dias_extension: días extra por ausencias justificadas.
- monto_total: precio pactado (permite pagos parciales / saldo).
- aviso_vencimiento_para: fecha de fin ya avisada por correo. Si una extensión
  corre esa fecha, el cliente vuelve a recibir el aviso cuando le toque.
"""

from datetime import datetime, date, timedelta
from app.extensions import db

DIAS_ALERTA_VENCIMIENTO = 3


def _contar_dias_semana(inicio, fin_inclusive, dias):
    """Cuenta los días entre inicio y fin (inclusive) cuyo día de semana esté en `dias`."""
    if not dias or not inicio or not fin_inclusive or fin_inclusive < inicio:
        return 0
    dset = set(dias)
    total = 0
    d = inicio
    while d <= fin_inclusive:
        if d.weekday() in dset:
            total += 1
        d += timedelta(days=1)
    return total


def _nth_dia_semana(inicio, n, dias):
    """Fecha del n-ésimo día habilitado (contando desde inicio inclusive)."""
    dset = set(dias) if dias else {0, 1, 2, 3, 4}
    if n <= 0:
        return inicio
    d = inicio
    contados = 0
    limite = inicio + timedelta(days=7 * n + 14)  # tope de seguridad
    while d <= limite:
        if d.weekday() in dset:
            contados += 1
            if contados >= n:
                return d
        d += timedelta(days=1)
    return d


class ClienteMembresia(db.Model):
    __tablename__ = 'clientes_membresias'

    id = db.Column(db.Integer, primary_key=True)
    cliente_id = db.Column(db.Integer, db.ForeignKey('clientes.id'), nullable=False)
    membresia_id = db.Column(db.Integer, db.ForeignKey('membresias.id'), nullable=False)
    fecha_inicio = db.Column(db.Date, nullable=False, default=date.today)
    fecha_fin = db.Column(db.Date, nullable=False)  # respaldo (modo por días)
    dias_extension = db.Column(db.Integer, default=0)
    monto_total = db.Column(db.Numeric(10, 2))
    activa = db.Column(db.Boolean, default=True)
    aviso_vencimiento_para = db.Column(db.Date)

    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    membresia = db.relationship('Membresia', lazy=True)
    pagos = db.relationship('Pago', backref='cliente_membresia', lazy=True)

    # ─── Helpers del plan ───
    @property
    def _numero_clases(self):
        return self.membresia.numero_clases if self.membresia else None

    @property
    def _dias_acceso(self):
        if self.membresia and self.membresia.dias_acceso:
            return self.membresia.dias_acceso
        return [0, 1, 2, 3, 4]

    @property
    def _fecha_fin_base(self):
        n = self._numero_clases
        if n and n > 0:
            # La membresía dura hasta la fecha de la última clase del paquete.
            return _nth_dia_semana(self.fecha_inicio, n, self._dias_acceso)
        return self.fecha_fin  # modo por días de calendario

    @property
    def fecha_fin_efectiva(self):
        return self._fecha_fin_base + timedelta(days=self.dias_extension or 0)

    @property
    def dias_restantes(self):
        return (self.fecha_fin_efectiva - date.today()).days

    # ─── Clases ───
    @property
    def clases_totales(self):
        n = self._numero_clases
        if n and n > 0:
            return n
        # Fallback: contar días habilitados dentro de la duración.
        return _contar_dias_semana(self.fecha_inicio, self._fecha_fin_base - timedelta(days=1), self._dias_acceso)

    @property
    def clases_transcurridas(self):
        hoy = date.today()
        # Hoy cuenta de inmediato; si la membresía empieza en el futuro, aún no cuenta.
        if hoy < self.fecha_inicio:
            return 0
        usadas = _contar_dias_semana(self.fecha_inicio, hoy, self._dias_acceso)
        return min(usadas, self.clases_totales)

    @property
    def clases_restantes(self):
        return max(0, self.clases_totales - self.clases_transcurridas)

    @property
    def vigente(self):
        # Vigente mientras no pase la fecha de la última clase (incluye ese día).
        return self.activa and self.fecha_fin_efectiva >= date.today()

    @property
    def por_vencer(self):
        return self.vigente and self.dias_restantes <= DIAS_ALERTA_VENCIMIENTO

    @property
    def aviso_enviado(self):
        return self.aviso_vencimiento_para == self.fecha_fin_efectiva

    # ─── Dinero ───
    @property
    def total(self):
        if self.monto_total is not None:
            return float(self.monto_total)
        return float(self.membresia.precio) if self.membresia and self.membresia.precio is not None else 0.0

    @property
    def monto_pagado(self):
        return round(sum(float(p.monto or 0) for p in self.pagos), 2)

    @property
    def saldo_pendiente(self):
        return round(max(0.0, self.total - self.monto_pagado), 2)

    @property
    def pagada(self):
        return self.saldo_pendiente <= 0.009

    def to_dict(self):
        return {
            'id': self.id,
            'cliente_id': self.cliente_id,
            'membresia_id': self.membresia_id,
            'membresia_nombre': self.membresia.nombre if self.membresia else None,
            'precio': float(self.membresia.precio) if self.membresia and self.membresia.precio is not None else 0.0,
            'fecha_inicio': self.fecha_inicio.isoformat() if self.fecha_inicio else None,
            'fecha_fin': self._fecha_fin_base.isoformat() if self._fecha_fin_base else None,
            'fecha_fin_efectiva': self.fecha_fin_efectiva.isoformat(),
            'dias_extension': self.dias_extension or 0,
            'dias_restantes': self.dias_restantes,
            'vigente': self.vigente,
            'por_vencer': self.por_vencer,
            'aviso_enviado': self.aviso_enviado,
            'activa': self.activa,
            'total': self.total,
            'monto_pagado': self.monto_pagado,
            'saldo_pendiente': self.saldo_pendiente,
            'pagada': self.pagada,
            'clases_totales': self.clases_totales,
            'clases_transcurridas': self.clases_transcurridas,
            'clases_restantes': self.clases_restantes,
        }

    def __repr__(self):
        return f'<ClienteMembresia cliente={self.cliente_id} fin={self.fecha_fin}>'
