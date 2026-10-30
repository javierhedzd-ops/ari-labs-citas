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

## Estado

En construcción. Aún no se compila (se compilará cuando todo esté listo).
