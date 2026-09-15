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

현재 기본 기능은 네트워크 없이 동작하며 거래와 월 예산은 Room 데이터베이스에 저장됩니다. AI, OCR, 공동 가계부, 서버 동기화는 서버 설정 전까지 비활성 안내 화면으로 표시됩니다.

## GitHub 배포

`docs/android-apk.yml`에 GitHub Actions 설정을 준비해 두었습니다. GitHub 계정에 Actions workflow 등록 권한이 있으면 이 파일을 `.github/workflows/android-apk.yml`로 옮긴 뒤 `main`에 push할 때 테스트·lint·Debug APK 빌드가 실행됩니다. 성공한 실행의 Artifacts에서 `moasseum-debug-apk`를 내려받아 기기에 설치하면 됩니다. Debug APK는 테스트용이며 Play Store 배포용 서명 APK/AAB를 대신하지 않습니다.

현재는 Android 앱만 배포합니다. AI Worker나 Supabase를 추가하는 작업부터는 Android APK 배포와 별도로 Cloudflare Worker 배포 및 Supabase RLS 검증이 필요합니다. Gemini 키와 Supabase secret은 APK·GitHub 소스에 넣지 않고 서버 secret으로만 등록합니다. 상세 절차는 [BUDGET_APP_DEPLOYMENT_GUIDE.md](BUDGET_APP_DEPLOYMENT_GUIDE.md)를 따릅니다.

앞으로 기능을 추가할 때마다 변경 보고에 다음을 함께 적습니다.

- 앱 코드만 바뀌는지, 서버 배포가 필요한지
- 실행할 Gradle/Cloudflare/Supabase 명령
- 생성되는 APK 또는 Actions artifact 위치
- 실제 기기 설치·실행 여부와 미실행 사유
