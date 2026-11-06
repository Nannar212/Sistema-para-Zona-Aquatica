"""
Configuración de Flask — Sistema de Gestión Zona Aquática
Clases de configuración para desarrollo, testing y producción.
"""

import os
from datetime import timedelta
from dotenv import load_dotenv

load_dotenv()


class Config:
    """Configuración base compartida por todos los entornos."""

    # --- Flask ---
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev_secret_key_cambiar')

    # --- Base de datos ---
    SQLALCHEMY_DATABASE_URI = os.getenv(
        'DATABASE_URL',
        'postgresql://piscina_admin:piscina_pass_cambiar@localhost:5432/piscina_db'
    )
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    SQLALCHEMY_ENGINE_OPTIONS = {
        'pool_size': 10,
        'pool_recycle': 300,
        'pool_pre_ping': True,
    }

    # --- JWT ---
    JWT_SECRET_KEY = os.getenv('JWT_SECRET_KEY', 'dev_jwt_secret_cambiar')
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=8)   # jornada laboral
    JWT_REFRESH_TOKEN_EXPIRES = timedelta(days=30)
    JWT_TOKEN_LOCATION = ['headers']

    # --- Uploads (fotos de la web pública, recibos, etc.) ---
    UPLOAD_FOLDER = os.getenv('UPLOAD_FOLDER', os.path.join(os.path.dirname(__file__), 'uploads'))
    MAX_CONTENT_LENGTH = 60 * 1024 * 1024  # 60 MB (permite modelos 3D .glb)

    # --- Admin inicial ---
    SEED_ADMIN_USERNAME = os.getenv('SEED_ADMIN_USERNAME', 'admin')
    SEED_ADMIN_PASSWORD = os.getenv('SEED_ADMIN_PASSWORD', 'Admin123!')

    # --- Correo (avisos de vencimiento) ---
    # Con BREVO_API_KEY se envía por HTTPS; si no, por SMTP. Varios hostings
    # bloquean los puertos SMTP, así que la API es la opción que funciona en todos.
    CORREO_REMITENTE = os.getenv('CORREO_REMITENTE', '')
    BREVO_API_KEY = os.getenv('BREVO_API_KEY', '')
    SMTP_HOST = os.getenv('SMTP_HOST', '')
    SMTP_PORT = int(os.getenv('SMTP_PORT') or 587)
    SMTP_USUARIO = os.getenv('SMTP_USUARIO', '')
    SMTP_PASSWORD = os.getenv('SMTP_PASSWORD', '')
    AVISOS_HORA = int(os.getenv('AVISOS_HORA') or 9)  # hora local desde la que se envían

    # --- CORS ---
    CORS_ORIGINS = os.getenv('CORS_ORIGINS', 'http://localhost:5173').split(',')


class DevelopmentConfig(Config):
    DEBUG = True
    FLASK_ENV = 'development'


class TestingConfig(Config):
    TESTING = True
    SQLALCHEMY_DATABASE_URI = 'sqlite:///:memory:'


class ProductionConfig(Config):
    DEBUG = False
    FLASK_ENV = 'production'
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=4)


config = {
    'development': DevelopmentConfig,
    'testing': TestingConfig,
    'production': ProductionConfig,
    'default': DevelopmentConfig,
}
