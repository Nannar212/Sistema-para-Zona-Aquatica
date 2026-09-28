"""
ContenidoPublico — contenido editable de la página pública (una sola fila).
El administrador lo edita desde el panel; la web pública lo consume sin login.
Las listas (destacados, estadísticas, galería) se guardan como JSON.
"""

from datetime import datetime
from app.extensions import db


def _destacados_default():
    return [
        {'icono': 'temperature', 'titulo': 'Agua temperada', 'texto': 'Cómoda todo el año'},
        {'icono': 'kid', 'titulo': 'Zona para niños', 'texto': 'Piscina poco profunda'},
        {'icono': 'lifebuoy', 'titulo': 'Salvavidas', 'texto': 'Personal atento'},
        {'icono': 'droplet', 'titulo': 'Agua limpia', 'texto': 'Cloración diaria'},
    ]


def _estadisticas_default():
    return [
        {'valor': '28°C', 'etiqueta': 'Agua temperada'},
        {'valor': 'Lun–Vie', 'etiqueta': 'Días de atención'},
        {'valor': '4', 'etiqueta': 'Turnos diarios'},
        {'valor': '+500', 'etiqueta': 'Nadadores felices'},
    ]


class ContenidoPublico(db.Model):
    __tablename__ = 'contenido_publico'

    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(120), default='Zona Aquática')
    hero_badge = db.Column(db.String(120), default='Quillacollo · Cochabamba')
    hero_titulo = db.Column(db.String(200), default='Refréscate en Zona Aquática')
    hero_subtitulo = db.Column(db.Text, default='Piscina temperada para toda la familia, abierta de lunes a viernes.')
    sobre_texto = db.Column(db.Text, default='Un espacio limpio y seguro para nadar, aprender y disfrutar en familia.')

    # Imágenes (URLs servidas desde /uploads)
    logo_url = db.Column(db.Text, default='')
    hero_imagen_url = db.Column(db.Text, default='')
    modelo_3d_url = db.Column(db.Text, default='')  # archivo .glb del recorrido 3D

    destacados = db.Column(db.JSON, default=_destacados_default)
    estadisticas = db.Column(db.JSON, default=_estadisticas_default)
    galeria = db.Column(db.JSON, default=list)  # lista de URLs de imágenes

    # Contacto
    direccion = db.Column(db.String(200), default='Av. Blanco Galindo, Quillacollo')
    telefono = db.Column(db.String(255), default='')
    whatsapp = db.Column(db.String(255), default='')        # número (ej: 78820899)
    whatsapp_link = db.Column(db.Text, default='')          # enlace directo (ej: https://wa.me/591...)
    google_maps_link = db.Column(db.Text, default='')       # enlace a Google Maps
    horario_texto = db.Column(db.String(120), default='Lunes a viernes · 07:00–19:00')

    # Redes sociales
    tiktok = db.Column(db.Text, default='')
    instagram = db.Column(db.Text, default='')
    youtube = db.Column(db.Text, default='')
    facebook = db.Column(db.Text, default='')

    mostrar_planes = db.Column(db.Boolean, default=True)
    mostrar_horarios = db.Column(db.Boolean, default=True)

    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self):
        return {
            'nombre': self.nombre,
            'hero_badge': self.hero_badge,
            'hero_titulo': self.hero_titulo,
            'hero_subtitulo': self.hero_subtitulo,
            'sobre_texto': self.sobre_texto,
            'logo_url': self.logo_url or '',
            'hero_imagen_url': self.hero_imagen_url or '',
            'modelo_3d_url': self.modelo_3d_url or '',
            'destacados': self.destacados or [],
            'estadisticas': self.estadisticas or [],
            'galeria': self.galeria or [],
            'direccion': self.direccion,
            'telefono': self.telefono or '',
            'whatsapp': self.whatsapp or '',
            'whatsapp_link': self.whatsapp_link or '',
            'google_maps_link': self.google_maps_link or '',
            'horario_texto': self.horario_texto,
            'tiktok': self.tiktok or '',
            'instagram': self.instagram or '',
            'youtube': self.youtube or '',
            'facebook': self.facebook or '',
            'mostrar_planes': self.mostrar_planes,
            'mostrar_horarios': self.mostrar_horarios,
        }
