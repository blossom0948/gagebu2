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

현재 기본 기능은 네트워크 없이 동작하며 거래와 한 달 목표 지출은 Room 데이터베이스에 저장됩니다. AI 문장 입력은 API 주소가 없을 때 기기 안의 로컬 파서로도 동작하고, 서버 주소를 넣으면 확인 가능한 Gemini 거래 후보로 전환됩니다. OCR과 공동 가계부·서버 동기화는 별도 설정 전까지 안내 상태로 표시됩니다.

## GitHub 배포

`docs/android-apk.yml`에 GitHub Actions 설정을 준비해 두었습니다. GitHub 계정에 Actions workflow 등록 권한이 있으면 이 파일을 `.github/workflows/android-apk.yml`로 옮긴 뒤 `main`에 push할 때 테스트·lint·Debug APK 빌드가 실행됩니다. 성공한 실행의 Artifacts에서 `moasseum-debug-apk`를 내려받아 기기에 설치하면 됩니다. Debug APK는 테스트용이며 Play Store 배포용 서명 APK/AAB를 대신하지 않습니다.

현재는 Android 앱만 배포합니다. AI Worker나 Supabase를 추가하는 작업부터는 Android APK 배포와 별도로 Cloudflare Worker 배포 및 Supabase RLS 검증이 필요합니다. Gemini 키와 Supabase secret은 APK·GitHub 소스에 넣지 않고 서버 secret으로만 등록합니다. 상세 절차는 [BUDGET_APP_DEPLOYMENT_GUIDE.md](BUDGET_APP_DEPLOYMENT_GUIDE.md)를 따릅니다.

AI Worker 코드는 `cloudflare/ai-worker`에 있습니다. Worker를 배포한 뒤 아래처럼 주소를 주입해 앱을 빌드합니다.

```bash
./gradlew :app:assembleDebug -PAI_API_BASE_URL=https://<worker-domain>
```

앱에서 AI 후보를 바로 원장에 저장하지 않고 금액·날짜·카테고리·가맹점 확인 화면을 거칩니다. 결제 알림도 사용자가 Android 설정에서 알림 접근을 허용한 뒤 후보함에서 확인해야 거래로 저장됩니다.

첫 실행에는 결제 알림 감지 안내 팝업이 표시되고, `설정 열기`를 누르면 Galaxy의 알림 접근 설정에서 모아씀 항목으로 바로 이동합니다. Android의 알림 접근은 일반 런타임 권한이 아니어서 앱이 시스템 토글을 대신 켤 수는 없으며, 사용자가 한 번 허용해야 합니다. 이후에는 관리 화면에서 상태를 확인하거나 다시 설정할 수 있습니다.

앞으로 기능을 추가할 때마다 변경 보고에 다음을 함께 적습니다.

- 앱 코드만 바뀌는지, 서버 배포가 필요한지
- 실행할 Gradle/Cloudflare/Supabase 명령
- 생성되는 APK 또는 Actions artifact 위치
- 실제 기기 설치·실행 여부와 미실행 사유
