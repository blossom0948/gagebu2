# 모아씀 AI Worker

앱이 보내는 자연어 거래 문장을 Gemini 구조화 출력으로 변환하는 Cloudflare Worker입니다.

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

현재 저장소의 기본값은 개인 테스트를 위해 `REQUIRE_AUTH=false`이며, IP별 간단한 호출 제한과 입력 길이 제한을 적용합니다. 여러 사용자에게 공개하기 전에는 `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`를 설정하고 `REQUIRE_AUTH=true`로 바꾼 뒤 앱의 로그인 토큰 연동을 완료해야 합니다. 인증 없는 Worker는 공개 Gemini 프록시가 될 수 있으므로 운영 배포에 그대로 사용하지 마세요.

배포 후 `/health`가 응답하는지 확인하고, Android 앱을 다음처럼 빌드할 때 Worker 주소를 주입합니다.

```bash
./gradlew :app:assembleDebug -PAI_API_BASE_URL=https://<worker-domain>
```

Gemini 키는 Worker secret으로만 관리하며 Android 소스, Gradle 파일, APK에 넣지 않습니다.
