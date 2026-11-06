"""
Avisos de vencimiento de membresía.
Se avisa una sola vez por fecha de fin (ver ClienteMembresia.aviso_vencimiento_para),
así que se puede ejecutar cuantas veces se quiera sin repetir correos. Si un envío
falla, o el correo se carga tarde, sale en la siguiente pasada mientras siga vigente.
"""

from flask import current_app
from app.extensions import db
from app.models.cliente_membresia import ClienteMembresia
from app.models.contenido_publico import ContenidoPublico
from app.services.correo import correo_configurado, enviar_correo
from app.utils.helpers import registrar_actividad


def membresias_por_vencer():
    """Membresías de clientes activos dentro de la ventana de alerta, la más urgente primero."""
    membresias = [
        cm for cm in ClienteMembresia.query.filter_by(activa=True).all()
        if cm.por_vencer and cm.cliente.bucket == 'activo'
    ]
    return sorted(membresias, key=lambda cm: cm.dias_restantes)


def _cuando(dias):
    if dias <= 0:
        return 'vence hoy'
    if dias == 1:
        return 'vence mañana'
    return f'vence en {dias} días'


def _redactar(cm):
    contenido = ContenidoPublico.query.first()
    negocio = (contenido.nombre if contenido else '') or 'Zona Aquática'
    contacto = (contenido.whatsapp or contenido.telefono) if contenido else ''
    cuando = _cuando(cm.dias_restantes)
    fin = cm.fecha_fin_efectiva.strftime('%d/%m/%Y')
    plan = cm.membresia.nombre if cm.membresia else 'Membresía'

    asunto = f'La membresía de {cm.cliente.nombre_completo} {cuando}'
    lineas = [
        'Hola:',
        '',
        f'Te recordamos que la membresía de {cm.cliente.nombre_completo} en {negocio} {cuando}.',
        '',
        f'Plan: {plan}',
        f'Último día: {fin}',
        '',
        'Para continuar sin interrupciones, puedes renovarla en recepción antes de esa fecha.',
    ]
    if contacto:
        lineas.append(f'Si tienes dudas, escríbenos o llámanos al {contacto}.')
    lineas += ['', negocio]
    return asunto, '\n'.join(lineas)


def enviar_avisos_vencimiento():
    """Envía los avisos pendientes y devuelve un resumen de lo ocurrido."""
    resumen = {'enviados': 0, 'fallidos': 0, 'sin_correo': 0}
    if not correo_configurado():
        current_app.logger.warning('Avisos de vencimiento: el correo no está configurado')
        return resumen

    for cm in membresias_por_vencer():
        if cm.aviso_enviado:
            continue
        cliente = cm.cliente
        if not cliente.correo_referencia:
            resumen['sin_correo'] += 1
            continue
        asunto, texto = _redactar(cm)
        try:
            enviar_correo(cliente.correo_referencia, asunto, texto)
        except Exception as e:
            current_app.logger.error(f'No se pudo avisar a {cliente.nombre_completo}: {e}')
            resumen['fallidos'] += 1
            continue
        # Se confirma uno por uno: si el proceso se corta, nadie recibe el aviso dos veces.
        cm.aviso_vencimiento_para = cm.fecha_fin_efectiva
        registrar_actividad('editar', 'clientes',
                            f'Envió aviso de vencimiento de {cliente.nombre_completo}')
        db.session.commit()
        resumen['enviados'] += 1
    return resumen
