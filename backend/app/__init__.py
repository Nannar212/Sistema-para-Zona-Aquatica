"""
App Factory — Sistema de Gestion Zona Aquatica
Crea y configura la instancia de Flask (patron factory).
"""

import os
from flask import Flask, jsonify, send_from_directory
from config import config
from app.extensions import db, migrate, jwt, cors


def create_app(config_name=None):
    if config_name is None:
        config_name = os.getenv('FLASK_ENV', 'development')

    app = Flask(__name__)
    app.url_map.strict_slashes = False
    app.config.from_object(config.get(config_name, config['default']))

    # Extensiones
    db.init_app(app)
    migrate.init_app(app, db)
    jwt.init_app(app)
    cors.init_app(app, resources={
        r"/api/*": {"origins": app.config.get('CORS_ORIGINS', ['http://localhost:5173'])}
    })

    # Crear esquema y sembrar datos base
    with app.app_context():
        from app import models  # noqa: F401
        db.create_all()
        _agregar_columnas_faltantes(app)
        _seed_admin_inicial(app)
        _seed_contenido_publico(app)
        _completar_numero_clases(app)

    _registrar_blueprints(app)
    _configurar_jwt()
    _registrar_errores(app)

    os.makedirs(app.config.get('UPLOAD_FOLDER', 'uploads'), exist_ok=True)

    @app.route('/api/health')
    def health():
        return jsonify({'status': 'ok', 'servicio': 'Zona Aquatica API'}), 200

    @app.route('/uploads/<path:filename>')
    def serve_upload(filename):
        return send_from_directory(app.config.get('UPLOAD_FOLDER', 'uploads'), filename)

    return app


def _registrar_blueprints(app):
    from app.routes.auth import auth_bp
    from app.routes.personal import personal_bp
    from app.routes.clientes import clientes_bp
    from app.routes.membresias import membresias_bp
    from app.routes.horarios import horarios_bp
    from app.routes.asistencias import asistencias_bp
    from app.routes.pagos import pagos_bp
    from app.routes.auditoria import auditoria_bp
    from app.routes.dashboard import dashboard_bp
    from app.routes.gastos import gastos_bp
    from app.routes.finanzas import finanzas_bp
    from app.routes.contenido import contenido_bp
    from app.routes.publico import publico_bp

    app.register_blueprint(auth_bp, url_prefix='/api/auth')
    app.register_blueprint(personal_bp, url_prefix='/api/personal')
    app.register_blueprint(clientes_bp, url_prefix='/api/clientes')
    app.register_blueprint(membresias_bp, url_prefix='/api/membresias')
    app.register_blueprint(horarios_bp, url_prefix='/api/horarios')
    app.register_blueprint(asistencias_bp, url_prefix='/api/asistencias')
    app.register_blueprint(pagos_bp, url_prefix='/api/pagos')
    app.register_blueprint(auditoria_bp, url_prefix='/api/auditoria')
    app.register_blueprint(dashboard_bp, url_prefix='/api/dashboard')
    app.register_blueprint(gastos_bp, url_prefix='/api/gastos')
    app.register_blueprint(finanzas_bp, url_prefix='/api/finanzas')
    app.register_blueprint(contenido_bp, url_prefix='/api/contenido')
    app.register_blueprint(publico_bp, url_prefix='/api/publico')


def _configurar_jwt():
    @jwt.expired_token_loader
    def expired(jwt_header, jwt_payload):
        return jsonify({'error': 'Tu sesion expiro. Inicia sesion nuevamente.'}), 401

    @jwt.invalid_token_loader
    def invalid(error):
        return jsonify({'error': 'Token invalido.'}), 401

    @jwt.unauthorized_loader
    def missing(error):
        return jsonify({'error': 'Se requiere iniciar sesion.'}), 401


def _registrar_errores(app):
    @app.errorhandler(404)
    def not_found(error):
        return jsonify({'error': 'Recurso no encontrado'}), 404

    @app.errorhandler(405)
    def method_not_allowed(error):
        return jsonify({'error': 'Metodo no permitido'}), 405

    @app.errorhandler(500)
    def internal(error):
        db.session.rollback()
        return jsonify({'error': 'Error interno del servidor'}), 500


def _seed_admin_inicial(app):
    """Crea un administrador inicial si no existe ningun usuario."""
    from app.models.usuario import Usuario
    try:
        if Usuario.query.count() == 0:
            username = app.config.get('SEED_ADMIN_USERNAME', 'admin')
            password = app.config.get('SEED_ADMIN_PASSWORD', 'Admin123!')
            admin = Usuario(
                nombres='Administrador',
                apellidos='Zona Aquatica',
                ci='0000000',
                username=username,
                edad=None,
                rol='admin',
                es_password_temporal=False,
                activo=True,
            )
            admin.set_password(password)
            db.session.add(admin)
            db.session.commit()
            app.logger.info(f'Admin inicial creado: {username} / {password}')
    except Exception as e:
        db.session.rollback()
        app.logger.warning(f'No se pudo crear el admin inicial: {e}')


def _seed_contenido_publico(app):
    """Crea la fila de contenido publico si no existe."""
    from app.models.contenido_publico import ContenidoPublico
    try:
        if ContenidoPublico.query.count() == 0:
            db.session.add(ContenidoPublico())
            db.session.commit()
            app.logger.info('Contenido publico inicial creado')
    except Exception as e:
        db.session.rollback()
        app.logger.warning(f'No se pudo crear el contenido publico: {e}')


def _completar_numero_clases(app):
    """
    Completa numero_clases en planes que no lo tengan, tomando el número que
    aparezca en el nombre (ej: "Vacacional 9 Clases" -> 9). Best-effort:
    el admin siempre puede ajustarlo en el editor de membresías.
    """
    import re
    from app.models.membresia import Membresia
    try:
        pendientes = Membresia.query.filter(Membresia.numero_clases.is_(None)).all()
        for m in pendientes:
            match = re.search(r'\d+', m.nombre or '')
            if match:
                m.numero_clases = int(match.group())
        if pendientes:
            db.session.commit()
    except Exception as e:
        db.session.rollback()
        app.logger.warning(f'No se pudo completar numero_clases: {e}')


def _agregar_columnas_faltantes(app):
    """
    Agrega columnas nuevas a tablas ya existentes (db.create_all no altera tablas).
    Necesario si la base fue creada con una version anterior del sistema.
    Solo aplica en PostgreSQL.
    """
    if not str(db.engine.url).startswith('postgresql'):
        return
    alters = [
        ('clientes_membresias', 'monto_total',
         'ALTER TABLE clientes_membresias ADD COLUMN monto_total NUMERIC(10,2)'),
        ('pagos', 'concepto',
         "ALTER TABLE pagos ADD COLUMN concepto VARCHAR(30) DEFAULT 'pago'"),
        ('contenido_publico', 'logo_url', "ALTER TABLE contenido_publico ADD COLUMN logo_url TEXT DEFAULT ''"),
        ('contenido_publico', 'hero_imagen_url', "ALTER TABLE contenido_publico ADD COLUMN hero_imagen_url TEXT DEFAULT ''"),
        ('contenido_publico', 'whatsapp_link', "ALTER TABLE contenido_publico ADD COLUMN whatsapp_link TEXT DEFAULT ''"),
        ('contenido_publico', 'google_maps_link', "ALTER TABLE contenido_publico ADD COLUMN google_maps_link TEXT DEFAULT ''"),
        ('contenido_publico', 'tiktok', "ALTER TABLE contenido_publico ADD COLUMN tiktok TEXT DEFAULT ''"),
        ('contenido_publico', 'instagram', "ALTER TABLE contenido_publico ADD COLUMN instagram TEXT DEFAULT ''"),
        ('contenido_publico', 'youtube', "ALTER TABLE contenido_publico ADD COLUMN youtube TEXT DEFAULT ''"),
        ('contenido_publico', 'facebook', "ALTER TABLE contenido_publico ADD COLUMN facebook TEXT DEFAULT ''"),
        ('clientes', 'estado', "ALTER TABLE clientes ADD COLUMN estado VARCHAR(20) DEFAULT 'activo'"),
        ('clientes', 'motivo_estado', "ALTER TABLE clientes ADD COLUMN motivo_estado TEXT DEFAULT ''"),
        ('membresias', 'numero_clases', "ALTER TABLE membresias ADD COLUMN numero_clases INTEGER"),
        ('clientes', 'correo_referencia', "ALTER TABLE clientes ADD COLUMN correo_referencia VARCHAR(160) DEFAULT ''"),
        ('clientes_membresias', 'aviso_vencimiento_para', "ALTER TABLE clientes_membresias ADD COLUMN aviso_vencimiento_para DATE"),
        ('contenido_publico', 'modelo_3d_url', "ALTER TABLE contenido_publico ADD COLUMN modelo_3d_url TEXT DEFAULT ''"),
    ]
    for tabla, columna, sql in alters:
        try:
            existe = db.session.execute(db.text(
                "SELECT 1 FROM information_schema.columns "
                "WHERE table_name = :t AND column_name = :c"
            ), {'t': tabla, 'c': columna}).fetchone()
            if existe:
                continue
            db.session.execute(db.text(sql))
            db.session.commit()
            app.logger.info(f'Columna {columna} agregada a {tabla}')
        except Exception:
            db.session.rollback()

    # Ampliar columnas / migrar datos existentes
    otros = [
        "ALTER TABLE contenido_publico ALTER COLUMN whatsapp TYPE VARCHAR(255)",
        "ALTER TABLE contenido_publico ALTER COLUMN telefono TYPE VARCHAR(255)",
        # Clientes dados de baja con el esquema anterior (activo=false) -> eliminados
        "UPDATE clientes SET estado='eliminado' WHERE activo = false AND (estado IS NULL OR estado='activo')",
    ]
    for sql in otros:
        try:
            db.session.execute(db.text(sql))
            db.session.commit()
        except Exception:
            db.session.rollback()
