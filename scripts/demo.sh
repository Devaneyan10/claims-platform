#!/usr/bin/env bash
# Walks one claim through the whole lifecycle. Requires curl and jq; both services running.
set -euo pipefail
API=http://localhost:8080/api
EXP=http://localhost:8081/api
h() { echo "-H" "X-User-Id: $1" "-H" "X-User-Role: $2" "-H" "Content-Type: application/json"; }
call() { local u=$1 r=$2 m=$3 p=$4 b=${5:-}; curl -sS -X "$m" "$API$p" $(h "$u" "$r") ${b:+-d "$b"}; }

echo "1. Alice reports a claim"
ID=$(call alice CLAIMANT POST /claims '{"claimantName":"Alice Tan","market":"SG","type":"MOTOR","description":"Rear-ended at a junction","incidentDate":"2026-11-01","estimatedAmount":4000}' | tee /dev/stderr | jq -r .id)
echo; echo "2. Officer bob sees it in the queue and picks it up"
call bob OFFICER GET /claims/queue | jq '.[].claimNumber'
call bob OFFICER POST "/claims/$ID/assign" | jq .status
call bob OFFICER POST "/claims/$ID/start-review" | jq .status
echo "3. Bob asks for more information"
RID=$(call bob OFFICER POST "/claims/$ID/info-requests" '{"question":"Please upload the police report number"}' | jq -r .id)
echo "4. Alice answers"
call alice CLAIMANT POST "/claims/$ID/info-requests/$RID/answer" '{"answer":"Police report SG-12345"}' | jq .answer
echo "5. Bob approves and settles"
call bob OFFICER POST "/claims/$ID/decision" '{"approved":true,"approvedAmount":3500,"reason":"Covered under policy"}' | jq .status
sleep 2
echo "Exposure while approved (before settlement):"
curl -sS "$EXP/exposure/summary" -H "X-User-Role: MANAGER" | jq .
call bob OFFICER POST "/claims/$ID/settle" | jq .status
echo "6. Manager views workload"
call mgr MANAGER GET /staff/workload | jq .
echo "7. Audit trail"
call alice CLAIMANT GET "/claims/$ID/audit" | jq -c '.[] | [.action,.actor]'
