"""
Notificador — envía los avisos de vencimiento de membresía por correo.
Corre como un proceso aparte del servidor web: gunicorn levanta varios workers
y cada uno enviaría los mismos correos.

Ejecutar:
    python notificador.py            (revisa cada hora, desde AVISOS_HORA)
    python notificador.py --una-vez  (una sola pasada, para probar o usar con cron)
"""

import sys
import time
from datetime import datetime
from app import create_app
from app.extensions import db
from app.services.avisos_vencimiento import enviar_avisos_vencimiento

SEGUNDOS_ENTRE_PASADAS = 3600


def pasada(app):
    with app.app_context():
        try:
            resumen = enviar_avisos_vencimiento()
            print(f'[{datetime.now():%Y-%m-%d %H:%M}] Avisos de vencimiento: {resumen}', flush=True)
        except Exception as e:
            db.session.rollback()
            print(f'[{datetime.now():%Y-%m-%d %H:%M}] Error en los avisos: {e}', flush=True)


def main():
    app = create_app()
    if '--una-vez' in sys.argv:
        pasada(app)
        return
    while True:
        if datetime.now().hour >= app.config['AVISOS_HORA']:
            pasada(app)
        time.sleep(SEGUNDOS_ENTRE_PASADAS)


if __name__ == '__main__':
    main()
