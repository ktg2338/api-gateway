#!/bin/bash

echo "======================================"
echo "Circuit Breaker 테스트 스크립트"
echo "======================================"
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "1️⃣  GET 요청 - Timeout 테스트 (/slow)"
echo "--------------------------------------"
echo "설명: 5초 지연 엔드포인트 호출 → 3초 타임아웃 발생 → Circuit Breaker OPEN"
echo ""
for i in 1 2 3 4 5 6; do
    echo -n "Request $i: "
    RESPONSE=$(curl -s -w "\n%{http_code}\n%{time_total}" http://localhost:8080/competition/test/slow 2>&1)
    HTTP_CODE=$(echo "$RESPONSE" | tail -2 | head -1)
    TIME=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$d' | sed '$d')

    if echo "$BODY" | grep -q "Circuit Breaker"; then
        echo -e "${RED}HTTP $HTTP_CODE${NC} - ${TIME}s - ${YELLOW}Circuit Breaker 발동!${NC}"
    else
        echo -e "${GREEN}HTTP $HTTP_CODE${NC} - ${TIME}s"
    fi
    sleep 0.5
done

echo ""
echo "2️⃣  POST 요청 - 모드 변경 테스트"
echo "--------------------------------------"
echo "설명: control 엔드포인트의 모드를 변경하여 다양한 시나리오 테스트"
echo ""

# fail 모드로 변경
echo -n "POST /control (모드 변경 -> fail): "
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/competition/test/control -H "Content-Type: application/json" -d '{"mode":"fail"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
echo -e "${GREEN}HTTP $HTTP_CODE${NC}"

# fail 모드 테스트
echo ""
echo "GET /control (fail 모드 확인):"
for i in 1 2 3; do
    echo -n "  Request $i: "
    RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:8080/competition/test/control)
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" == "500" ]; then
        echo -e "${RED}HTTP $HTTP_CODE${NC} - Fail 모드 정상 동작"
    else
        echo -e "${GREEN}HTTP $HTTP_CODE${NC}"
    fi
    sleep 0.3
done

echo ""
echo "3️⃣  Circuit Breaker 복구 테스트"
echo "--------------------------------------"
echo "10초 대기 중... (OPEN → HALF_OPEN)"
sleep 10

# success 모드로 변경
echo -n "POST /control (모드 변경 -> success): "
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/competition/test/control -H "Content-Type: application/json" -d '{"mode":"success"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
echo -e "${GREEN}HTTP $HTTP_CODE${NC}"

echo ""
echo "GET /control (success 모드 - Circuit 복구 확인):"
for i in 1 2 3 4 5; do
    echo -n "  Request $i: "
    RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:8080/competition/test/control)
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)

    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}HTTP $HTTP_CODE${NC} - 정상"
    else
        echo -e "${RED}HTTP $HTTP_CODE${NC}"
    fi
    sleep 0.5
done

echo ""
echo "4️⃣  Unstable 엔드포인트 테스트 (50% 실패)"
echo "--------------------------------------"
for i in 1 2 3 4 5 6 7 8; do
    echo -n "Request $i: "
    RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:8080/competition/test/unstable)
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" == "500" ]; then
        echo -e "${RED}HTTP $HTTP_CODE${NC} - 실패"
    elif echo "$BODY" | grep -q "Circuit Breaker"; then
        echo -e "${YELLOW}HTTP $HTTP_CODE - Circuit Breaker 발동!${NC}"
    else
        echo -e "${GREEN}HTTP $HTTP_CODE${NC} - 성공"
    fi
    sleep 0.3
done

echo ""
echo "======================================"
echo "테스트 완료!"
echo "======================================"

#rate limit test
#hey -n 150 -m POST -H "accept: */*" http://localhost:8080/competition/test/profile
