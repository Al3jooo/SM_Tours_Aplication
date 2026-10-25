#!/usr/bin/env bash
# Samaria360 - demostración de concurrencia por unidad (CU Reservar servicio, decisiones D1 y D3).
#
# Usa las dos unidades de Hotel Casa Tayrona (Doble y Suite familiar, migraciones M001 y M002):
#   1. busca la primera noche, desde mañana, en la que las dos unidades tienen todos sus cupos;
#   2. reserva todas las habitaciones de cada unidad menos una;
#   3. lanza N solicitudes por unidad al mismo tiempo (2N en total), cada una por la última habitación.
# Resultado esperado: exactamente un 201 por unidad; las demás, 409 CUPO_AGOTADO; las dos unidades quedan en 0.
# Cada ejecución agota una noche (ver docs/demo/reservas/README.md).
#
# Uso (con el sistema levantado: docker compose up -d --build):
#   CORREO=<turista> CONTRASENA=<contraseña> bash docs/demo/reservas/concurrencia.sh [N]
# Requisitos: curl y Node.js (el mismo del frontend; se usa para leer JSON, porque jq no viene en Windows).

set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
N="${1:-10}"
: "${CORREO:?Defina CORREO (un turista; ver README.md, «Arranque en un comando»)}"
: "${CONTRASENA:?Defina CONTRASENA}"
command -v curl >/dev/null || { echo "Falta curl" >&2; exit 1; }
command -v node >/dev/null || { echo "Falta Node.js" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Evalúa una expresión de JavaScript sobre el JSON de la entrada estándar (disponible como «j»).
json() {
  node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{const j=JSON.parse(d);const r=($1);
    console.log(typeof r==='string'||typeof r==='number'?r:JSON.stringify(r))})"
}
# Fecha de hoy en Santa Marta y suma de días (aaaa-mm-dd).
hoy() { node -e "console.log(new Intl.DateTimeFormat('en-CA',{timeZone:'America/Bogota'}).format(new Date()))"; }
sumar() {
  node -e "const d=new Date(process.argv[1]+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+Number(process.argv[2]));
    console.log(d.toISOString().slice(0,10))" "$1" "$2"
}

echo "== Inicio de sesión en $BASE"
node -e "console.log(JSON.stringify({correo:process.argv[1],contrasena:process.argv[2]}))" "$CORREO" "$CONTRASENA" \
  > "$TMP/login.json"
TOKEN=$(curl -sf -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
  --data-binary "@$TMP/login.json" | json 'j.token')

echo "== Hotel Casa Tayrona y sus unidades (detalle de ms-catalog)"
HOTEL=$(curl -sf "$BASE/api/v1/catalogo/oferta?categoria=Alojamiento&texto=Casa%20Tayrona" \
  | json 'j.elementos.find(e=>e.nombre==="Hotel Casa Tayrona").id')
curl -sf "$BASE/api/v1/catalogo/servicios/$HOTEL" > "$TMP/hotel.json"
export DOBLE SUITE
DOBLE=$(json 'j.detalleCategoria.unidades.find(u=>u.nombre==="Doble").idUnidad' < "$TMP/hotel.json")
SUITE=$(json 'j.detalleCategoria.unidades.find(u=>u.nombre==="Suite familiar").idUnidad' < "$TMP/hotel.json")
PRECIO_DOBLE=$(json 'j.detalleCategoria.unidades.find(u=>u.nombre==="Doble").precioNoche' < "$TMP/hotel.json")
PRECIO_SUITE=$(json 'j.detalleCategoria.unidades.find(u=>u.nombre==="Suite familiar").precioNoche' < "$TMP/hotel.json")

echo "== Noche con las dos unidades completas (inventario de 60 días, materializado bajo demanda)"
HOY=$(hoy)
curl -sf "$BASE/api/v1/reservas/disponibilidad?servicio=$HOTEL&desde=$(sumar "$HOY" 1)&hasta=$(sumar "$HOY" 59)" \
  > "$TMP/disponibilidad.json"
NOCHE=$(json '(()=>{const u=id=>(j.unidades.find(x=>x.idUnidad===id)||{noches:[]}).noches;
  const llena=(n)=>n.cuposDisponibles===n.cuposTotales;
  const f=u(process.env.DOBLE).find(n=>llena(n)&&u(process.env.SUITE).some(s=>s.fecha===n.fecha&&llena(s)));
  return f?f.fecha:""})()' < "$TMP/disponibilidad.json")
if [ -z "$NOCHE" ]; then
  echo "No quedan noches con las dos unidades completas en los próximos 59 días." >&2
  exit 1
fi
SALIDA=$(sumar "$NOCHE" 1)
TOTAL_DOBLE=$(json "j.unidades.find(x=>x.idUnidad==='$DOBLE').noches.find(n=>n.fecha==='$NOCHE').cuposTotales" \
  < "$TMP/disponibilidad.json")
TOTAL_SUITE=$(json "j.unidades.find(x=>x.idUnidad==='$SUITE').noches.find(n=>n.fecha==='$NOCHE').cuposTotales" \
  < "$TMP/disponibilidad.json")
echo "Noche usada: $NOCHE (salida $SALIDA). Doble: $TOTAL_DOBLE habitaciones; Suite familiar: $TOTAL_SUITE."

# Cuerpo de una reserva de una noche: unidad, habitaciones y precio por noche.
cuerpo() {
  node -e "console.log(JSON.stringify({idServicio:process.argv[1],idUnidad:process.argv[2],fechaEntrada:process.argv[3],
    fechaSalida:process.argv[4],cantidad:Number(process.argv[5]),personas:1,
    precioTotalEsperado:Number(process.argv[6])*Number(process.argv[5])}))" \
    "$HOTEL" "$1" "$NOCHE" "$SALIDA" "$2" "$3"
}
reservar() { # archivo-cuerpo clave archivo-respuesta → código HTTP
  curl -s -o "$3" -w '%{http_code}\n' -X POST "$BASE/api/v1/reservas" -H "Authorization: Bearer $TOKEN" \
    -H "Idempotency-Key: $2" -H 'Content-Type: application/json' --data-binary "@$1"
}

echo "== Se reservan todas las habitaciones menos una de cada unidad"
cuerpo "$DOBLE" $((TOTAL_DOBLE - 1)) "$PRECIO_DOBLE" > "$TMP/previa-doble.json"
cuerpo "$SUITE" $((TOTAL_SUITE - 1)) "$PRECIO_SUITE" > "$TMP/previa-suite.json"
echo "Doble ($((TOTAL_DOBLE - 1)) habitaciones): $(reservar "$TMP/previa-doble.json" "$(node -e 'console.log(crypto.randomUUID())')" /dev/null)"
echo "Suite familiar ($((TOTAL_SUITE - 1)) habitaciones): $(reservar "$TMP/previa-suite.json" "$(node -e 'console.log(crypto.randomUUID())')" /dev/null)"

echo "== $N solicitudes por unidad, al mismo tiempo, por la última habitación"
cuerpo "$DOBLE" 1 "$PRECIO_DOBLE" > "$TMP/ultima-doble.json"
cuerpo "$SUITE" 1 "$PRECIO_SUITE" > "$TMP/ultima-suite.json"
# Claves preparadas antes de lanzar, para que las solicitudes salgan juntas.
mapfile -t CLAVES < <(node -e "for(let i=0;i<2*Number(process.argv[1]);i++)console.log(crypto.randomUUID())" "$N")
for i in $(seq 1 "$N"); do
  reservar "$TMP/ultima-doble.json" "${CLAVES[$((2 * i - 2))]}" "$TMP/doble-$i.json" > "$TMP/doble-$i.codigo" &
  reservar "$TMP/ultima-suite.json" "${CLAVES[$((2 * i - 1))]}" "$TMP/suite-$i.json" > "$TMP/suite-$i.codigo" &
done
wait

contar() { cat "$TMP"/$1-*.codigo | grep -c "^$2$" || true; }
agotados() { grep -l '"CUPO_AGOTADO"' "$TMP"/$1-*.json 2>/dev/null | wc -l | tr -d ' '; }
curl -sf "$BASE/api/v1/reservas/disponibilidad?servicio=$HOTEL&desde=$NOCHE&hasta=$NOCHE" > "$TMP/final.json"
QUEDAN_DOBLE=$(json "j.unidades.find(x=>x.idUnidad==='$DOBLE').noches[0].cuposDisponibles" < "$TMP/final.json")
QUEDAN_SUITE=$(json "j.unidades.find(x=>x.idUnidad==='$SUITE').noches[0].cuposDisponibles" < "$TMP/final.json")

echo
echo "Resultado ($NOCHE):"
echo "  Doble:          $(contar doble 201) × 201, $(contar doble 409) × 409 ($(agotados doble) CUPO_AGOTADO); cupos al final: $QUEDAN_DOBLE de $TOTAL_DOBLE"
echo "  Suite familiar: $(contar suite 201) × 201, $(contar suite 409) × 409 ($(agotados suite) CUPO_AGOTADO); cupos al final: $QUEDAN_SUITE de $TOTAL_SUITE"

if [ "$(contar doble 201)" = "1" ] && [ "$(contar suite 201)" = "1" ] && [ "$QUEDAN_DOBLE" = "0" ] && [ "$QUEDAN_SUITE" = "0" ]; then
  echo "OK: un solo 201 por unidad y las dos unidades en 0."
else
  echo "FALLA: se esperaba un solo 201 por unidad y las dos unidades en 0." >&2
  exit 1
fi
