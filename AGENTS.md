# AGENTS.md

## 1) Project Overview

이 프로젝트는 보험 관련 외부 Open API를 운영자 관점에서 통합 조회/관리하는 백엔드 프로토타입이다.
목표는 "보험 업무 전체 구현"이 아니라 "인터페이스 운영 관리"다.

핵심 목표:
- 외부 보험 API 3종 통합 조회
- API 호출 이력 저장/검색
- 성공/실패 상태 관리
- 실패 건 재처리
- 운영자용 대시보드 제공

비목표(Non-goals):
- 실제 보험 가입/청약/지급 처리
- 실시간 금융거래 엔진
- 복잡한 본인인증/전자서명 체계
- 대규모 분산 인프라(예: 멀티리전, 이벤트 버스 중심 아키텍처)

---

## 2) Architecture Principles

기본 아키텍처는 Layered Architecture를 따른다.

의존 방향:
- Controller -> Service -> Repository
- Service -> External API Client
- Repository -> Database

계층 책임:
- Controller
  - Request 검증, Response 변환/반환
  - 비즈니스 로직 금지
- Service
  - 비즈니스 규칙, 상태 전이, 호출 흐름 제어
  - 트랜잭션 경계 관리
- Repository
  - 조회/저장 전담
  - 정책/상태 판단 로직 금지
- External API Client
  - 외부 API 통신 전담
  - 외부 스펙 차이 흡수

권장 패키지(현재 구조 포함):
- `domain/*/controller`
- `domain/*/service`
- `domain/*/repository`
- `domain/*/client`
- `domain/*/dto`
- `common/config`, `common/exception`

---

## 3) Tech Stack Guardrails

현재 기본 스택(변경 금지 unless 명시 요청):
- Java 17
- Spring Boot
- Spring Security
- Session 기반 인증
- Spring Data JPA
- MySQL
- springdoc-openapi (Swagger UI)
- Docker / docker-compose
- Spring Scheduler

금지:
- 인증 방식을 임의로 JWT로 전환
- 아키텍처를 DDD/Clean으로 전면 교체
- 요청 없는 신규 프레임워크/라이브러리 대량 도입

---

## 4) Domain Modeling Guidance

핵심 관리 대상은 "외부 인터페이스 호출과 운영 이력"이다.

주요 개념:
- 인터페이스 정의 정보
- 인터페이스 호출 이력
- 호출 상태
- 재처리 이력
- 대시보드 집계

상태 예시:
- `SUCCESS`, `FAIL`, `PENDING`, `RETRY`

상태 처리 규칙:
- 문자열 상수 남용 대신 enum 우선
- 상태 전이는 Service에서만 수행
- Repository는 상태 판단하지 않음

---

## 5) Coding Rules

공통:
- 변경은 최소 범위로 수행
- 기존 네이밍/구조 우선 존중
- 요청 없는 리팩터링 금지
- 하드코딩 금지, 민감정보 커밋 금지
- Entity를 API 응답으로 직접 노출 금지

Controller:
- DTO 기반 입출력 사용 (`Request DTO`, `Response DTO`)
- 파라미터 검증 + 응답 매핑에 집중

Service:
- 외부 API 호출 -> 상태 결정 -> 이력 저장 흐름을 명시적으로 유지
- 예외 정책을 서비스 레벨에서 일관 처리

Repository:
- CRUD/쿼리 전담
- 외부 연동/정책/상태 전이 금지

DTO/Entity:
- DTO와 Entity 분리
- 외부 API DTO와 내부 DTO 필요시 분리
- 외부 응답을 곧바로 Entity에 강결합하지 않기

예외/로그:
- 사용자 메시지와 운영 로그 메시지 분리
- 개인정보/토큰/API Key 로그 출력 금지
- 장애 추적에 필요한 최소 메타데이터는 유지

---

## 6) External API Integration Rules

핵심 원칙:
- 외부 호출은 `client` 계층에만 위치
- Controller에서 외부 API 직접 호출 금지
- API별 스펙 차이는 client/mapper에서 흡수
- Service는 "호출 순서와 업무 흐름"만 통제

실패 처리:
- 타임아웃, HTTP 오류, 파싱 실패를 구분 처리
- 실패 시 호출 이력 + 상태값 반드시 기록
- 재처리는 기존 요청 조건(또는 재처리 정책) 기준으로 동작

확장 시:
- 신규 API 추가가 기존 구조를 깨지 않게 설계
- 공통 흐름과 API별 구현 분리
- 필요한 범위에서만 전략/인터페이스 패턴 사용

---

## 7) Database Rules

운영성 우선 원칙:
- 조회 성능보다 "추적 가능성"을 우선 확보
- 요청 시각, 인터페이스 종류, 상태, 재처리 여부 저장
- `createdAt`, `updatedAt` 정책 일관성 유지
- 상태 저장 방식(enum/code) 프로젝트 전체 통일

조회/성능:
- 대시보드 쿼리는 유지보수 가능한 수준으로 설계
- N+1 가능성 점검
- 인덱스 후보를 명시적으로 검토
  - interface type
  - status
  - called at
  - retry flag

스키마 변경:
- 스키마 변경 시 연관 코드 + 문서/주석 동시 반영

---

## 8) Scheduler Rules

스케줄러는 배치/상태점검 용도로만 사용:
- 실패 건 자동 재시도
- 일 단위 집계
- 상태 점검

규칙:
- 스케줄러에는 오케스트레이션만 배치
- 핵심 로직은 Service로 분리
- 수동 재처리 API와 충돌하지 않게 설계

---

## 9) Security Rules

현재 인증 정책은 Session 기반이다.

규칙:
- JWT로 임의 전환 금지
- 운영자 기능은 인증/인가 명시 적용
- 보안 설정은 `common/config`에서 일관 관리
- 개발 편의를 위한 보안 예외는 범위/환경을 제한

---

## 10) API Documentation Rules

- 주요 엔드포인트는 Swagger 문서화 필수
- 요청/응답/에러 예시를 구현과 함께 유지
- API 변경 시 문서 동시 수정

---

## 11) Testing Expectations

변경 유형별 최소 테스트 기준:
- Service 변경 -> Service 테스트
- Controller 스펙 변경 -> Controller 테스트
- Repository 쿼리 변경 -> Repository/통합 테스트
- External API 연동 -> Mock 기반 테스트 우선

테스트 내용:
- 성공 케이스 + 실패/예외/재처리 케이스
- 상태 전이 검증
- 회귀 위험이 큰 흐름 우선 보호

원칙:
- 큰 구조 변경을 테스트 없이 종료하지 않는다.

---

## 12) Run & Verification (Current Repo)

작업 경로:
- 애플리케이션 모듈: `insurance-interface`

기본 명령(모듈 디렉터리에서 실행):
- 빌드: `./gradlew clean build`
- 테스트: `./gradlew test`
- 컴파일 확인: `./gradlew compileJava`
- 실행: `./gradlew bootRun`

운영 확인 체크리스트:
- 빌드 성공
- 테스트 통과
- 변경 API가 기존 레이어 규칙과 충돌 없는지 확인
- 예외 처리/로그 민감정보 마스킹 확인
- Swagger 반영 확인

---

## 13) Decision Priority

충돌 시 우선순위:
1. 요구사항 범위 유지
2. Layered Architecture 준수
3. 운영자용 인터페이스 관리 목적 유지
4. 단순하고 유지보수 가능한 구조
5. 과도한 추상화보다 명확한 흐름

핵심 기준:
- 이 프로젝트의 성공은 "이론적으로 화려한 구조"보다
  "외부 인터페이스를 안정적으로 조회/추적/재처리"하는 데 있다.

---

## 14) Do Not

다음 작업은 요청 없이 수행하지 않는다:
- 세션 인증 -> JWT 전환
- Layered -> DDD/Clean 전면 개편
- 불필요한 스택/인프라 추가
- Entity 대량 직접 노출 API 작성
- 비밀키/설정 하드코딩
- 테스트 실패 상태로 종료
- 문서와 구현 불일치 방치

---

## 15) Definition of Done

완료 기준:
- 요구사항 범위 내 구현 완료
- 레이어 책임 분리 준수
- 상태/이력/재처리 흐름 일관성 확보
- 예외/로그 최소 품질 충족
- 가능한 범위 테스트 반영
- Swagger/문서 동기화

