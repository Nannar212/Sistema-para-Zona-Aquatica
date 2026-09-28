"""
Punto de entrada del backend — Sistema Zona Aquática

Ejecutar:
    python run.py                (desarrollo)
    docker compose up backend    (con Docker)
"""

import os
from app import create_app
from app.extensions import db

app = create_app()


if __name__ == '__main__':
    # Railway/otros hosts pasan el puerto por la variable PORT.
    port = int(os.getenv('PORT', 5000))
    app.run(
        host='0.0.0.0',
        port=port,
        debug=app.config.get('DEBUG', True),
    )
