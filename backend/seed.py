"""
Datos de ejemplo para Zona Aquática.
Ejecutar (una vez que los contenedores están arriba):
    docker compose exec backend python seed.py

Crea membresías, horarios y algunos clientes de muestra. Es idempotente:
si ya existen datos, no los duplica.
"""

from datetime import date, timedelta, datetime, time
from app import create_app
from app.extensions import db
from app.models.membresia import Membresia
from app.models.horario import Horario
from app.models.cliente import Cliente
from app.models.cliente_membresia import ClienteMembresia
from app.models.pago import Pago

app = create_app()


def seed():
    with app.app_context():
        # ─── Membresías ───
        if Membresia.query.count() == 0:
            membresias = [
                Membresia(nombre='Pase diario', descripcion='Acceso por un día',
                          duracion_dias=1, precio=20, dias_acceso=[0, 1, 2, 3, 4]),
                Membresia(nombre='Mensual', descripcion='Acceso de lunes a viernes por un mes',
                          duracion_dias=30, precio=200, dias_acceso=[0, 1, 2, 3, 4]),
                Membresia(nombre='Trimestral', descripcion='Tres meses de acceso lun-vie',
                          duracion_dias=90, precio=540, dias_acceso=[0, 1, 2, 3, 4]),
            ]
            db.session.add_all(membresias)
            db.session.commit()
            print(f'  ✓ {len(membresias)} membresías creadas')

        # ─── Horarios ───
        if Horario.query.count() == 0:
            horarios = [
                Horario(nombre='Mañana', hora_inicio=time(7, 0), hora_fin=time(9, 0),
                        cupo_maximo=30, dias_semana=[0, 1, 2, 3, 4], color='#185fa5'),
                Horario(nombre='Media mañana', hora_inicio=time(9, 0), hora_fin=time(11, 0),
                        cupo_maximo=30, dias_semana=[0, 1, 2, 3, 4], color='#57a0e0'),
                Horario(nombre='Tarde', hora_inicio=time(15, 0), hora_fin=time(17, 0),
                        cupo_maximo=30, dias_semana=[0, 1, 2, 3, 4], color='#ef9f27'),
                Horario(nombre='Tarde-noche', hora_inicio=time(17, 0), hora_fin=time(19, 0),
                        cupo_maximo=30, dias_semana=[0, 1, 2, 3, 4], color='#0c447c'),
            ]
            db.session.add_all(horarios)
            db.session.commit()
            print(f'  ✓ {len(horarios)} horarios creados')

        # ─── Clientes de ejemplo ───
        if Cliente.query.count() == 0:
            mensual = Membresia.query.filter_by(nombre='Mensual').first()
            manana = Horario.query.filter_by(nombre='Mañana').first()
            tarde = Horario.query.filter_by(nombre='Tarde').first()

            ejemplos = [
                ('Ana', 'Rojas', 'F', 24, '70011122', manana),
                ('Luis', 'Choque', 'M', 31, '70033344', manana),
                ('María', 'Fernández', 'F', 19, '70055566', tarde),
                ('Carlos', 'Vargas', 'M', 42, '70077788', tarde),
            ]
            for nombres, apellidos, sexo, edad, cel, horario in ejemplos:
                c = Cliente(nombres=nombres, apellidos=apellidos, sexo=sexo, edad=edad,
                            celular=cel, horario_id=horario.id if horario else None)
                db.session.add(c)
                db.session.flush()
                if mensual:
                    cm = ClienteMembresia(
                        cliente_id=c.id, membresia_id=mensual.id,
                        fecha_inicio=date.today(),
                        fecha_fin=date.today() + timedelta(days=mensual.duracion_dias),
                    )
                    db.session.add(cm)
                    db.session.flush()
                    db.session.add(Pago(cliente_id=c.id, cliente_membresia_id=cm.id,
                                        monto=mensual.precio, metodo='efectivo',
                                        monto_efectivo=mensual.precio, monto_qr=0,
                                        fecha=datetime.utcnow()))
            db.session.commit()
            print(f'  ✓ {len(ejemplos)} clientes de ejemplo creados')

        print('Seed completado.')


if __name__ == '__main__':
    print('Sembrando datos de ejemplo...')
    seed()
