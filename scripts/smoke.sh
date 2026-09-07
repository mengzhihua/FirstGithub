#!/usr/bin/env bash
# End-to-end API smoke test: ASN -> receive -> putaway -> ship order -> allocate -> pick -> ship -> count.
# Usage: scripts/smoke.sh [base_url]   (default http://localhost:8080)
set -euo pipefail
BASE="${1:-http://localhost:8080}/api"
J='Content-Type: application/json'

need() { command -v "$1" >/dev/null || { echo "missing $1"; exit 1; }; }
need curl; need jq

call() { # method path [json]
  local out
  out=$(curl -sf -X "$1" "$BASE$2" -H "$J" ${3:+-d "$3"})
  [ "$(echo "$out" | jq -r .code)" = "0" ] || { echo "FAIL $1 $2 -> $out"; exit 1; }
  echo "$out" | jq -c .data
}

echo "== 1. create ASN"
ASN=$(call POST /inbound/asn '{"warehouseCode":"WH01","ownerCode":"OWN01","supplierCode":"SUP01","type":"PURCHASE",
  "lines":[{"itemCode":"SKU001","expectedQty":100},{"itemCode":"SKU003","expectedQty":50}]}')
ASN_ID=$(echo "$ASN" | jq .id); echo "asn=$(echo "$ASN" | jq -r .code) status=$(echo "$ASN" | jq -r .status)"

echo "== 2. receive"
L1=$(echo "$ASN" | jq '.lines[0].id'); L2=$(echo "$ASN" | jq '.lines[1].id')
R=$(call POST "/inbound/asn/$ASN_ID/receive" "[{\"lineId\":$L1,\"qty\":100,\"lotNo\":\"LOT2409A\",\"expiryDate\":\"2027-12-31\"},{\"lineId\":$L2,\"qty\":50,\"lotNo\":\"LOT2409B\"}]")
echo "status=$(echo "$R" | jq -r .status) receivedQty=$(echo "$R" | jq -r .receivedQty)"

echo "== 3. putaway tasks"
TASKS=$(call GET "/inbound/asn/$ASN_ID/tasks")
echo "$TASKS" | jq -c '.[] | {code,itemCode,qty,suggestLocation}'
for T in $(echo "$TASKS" | jq -c '.[]'); do
  TID=$(echo "$T" | jq .id); LOC=$(echo "$T" | jq -r '.suggestLocation // "A-02-01-02"')
  call POST "/inbound/putaway/$TID/confirm" "{\"toLocation\":\"$LOC\"}" | jq -c '{code,status,toLocation}'
done
echo "asn status=$(call GET "/inbound/asn/$ASN_ID" | jq -r .status)"

echo "== 4. inventory"
call GET "/inventory/page?size=50" | jq -c '.records[] | {locationCode,itemCode,lotNo,qty,allocatedQty,status}'

echo "== 5. create ship order + allocate"
SO=$(call POST /outbound/order '{"warehouseCode":"WH01","ownerCode":"OWN01","customerCode":"CUS01","type":"SALES","priority":5,
  "lines":[{"itemCode":"SKU001","orderQty":30},{"itemCode":"SKU003","orderQty":60}]}')
SO_ID=$(echo "$SO" | jq .id); echo "so=$(echo "$SO" | jq -r .code)"
A=$(call POST "/outbound/order/$SO_ID/allocate"); echo "status=$(echo "$A" | jq -r .status) allocated=$(echo "$A" | jq -r .allocatedQty) (expect PART_ALLOCATED, 80)"

echo "== 6. pick"
for T in $(call GET "/outbound/order/$SO_ID/tasks" | jq -c '.[]'); do
  TID=$(echo "$T" | jq .id); Q=$(echo "$T" | jq .qty)
  call POST "/outbound/pick/$TID/confirm" "{\"qty\":$Q}" | jq -c '{code,fromLocation,toLocation,pickedQty,status}'
done
echo "status=$(call GET "/outbound/order/$SO_ID" | jq -r .status) (expect PICKED)"

echo "== 7. ship"
S=$(call POST "/outbound/order/$SO_ID/ship"); echo "status=$(echo "$S" | jq -r .status) shipped=$(echo "$S" | jq -r .shippedQty)"

echo "== 8. move / freeze / adjust"
INV=$(call GET "/inventory/page?itemCode=SKU001" | jq '.records[0]'); IID=$(echo "$INV" | jq .id)
call POST /inventory/move "{\"inventoryId\":$IID,\"qty\":10,\"toLocation\":\"P-01-01\"}" | jq -c '{locationCode,qty}'
call POST /inventory/freeze "{\"inventoryId\":$IID,\"frozen\":true,\"reason\":\"QC hold\"}" | jq -c '{locationCode,status}'
call POST /inventory/freeze "{\"inventoryId\":$IID,\"frozen\":false,\"reason\":\"QC pass\"}" | jq -c '{locationCode,status}'
call POST /inventory/adjust "{\"inventoryId\":$IID,\"newQty\":58,\"reason\":\"damaged\"}" | jq -c '{locationCode,qty}'

echo "== 9. count"
C=$(call POST /inventory/count '{"warehouseCode":"WH01","zoneCode":"STA","remark":"cycle count"}')
CID=$(echo "$C" | jq .id); echo "count=$(echo "$C" | jq -r .code) lines=$(echo "$C" | jq -r .lineCount)"
LINES=$(call GET "/inventory/count/$CID/lines")
COUNTS=$(echo "$LINES" | jq -c 'map({(.id|tostring): (.systemQty - 1)}) | add')
echo "submit: $(call POST "/inventory/count/$CID/submit" "$COUNTS" | jq -c '{status,diffCount}')"
echo "post:   $(call POST "/inventory/count/$CID/post" | jq -c '{status}')"

echo "== 10. summary / txns / dashboard"
call GET /inventory/summary | jq -c '.[]'
call GET "/inventory/txn/page?size=100" | jq -r '.records | group_by(.txnType) | map("\(.[0].txnType)=\(length)") | join(" ")'
call GET /dashboard | jq -c '{inventoryQty,locationUsed,asnOpen,orderOpen}'
echo "SMOKE OK"
