# Insurance Interface (Backend Prototype)

보험 관련 외부 Open API를 운영자 관점에서 통합 조회/관리하는 백엔드 프로토타입입니다.

- 외부 보험 API 3종 통합 조회
- API 호출 이력 저장/검색
- 성공/실패 상태 관리
- 실패 건 재처리 이력 관리
- 운영자용 대시보드/조회 화면 제공

## 문서

- 프로젝트 기획안: [docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md)
- 아키텍처 설계도: [docs/ARCHITECTURE_DESIGN.md](docs/ARCHITECTURE_DESIGN.md)

## 1. 실행 환경

### 필수 설치

- Git
- Docker Desktop + Docker Compose (권장 실행 방식)

### 로컬 실행 시 추가 설치

- Java 17
- (선택) MySQL 8.x

## 2. 저장소 클론

```bash
git clone <YOUR_REPOSITORY_URL>
cd PROJECT
```

## 3. 환경 변수 설정

이 프로젝트는 **민감정보를 GitHub에 올리지 않고**, 템플릿 파일(`.env.example`)을 복사해 사용합니다.
**실제 `.env` 값은 저장소에 포함하지 않았으며, 이력서 포트폴리오 제출본에 별도 파일로 첨부했습니다.**

### 3-1. Docker Compose용 (루트)

```bash
cp .env.example .env
```

루트 `.env` 주요 항목:

- `MYSQL_DATABASE`: DB 이름
- `MYSQL_USER`: DB 계정
- `MYSQL_PASSWORD`: DB 계정 비밀번호
- `MYSQL_ROOT_PASSWORD`: MySQL root 비밀번호
- `CAR_INSURANCE_API_SERVICE_KEY`: 자동차보험 API 서비스 키
- `LIFE_INSURANCE_API_SERVICE_KEY`: 생명보험 API 서비스 키 (미입력 시 `CAR_INSURANCE_API_SERVICE_KEY` 사용)
- `INDEMNITY_INSURANCE_API_SERVICE_KEY`: 실손보험 API 서비스 키 (미입력 시 `CAR_INSURANCE_API_SERVICE_KEY` 사용)

### 3-2. 애플리케이션 직접 실행용 (`insurance-interface` 모듈)

```bash
cp insurance-interface/.env.example insurance-interface/.env
```

모듈 `.env` 주요 항목:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `CAR_INSURANCE_API_SERVICE_KEY`
- `LIFE_INSURANCE_API_SERVICE_KEY`
- `INDEMNITY_INSURANCE_API_SERVICE_KEY`
- 각 API Base URL/Timeout/조회 건수(`*_BASE_URL`, `*_TIMEOUT_MS`, `*_NUM_OF_ROWS`)

## 4. 빠른 실행 (권장: Docker Compose)

루트 경로에서 실행:

```bash
docker compose up -d --build
```

중지:

```bash
docker compose down
```

데이터 볼륨까지 삭제:

```bash
docker compose down -v
```

## 5. 로컬 실행 (DB 분리)

1) MySQL만 컨테이너로 실행

```bash
docker compose up -d mysql
```

2) 애플리케이션 실행

```bash
cd insurance-interface
./gradlew bootRun
```

## 6. 초기 운영자 계정 생성 (필수)

현재 구현에는 회원가입 API가 없으므로, 최초 1회 운영자 계정을 DB에 직접 생성해야 합니다.

1) BCrypt 해시 생성 예시 (`testpw` 비밀번호 기준)

```bash
htpasswd -bnBC 10 "" "testpw" | tr -d ':\n'
```

2) MySQL 접속 후 계정 INSERT

```sql
INSERT INTO app_user (
  login_id,
  password_hash,
  user_name,
  role,
  use_yn,
  created_at,
  updated_at
) VALUES (
  'test',
  '$2y$10$rLgnJGmO6iXl0yUEkASnIehD42EW2o0VrQX54pEN265jHpUqPIFG6',
  '테스트운영자',
  'ROLE_OPERATOR',
  true,
  NOW(),
  NOW()
);
```

MySQL 접속 예시 (`MYSQL_USER`, `MYSQL_DATABASE`는 본인 `.env` 값으로 치환):

```bash
docker exec -it my-mysql mysql -u<MYSQL_USER> -p <MYSQL_DATABASE>
```

## 7. 접속 주소

- 로그인 페이지: <http://localhost:8080/login>
- 대시보드: <http://localhost:8080/dashboard>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- 초기 로그인 계정: `test` / `testpw`

## 8. 빌드/테스트/컴파일

모듈 디렉터리(`insurance-interface`)에서 실행:

```bash
./gradlew clean build
./gradlew test
./gradlew compileJava
```

## 9. 제출 시 체크리스트

- `.env` 파일 커밋 금지 (`.env.example`만 커밋)
- API 키/DB 비밀번호 문서 내 평문 노출 금지
- README 기준으로 제3자 로컬 재현 가능 여부 확인
- Swagger UI 접근 및 주요 API 호출 확인
