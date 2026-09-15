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

`SUPABASE_URL`은 Wrangler 변수로 설정하고 `REQUIRE_AUTH=true`를 유지하세요. 그러면 Worker가 앱이 보내는 Supabase Bearer token을 먼저 검증한 뒤 Gemini를 호출합니다. 테스트만 할 때도 `REQUIRE_AUTH=false`로 공개 배포하지 마세요.

배포 후 `/health`가 응답하는지 확인하고, Android 앱을 다음처럼 빌드할 때 Worker 주소를 주입합니다.

```bash
./gradlew :app:assembleDebug -PAI_API_BASE_URL=https://<worker-domain>
```

Gemini 키는 Worker secret으로만 관리하며 Android 소스, Gradle 파일, APK에 넣지 않습니다.
