# Ari Labs · Citas

Modelo 1 de Ari Labs: app de citas para quien trabaja yendo a casas. Primer cliente: un rezador de Mérida (rezos, novenas, velorios y misas).

## Qué hace

- Escribes la cita como salga ("sábado 3pm, colonia Chuburná, doña Mari") y la app la ordena: fecha, hora, lugar, cliente, tipo, teléfono, precio y anticipo. Lo que no entienda te lo pregunta antes de guardar.
- Muestra lo de hoy y lo de los próximos 7 días, con botón para copiar el aviso o mandarlo por WhatsApp.
- Botón de Maps para llegar a cada cita.
- Mover una cita y sacar el mensaje para avisar a los demás clientes de ese día. Cancelar sin borrar, anotando qué pasó con el anticipo.
- Ordena las citas del día por zona (Centro, Norte, Oriente, Sur, Poniente) para no cruzar la ciudad.
- Funciona sin internet: no pide permiso de internet y guarda todo en el teléfono.

## Supuestos (confirmar con el cliente)

- La usa una sola persona, en Android, con letra grande.
- Las citas se entienden con reglas, sin IA. Las zonas salen de una lista de colonias de Mérida hecha a mano (`Colonias.kt`); si no conoce la colonia, se escoge a mano.
- Una hora sin "de la mañana/tarde" de 1 a 7 se toma por la tarde; de 8 a 11 se pregunta.

## Cómo está hecha

- Kotlin + Jetpack Compose, una sola pantalla (`MainActivity`) con vistas: Agenda, Nueva cita, Revisar, Detalle, Avisos y Ajustes.
- `lector/LectorDeCitas.kt`: entiende el texto libre con reglas. `lector/Colonias.kt`: colonias de Mérida y su zona.
- `datos/`: la cita, la agenda (hoy, próximos 7 días, orden por zona), los mensajes de WhatsApp y el guardado en un archivo JSON del teléfono.
- Diseño "agenda de papel": fondo crema, tinta azul, letra grande y botones de 56 dp.
- Pruebas en `app/src/test` (lector, agenda y mensajes).

## Estado

- 0.1.0 compilada en verde (Actions, run 41031138079): APK de prueba `ari-labs-citas-debug`.
- Simulación de 1000 usuarios: 0 % de errores sin avisar en todos los campos; 566 de 1000 citas entendidas completas sin preguntar nada (el resto pregunta lo que falta o es dudoso).
- Falta: probarla en teléfonos reales (el tío y 5 a 10 personas que trabajen a domicilio) y firmar una versión de entrega.
