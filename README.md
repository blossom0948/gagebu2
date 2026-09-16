# 모아씀

내 소비를 가볍게 모으는 로컬 우선 가계부 앱입니다.

## 열기

이 폴더를 Android Studio에서 열고 `app` 실행 구성을 선택하면 됩니다.

## 빌드

macOS/Linux:

```bash
./gradlew :app:assembleDebug
```

생성 파일:

```text
app/build/outputs/apk/debug/app-debug.apk
```

현재 개인 가계부는 서버 연결 없이 기기에서 동작합니다. 거래 수정·삭제 취소, 월 목표 지출과 카테고리별 예산, 반복 거래, 기본·사용자 카테고리 관리, 결제수단·카드·결제일 관리, CSV/JSON 백업, 기기 데이터 삭제를 지원합니다. AI 문장 입력은 거래 후보를 검토한 뒤 저장하며, AI 분석과 소비 Q&A는 사용자가 눌렀을 때 월 합계·예산·카테고리 합계만 전송합니다. 음성은 Android 음성 인식 앱을 호출하고, 한국어 영수증 OCR은 사진을 기기 안에서 처리합니다.

알림 감지는 Android의 알림 접근과 모아씀의 알림 표시 권한이 필요합니다. 기본은 기기 내 엄격한 거래 문구 검사이며, `관리`에서 AI 오탐 줄이기를 켠 경우에만 금액/금융 단서가 있는 알림의 제목·내용을 Cloudflare Worker를 통해 Gemini에 보내 완료된 지출/수입인지 분류합니다. 이 선택 기능은 알림에 담긴 개인정보를 외부 AI 서버로 전송하고 Gemini 사용량을 쓸 수 있습니다. 인식 결과는 자동 저장하지 않고 앱에서 확인을 받습니다.

함께 쓰기/계정·기기 간 동기화는 아직 연결되지 않았습니다. 공동 장부를 실제로 활성화하려면 Supabase 프로젝트, 인증, RLS 정책과 동기화 검증이 필요합니다. 알림 감지는 Android의 알림 접근 설정에서 사용자가 허용해야 하며, 앱이 대신 켤 수 없습니다.

### 개발 확인

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
cd cloudflare/ai-worker
npm run typecheck
```

앱 APK 경로는 `app/build/outputs/apk/debug/app-debug.apk`입니다. AI Worker 코드나 경로를 바꾸면 `cloudflare/ai-worker`에서 `npm run deploy`도 실행해야 합니다. AI 키는 Worker Secret으로만 관리하고 APK에 넣지 않습니다.

## 무료 개인 배포와 앱 내 업데이트

Play Console 없이 본인 Galaxy에서 사용할 때는 GitHub Releases에 서명된 APK를 올리는 방식으로 배포합니다. 앱의 `관리 → 앱 업데이트 → 업데이트 확인`을 누르면 새 APK를 앱 안에서 직접 다운로드하고, 크기·해시·앱 ID·버전·서명을 확인한 뒤 Android 설치 화면을 엽니다. GitHub 다운로드 페이지나 브라우저로 나갈 필요는 없습니다.

처음 한 번은 Android가 모아씀에 `이 출처의 앱 설치 허용`을 요청할 수 있고, 설치 화면에서 업데이트를 사용자가 승인해야 합니다. Android 보안상 이 설정과 최종 설치 확인은 앱이 대신 누를 수 없습니다. 기존 앱 데이터는 업데이트해도 Room 저장소에 그대로 남습니다.

Google Play Protect가 개인 APK를 추가로 차단하면 시스템 화면의 안내에서 출처를 확인한 뒤 설치를 선택해야 합니다. 앱이나 APK가 Play Protect 검사를 우회하도록 만들 수는 없습니다.

서명 키는 로컬과 GitHub Actions secret으로만 관리하고 저장소에는 올리지 않습니다. 릴리스마다 `versionCode`를 올려야 앱이 새 버전으로 인식합니다.

## GitHub 배포

`docs/android-apk.yml`에 GitHub Actions 설정을 준비해 두었습니다. GitHub 계정에 Actions workflow 등록 권한이 있으면 이 파일을 `.github/workflows/android-apk.yml`로 옮긴 뒤 `main`에 push할 때 테스트·lint·Debug APK 빌드가 실행됩니다. 성공한 실행의 Artifacts에서 `moasseum-debug-apk`를 내려받아 기기에 설치하면 됩니다. Debug APK는 테스트용이며 Play Store 배포용 서명 APK/AAB를 대신하지 않습니다.

현재는 Android 앱만 배포합니다. AI Worker나 Supabase를 추가하는 작업부터는 Android APK 배포와 별도로 Cloudflare Worker 배포 및 Supabase RLS 검증이 필요합니다. Gemini 키와 Supabase secret은 APK·GitHub 소스에 넣지 않고 서버 secret으로만 등록합니다. 상세 절차는 [BUDGET_APP_DEPLOYMENT_GUIDE.md](BUDGET_APP_DEPLOYMENT_GUIDE.md)를 따릅니다.

서명 릴리스 자동화 템플릿은 `docs/android-release.yml`에 있습니다. GitHub Actions workflow 권한을 받은 뒤 `.github/workflows/android-release.yml`로 옮기고, 아래 저장소 secrets를 등록하면 `main` push마다 테스트·서명 APK/AAB 빌드와 GitHub Release 생성이 실행됩니다.

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

GitHub Release는 무료 개인 배포용이며 앱이 APK를 직접 내려받습니다. Android 앱을 Play Store처럼 무음으로 교체하지는 않으며, 다운로드 뒤 Android 설치 확인이 필요합니다.

AI Worker 코드는 `cloudflare/ai-worker`에 있으며 현재 `https://moasseum-ai-worker.blossom0948.workers.dev`에 배포되어 있습니다. 기본 Debug 빌드에는 이 주소가 들어가고, 다른 환경을 사용할 때만 아래처럼 주소를 덮어씌웁니다.

```bash
./gradlew :app:assembleDebug -PAI_API_BASE_URL=https://<worker-domain>
```

앱에서 AI 후보를 바로 원장에 저장하지 않고 금액·날짜·카테고리·가맹점 확인 화면을 거칩니다. 알림 접근을 허용하면 금융 알림 후보를 만들고, 앱이 백그라운드일 때는 모아씀 알림으로 `인식되었습니다. 추가할까요?`를 표시합니다. 후보 알림에서 열거나 앱이 앞에 있을 때 확인한 다음에만 거래로 저장됩니다. `관리`에서 AI 오탐 줄이기를 켜면 금액/금융 단서가 있는 알림의 제목과 내용이 Gemini 분류에 전송됩니다.

현재 Worker는 로그인 기능이 아직 앱에 연결되지 않아 개인 테스트용으로 `REQUIRE_AUTH=false`로 실행 중이며, 요청 길이·간단한 호출 제한을 적용합니다. `/v1/parse-transaction`은 사용자가 입력한 문장을 거래 후보로 만들고, `/v1/analyze-spending`은 집계 수치로 짧은 소비 분석을 반환하며, `/v1/ask-spending`은 같은 집계만으로 Q&A를 처리합니다. 공개 배포 전에는 Supabase 인증을 연결하고 `REQUIRE_AUTH=true`로 전환해야 합니다.

첫 실행에는 결제 알림 감지 안내 팝업이 표시되고, `알림 읽기 설정 열기`와 `알림 표시 설정 열기`를 단계별로 안내합니다. Galaxy의 알림 접근은 일반 런타임 권한이 아니어서 앱이 시스템 토글을 대신 켤 수는 없으며, 사용자가 한 번 허용해야 합니다. 이후에는 관리 화면에서 서비스 연결·마지막 수신·후보 생성 상태를 확인하거나 다시 연결할 수 있습니다.

앞으로 기능을 추가할 때마다 변경 보고에 다음을 함께 적습니다.

- 앱 코드만 바뀌는지, 서버 배포가 필요한지
- 실행할 Gradle/Cloudflare/Supabase 명령
- 생성되는 APK 또는 Actions artifact 위치
- 실제 기기 설치·실행 여부와 미실행 사유
