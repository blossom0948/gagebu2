# 모아씀 AI Worker

모아씀 앱의 AI 요청을 Gemini로 전달하는 Cloudflare Worker입니다.

- `POST /v1/parse-transaction`: 사용자가 직접 입력한 자연어 문장을 검토용 거래 후보로 변환합니다.
- `POST /v1/analyze-spending`: 월 합계, 목표 예산, 이전 달 합계, 카테고리별 합계만 받아 분석 문장·관찰·제안을 반환합니다. 가맹점, 메모, 거래별 데이터는 요청에 포함하지 않습니다.
- `POST /v1/classify-notification`: 사용자가 앱 설정에서 AI 알림 분류를 켠 경우에만 알림 제목·내용을 받아, 완료된 지출/수입인지 판별합니다. 광고·쿠폰·잔액·예정·취소 등은 `OTHER`로 거절합니다. 알림에는 개인정보가 있을 수 있고 Gemini 사용량이 발생하므로 이 설정은 기본 꺼짐입니다.
- `GET /health`: 실행 상태를 확인합니다.

영수증 OCR은 Android의 번들된 한국어 ML Kit 모델로 기기 안에서 처리하며 이 Worker에 보내지 않습니다.

## 로컬 실행

```bash
npm install
npm run typecheck
npm run dev
```

## 배포

```bash
npx wrangler login
npx wrangler secret put GEMINI_API_KEY
npx wrangler secret put SUPABASE_PUBLISHABLE_KEY
npm run deploy
```

현재 저장소의 기본값은 개인 테스트를 위해 `REQUIRE_AUTH=false`이며, IP별 간단한 호출 제한과 입력 제한을 적용합니다. 여러 사용자에게 공개하기 전에는 `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`를 설정하고 `REQUIRE_AUTH=true`로 바꾼 뒤 앱의 로그인 토큰 연동을 완료해야 합니다. 인증 없는 Worker는 공개 Gemini 프록시가 될 수 있으므로 운영 배포에 그대로 사용하지 마세요.

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
