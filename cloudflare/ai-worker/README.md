# 모아씀 AI Worker

모아씀 앱의 AI 요청을 Gemini로 전달하는 Cloudflare Worker입니다.

- `POST /v1/parse-transaction`: 사용자가 직접 입력한 자연어 문장을 검토용 거래 후보로 변환합니다.
- `POST /v1/parse-batch-command`: 문장 일괄 처리 명령을 추가 후보 또는 좁은 검색/수정 계획으로 변환합니다. Worker에는 사용자 입력 문장만 전송하고 거래 행은 전송하지 않습니다. 수정·삭제 대상은 앱이 기기 안의 개인 거래와 대조해 보여 주며, 사용자가 행을 선택하고 확인하기 전에는 저장하지 않습니다. 이체·공유·할부 거래는 앱에서 제외합니다.
- `POST /v1/analyze-spending`: 월 합계, 목표 예산, 이전 달 합계, 카테고리별 합계만 받아 분석 문장·관찰·제안을 반환합니다. 가맹점, 메모, 거래별 데이터는 요청에 포함하지 않습니다.
- `POST /v1/ask-spending`: 월간 집계만 근거로 사용자의 소비 질문에 짧게 답합니다. 질문과 집계 외의 거래 원문은 보내지 않습니다.
- `POST /v1/classify-notification`: 사용자가 앱 설정에서 AI 알림 분류를 켠 경우에만 알림 제목·내용을 받아, 완료된 지출/수입인지 판별합니다. 광고·쿠폰·잔액·예정·취소 등은 `OTHER`로 거절합니다. 알림에는 개인정보가 있을 수 있고 Gemini 사용량이 발생하므로 이 설정은 기본 꺼짐입니다.
- `GET /health`: 실행 상태를 확인합니다.

영수증 OCR은 Android의 번들된 한국어 ML Kit 모델로 기기 안에서 처리하며 이 Worker에 보내지 않습니다.

## 로컬 실행

```bash
npm install
npm run typecheck
npm test
npm run dev
```

## 배포

```bash
npx wrangler login
npx wrangler secret put GEMINI_API_KEY
npm run deploy
```

현재 `REQUIRE_AUTH=true`이며 모아씀 전용 Free Supabase의 `/auth/v1/user`로 토큰을 확인합니다. 무인증/위조 토큰은 401, 인증 서버 장애는 503입니다. 사용자별 분당 30회 제한은 Worker 인스턴스 메모리 기반이며 전 세계 합산의 절대 한도를 보장하지 않습니다. `SUPABASE_URL`과 publishable key는 클라이언트 공개 설정으로 `wrangler.jsonc`에 있고, Gemini 키만 runtime secret입니다. Secret/service_role key는 필요하지 않습니다.

`npm test`는 Node의 타입 제거 기능과 Miniflare/Workerd를 사용합니다(이 작업은 Node 26에서 검증). Cloudflare 실행 환경은 `fetch`의 `redirect:"error"`를 거부하므로 `manual`을 사용하고 3xx를 거절합니다. Node mock 테스트만으로 통과시키지 않고 실제 Workerd와 배포 서버의 401/200, Galaxy의 인증된 AI 응답을 함께 검사합니다. 토큰·요청 헤더·본문을 로그에 출력하지 않습니다.

배포 후 `/health`가 응답하는지 확인하고, Android 앱을 다음처럼 빌드할 때 Worker 주소를 주입합니다.

```bash
./gradlew :app:assembleDebug -PAI_API_BASE_URL=https://<worker-domain>
```

Gemini 키는 Worker secret으로만 관리하며 Android 소스, Gradle 파일, APK에 넣지 않습니다.

앱 기능별 배포 범위:

- Android 화면·로컬 기능 변경: Gradle 테스트와 APK 릴리스가 필요합니다.
- Worker 라우트·프롬프트 변경: `npm run typecheck` 후 `npm run deploy`가 필요합니다.
- 양쪽 계약을 함께 바꾸면 Worker를 먼저 배포하고 앱 APK를 새로 빌드합니다.
- Worker 호출은 Gemini 계정의 현재 한도/요금제에 따릅니다. 앱의 AI 분석은 사용자가 명시적으로 실행해야 전송되며, 알림 내용은 AI 알림 분류 설정에 동의한 경우에만 전송됩니다.
