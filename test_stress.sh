#!/bin/bash
SECONDS=0
LOG_FILE="/tmp/resultado_test.log"
> "$LOG_FILE"  # Vacía el archivo sin meter texto
echo "[$(date)] Iniciando test: 1000 requests x 4 archivos"

for ((req=1; req<=1000; req++)); do
  curl -s -o /dev/null -w "%{http_code}\n" --max-time 180 -X POST http://localhost:8080/api/documentos/carga-masiva \
    -F "files=@/tmp/vt$(( (req % 100) + 1 )).txt" \
    -F "files=@/tmp/vt$(( ((req+1) % 100) + 1 )).txt" \
    -F "files=@/tmp/vt$(( ((req+2) % 100) + 1 )).txt" \
    -F "files=@/tmp/vt$(( ((req+3) % 100) + 1 )).txt" >> "$LOG_FILE" &

  if (( req % 50 == 0 )); then
    echo "Lanzados $req requests..."
    wait
  fi
done

wait
echo "========================================="
echo "TIEMPO TOTAL DEL TEST: $SECONDS segundos"
echo "========================================="
echo "Resumen de respuestas HTTP:"
grep -E '^[0-9]{3}$' "$LOG_FILE" | sort | uniq -c