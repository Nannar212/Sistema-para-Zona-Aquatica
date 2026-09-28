"""
Contenido público — edición del contenido de la página pública. Solo administrador.
Incluye subida de imágenes (logo cuadrado auto-ajustado, imagen del hero y galería).
"""

import os
import secrets
from flask import Blueprint, request, jsonify, current_app
from app.extensions import db
from app.models.contenido_publico import ContenidoPublico
from app.utils.decorators import rol_requerido
from app.utils.helpers import registrar_actividad

contenido_bp = Blueprint('contenido', __name__)

CAMPOS_TEXTO = (
    'nombre', 'hero_badge', 'hero_titulo', 'hero_subtitulo', 'sobre_texto',
    'direccion', 'telefono', 'whatsapp', 'whatsapp_link', 'google_maps_link',
    'horario_texto', 'tiktok', 'instagram', 'youtube', 'facebook',
    'logo_url', 'hero_imagen_url', 'modelo_3d_url',
)
CAMPOS_JSON = ('destacados', 'estadisticas', 'galeria')
CAMPOS_BOOL = ('mostrar_planes', 'mostrar_horarios')

EXT_PERMITIDAS = {'png', 'jpg', 'jpeg', 'webp', 'gif'}


def _obtener():
    contenido = ContenidoPublico.query.first()
    if not contenido:
        contenido = ContenidoPublico()
        db.session.add(contenido)
        db.session.commit()
    return contenido


@contenido_bp.route('', methods=['GET'])
@rol_requerido('admin')
def obtener():
    return jsonify(_obtener().to_dict()), 200


@contenido_bp.route('', methods=['PUT'])
@rol_requerido('admin')
def actualizar():
    try:
        contenido = _obtener()
        data = request.get_json() or {}

        for campo in CAMPOS_TEXTO:
            if campo in data:
                setattr(contenido, campo, (data[campo] or '').strip())
        for campo in CAMPOS_JSON:
            if campo in data and isinstance(data[campo], list):
                setattr(contenido, campo, data[campo])
        for campo in CAMPOS_BOOL:
            if campo in data:
                setattr(contenido, campo, bool(data[campo]))

        registrar_actividad('editar', 'contenido', 'Actualizó el contenido de la página pública')
        db.session.commit()
        return jsonify({'mensaje': 'Contenido actualizado', 'contenido': contenido.to_dict()}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Error al actualizar contenido: {str(e)}'}), 500


@contenido_bp.route('/upload', methods=['POST'])
@rol_requerido('admin')
def subir_imagen():
    """
    Sube una imagen. Campo 'tipo': logo | hero | galeria.
    - logo: se recorta y ajusta a cuadrado automáticamente.
    - hero/galeria: se redimensiona conservando la proporción.
    Devuelve {url} para usar en el contenido.
    """
    try:
        from PIL import Image  # import diferido (solo cuando se sube)

        if 'file' not in request.files:
            return jsonify({'error': 'No se envió ningún archivo'}), 400
        archivo = request.files['file']
        tipo = (request.form.get('tipo') or 'galeria').lower()

        if not archivo or archivo.filename == '':
            return jsonify({'error': 'Archivo vacío'}), 400
        ext = archivo.filename.rsplit('.', 1)[-1].lower() if '.' in archivo.filename else ''
        if ext not in EXT_PERMITIDAS:
            return jsonify({'error': 'Formato no permitido (usa PNG, JPG, WEBP o GIF)'}), 400

        carpeta = os.path.join(current_app.config.get('UPLOAD_FOLDER', 'uploads'), 'publico')
        os.makedirs(carpeta, exist_ok=True)

        img = Image.open(archivo.stream)

        if tipo == 'logo':
            # Recortar al centro y ajustar a cuadrado (evita errores de proporción)
            img = img.convert('RGBA')
            w, h = img.size
            lado = min(w, h)
            izq = (w - lado) // 2
            arr = (h - lado) // 2
            img = img.crop((izq, arr, izq + lado, arr + lado)).resize((320, 320))
            nombre = f'logo_{secrets.token_hex(6)}.png'
            img.save(os.path.join(carpeta, nombre), 'PNG')
        else:
            # hero o galería: conservar proporción, ancho máximo 1600px
            img = img.convert('RGB')
            if img.width > 1600:
                ratio = 1600 / img.width
                img = img.resize((1600, int(img.height * ratio)))
            prefijo = 'hero' if tipo == 'hero' else 'galeria'
            nombre = f'{prefijo}_{secrets.token_hex(6)}.jpg'
            img.save(os.path.join(carpeta, nombre), 'JPEG', quality=85)

        url = f'/uploads/publico/{nombre}'
        registrar_actividad('editar', 'contenido', f'Subió una imagen ({tipo})')
        return jsonify({'url': url}), 201
    except Exception as e:
        return jsonify({'error': f'Error al subir imagen: {str(e)}'}), 500


@contenido_bp.route('/upload-modelo', methods=['POST'])
@rol_requerido('admin')
def subir_modelo():
    """Sube el modelo 3D del recorrido (.glb o .gltf). Devuelve {url}."""
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'No se envió ningún archivo'}), 400
        archivo = request.files['file']
        if not archivo or archivo.filename == '':
            return jsonify({'error': 'Archivo vacío'}), 400
        ext = archivo.filename.rsplit('.', 1)[-1].lower() if '.' in archivo.filename else ''
        if ext not in {'glb', 'gltf'}:
            return jsonify({'error': 'Formato no permitido. Usa un archivo .glb (recomendado) o .gltf'}), 400

        carpeta = os.path.join(current_app.config.get('UPLOAD_FOLDER', 'uploads'), 'publico')
        os.makedirs(carpeta, exist_ok=True)
        nombre = f'modelo_{secrets.token_hex(6)}.{ext}'
        archivo.save(os.path.join(carpeta, nombre))

        url = f'/uploads/publico/{nombre}'
        registrar_actividad('editar', 'contenido', 'Subió el modelo 3D de las instalaciones')
        return jsonify({'url': url}), 201
    except Exception as e:
        return jsonify({'error': f'Error al subir el modelo 3D: {str(e)}'}), 500
