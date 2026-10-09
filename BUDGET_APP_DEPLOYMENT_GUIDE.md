# AI 가계부 앱 배포 가이드

> 이 문서는 배포 절차만 다룬다. 앱의 화면·기능 설계는 `WEEPLE_INSPIRED_BUDGET_APP_MASTER_PLAN.md`를 따른다.  
> 목표는 GitHub에 소스를 보관하고, Cloudflare에 AI API를 배포하며, Android 앱은 APK로 실제 기기에 설치하는 것이다.

## 1. 먼저 알아둘 점

이 앱은 Kotlin + Jetpack Compose로 만드는 **Android 네이티브 앱**이다. 따라서 Cloudflare에 올리는 것만으로 휴대폰에 앱이 설치되지는 않는다.

| 구성 | 역할 |
|---|---|
| GitHub | 앱과 서버 코드 보관, 변경 이력 관리 |
| Cloudflare Worker | 앱의 AI 요청을 Gemini로 전달하는 HTTPS API |
| Supabase | 사용자 로그인과 가계부 데이터베이스 |
| Android APK / Google Play | 휴대폰에 설치·업데이트되는 실제 앱 |

Cloudflare Workers는 GitHub 저장소 변경으로 자동 빌드·배포할 수 있다. Android APK는 Android Gradle 빌드로 따로 만든다. [Workers Git 연동](https://developers.cloudflare.com/workers/ci-cd/builds/git-integration/)과 [Workers Builds 설정](https://developers.cloudflare.com/workers/ci-cd/builds/configuration/)을 참고한다.

### 추천 흐름

1. 먼저 Android 앱을 Debug APK로 빌드해 본인 기기에 설치한다.
2. Cloudflare Worker를 테스트용 URL로 배포하고 앱에서 AI 요청이 되는지 확인한다.
3. GitHub Actions로 APK 빌드·테스트를 자동화한다.
4. 다른 사람에게 배포할 때는 서명된 APK 또는 Google Play App Bundle을 만든다.

본인만 무료로 사용할 때는 4번 대신 **GitHub Release + 앱 안의 업데이트 다운로드**를 사용한다. 모아씀은 APK를 앱 안에서 받아 검증하고 Android 설치 화면을 열지만, 설치 허용과 최종 확인까지 자동으로 처리할 수는 없다.

---

## 2. 준비물

- Android 앱 프로젝트와 Gradle Wrapper (`gradlew` 또는 `gradlew.bat`)
- GitHub 계정과 앱 프로젝트 저장소
- Cloudflare 계정
- Supabase 프로젝트
- Gemini API 키
- Galaxy 기기 또는 Android 에뮬레이터

아직 프로젝트가 GitHub에 없다면 Android Studio에서 프로젝트를 정상적으로 빌드한 다음 새 GitHub 저장소에 올린다. Cloudflare와 연결하는 것은 앱 저장소가 아니라 **AI Worker 코드가 들어 있는 저장소 또는 하위 폴더**다.

같은 저장소에서 앱과 Worker를 함께 관리한다면 아래처럼 나누면 찾기 쉽다.

```text
budget-app/
├── app/                       # Android 앱
├── cloudflare/ai-worker/      # Cloudflare Worker API
└── .github/workflows/         # APK 자동 빌드
```

---

## 3. Cloudflare Worker를 GitHub에 연결하기

이 단계는 Worker API 코드와 `wrangler.jsonc` 또는 `wrangler.toml` 설정 파일이 저장소에 만들어진 뒤 진행한다. Cloudflare Worker가 아직 코드로 구현되지 않았다면 이 절차만으로 AI API가 생기지는 않는다.

### 새 Worker를 만들고 저장소 연결

1. Cloudflare 대시보드에서 **Workers & Pages**로 이동한다.
2. **Create application**을 누른다.
3. **Import a repository** 옆의 **Get started**를 선택한다.
4. GitHub 계정을 연결하고 Worker 코드가 있는 저장소를 선택한다.
5. 빌드 설정을 확인한다.
   - 저장소 전체가 Worker 프로젝트면 Root directory는 저장소 루트로 둔다.
   - 위 폴더 예시처럼 한 저장소에 앱과 Worker가 함께 있으면 Root directory는 `cloudflare/ai-worker`로 설정한다.
   - 일반적인 Worker는 Build command를 비워둘 수 있고, Deploy command는 `npx wrangler deploy`를 사용한다. 빌드가 필요한 템플릿이면 프로젝트가 안내하는 명령을 사용한다.
   - 대시보드 Worker 이름과 Wrangler 설정 파일의 `name` 값이 정확히 일치해야 한다.
6. 처음에는 테스트용 Worker 이름과 URL을 사용하고 **Save and Deploy**를 누른다.
7. 배포가 끝나면 대시보드에 표시된 `workers.dev` 주소를 복사한다.

기존 Worker를 연결하는 경우 **Workers & Pages → 해당 Worker → Settings → Builds → Connect**에서 GitHub 저장소를 연결한다. 기본 브랜치는 보통 `main`이다. 이후 연결된 브랜치에 push하면 빌드와 배포가 실행된다.

> `main`에 push하면 실제 Worker가 자동 갱신될 수 있다. AI 호출과 인증 검사를 테스트 Worker에서 먼저 확인한 다음 운영 브랜치에 반영한다. Worker가 앱 저장소의 하위 폴더에 있다면 Root directory를 지정한다. 프로젝트 이름 불일치나 폴더 경로 오류는 흔한 빌드 실패 원인이다.

Cloudflare는 연결 과정에서 저장소 권한 승인을 요청한다. 필요한 저장소만 선택하고, 개인 액세스 토큰을 소스 코드에 넣지 않는다.

---

## 4. API 키와 서버 비밀값 등록하기

Gemini 키를 Android 앱이나 공개 GitHub 저장소에 넣으면 APK를 분석해 키를 가져갈 수 있다. Gemini 키는 **Cloudflare Worker Secret**으로만 저장한다.

1. Cloudflare 대시보드에서 **Workers & Pages**로 이동한다.
2. 해당 Worker를 선택하고 **Settings**를 연다.
3. **Variables and Secrets**에서 **Add**를 누른다.
4. Type에서 **Secret**을 선택한다.
5. 변수 이름에 `GEMINI_API_KEY`, Value에 Gemini 키를 입력한다.
6. **Deploy**를 눌러 변경을 반영한다.

Cloudflare의 Secret 값은 Worker가 사용할 수 있지만 대시보드/Wrangler에서 이후 평문으로 보이지 않는다. 자세한 내용은 [Cloudflare Workers Secrets 공식 안내](https://developers.cloudflare.com/workers/configuration/secrets/)를 참고한다.

필요한 경우에만 `SUPABASE_URL`, 서버 전용 Supabase Secret Key 등의 서버 설정을 Worker에 추가한다. Supabase의 Service Role/Secret Key는 RLS를 우회할 수 있으므로 **Android 앱, GitHub 저장소, 공개 로그에 넣지 않는다.** Android 앱에서 Supabase에 직접 연결할 때는 publishable key와 올바른 RLS 정책을 사용한다. [Supabase 데이터 보안 안내](https://supabase.com/docs/guides/database/secure-data)

주의할 점:

- Worker Secret은 **빌드 설정용 비밀값**이 아니라 Worker 실행에 쓰는 **runtime secret**으로 등록한다.
- Gemini 키를 `local.properties`, `.env`, Gradle 파일에 넣고 APK에 포함시키지 않는다.
- GitHub Actions에서 Cloudflare로 직접 배포하도록 별도 자동화를 만들 경우에만 Cloudflare 배포 토큰이 필요하다. 이 가이드의 기본 방식인 Cloudflare Git 연동에서는 토큰을 코드에 직접 작성하지 않는다.
- 키를 실수로 GitHub에 push했다면 파일에서 지우는 것만으로 충분하지 않다. 해당 키를 폐기하고 새 키를 발급한다.

---

## 5. Worker와 Supabase 연결 확인

Android 앱은 `https://...workers.dev`로 끝나는 Worker 주소를 HTTPS API 주소로 사용한다. 이 주소는 비밀번호나 API 키가 아니지만, 올바른 운영/테스트 주소를 구분해 설정한다.

Android 프로젝트의 서버 주소 설정(예: `AI_API_BASE_URL` 또는 프로젝트에서 정한 동등한 설정)에 Worker 주소를 넣고 새 APK를 빌드한다. 이 주소는 앱이 API를 찾기 위해 포함되어도 되지만, `GEMINI_API_KEY` 값은 절대로 여기에 넣지 않는다. Worker의 실제 URL과 API 경로는 프로젝트 코드에 구현된 라우트와 일치해야 한다.

- Worker가 공개 Gemini 프록시가 되지 않도록 AI API 경로에서 Supabase 로그인 토큰 등 사용자 인증을 확인한다.
- 요청 크기와 호출 횟수에 제한을 둬 키 오용과 과도한 사용을 막는다.
- Supabase 테이블에는 RLS 정책을 적용하고, 다른 계정의 거래를 읽거나 수정할 수 없는지 테스트한다.
- CORS 설정은 브라우저 접근 제어용이다. Android 네이티브 앱 API의 사용자 인증을 대신하지 않는다.
- 첫 점검에는 실제 거래가 아니라 가짜 금액·가맹점으로 테스트한다. 민감한 거래 문구를 Cloudflare/GitHub 빌드 로그에 출력하지 않는다.
- 알림 AI 분류는 사용자가 앱 설정에서 켠 경우에만 동작한다. 금액/금융 단서로 추린 알림의 제목과 내용이 Gemini에 전송되므로 개인정보와 모델 사용량을 고려하고, 기본값은 꺼 둔다.

최소 점검:

1. Worker의 공개 상태 확인용 경로(예: `/health`)가 정상 응답하는지 확인한다.
2. Gemini 키가 등록되지 않았을 때 오류가 안전하게 처리되는지 확인한다.
3. 테스트 로그인 후 AI 입력 요청이 성공하는지 확인한다.
4. 로그에 키, 인증 토큰, 영수증 원문, 실제 거래 상세가 남지 않는지 확인한다.
5. 앱에서 Worker 주소를 사용할 때 인터넷 권한과 HTTPS 설정이 있는지 확인한다.

---

## 6. Android 앱을 내 휴대폰에 설치하기

### 가장 쉬운 방법: Android Studio에서 바로 실행

1. 휴대폰에서 **설정 → 휴대전화 정보 → 소프트웨어 정보**로 이동한다.
2. **빌드 번호**를 여러 번 눌러 개발자 옵션을 활성화한다.
3. 설정의 **개발자 옵션**에서 **USB 디버깅**을 켠다.
4. USB 케이블로 휴대폰을 PC에 연결하고, 휴대폰에 뜨는 디버깅 허용 창에서 허용한다.
5. Android Studio 상단 기기 목록에서 본인 Galaxy 기기를 선택한다.
6. **Run ▶**을 누른다. Android Studio가 Debug APK를 빌드해 설치하고 실행한다.

### APK 파일을 직접 만들어 옮기기

Android Studio 메뉴에서 **Build → Build Bundle(s) / APK(s) → Build APK(s)**를 선택해도 된다. 빌드 완료 알림의 링크를 눌러 APK 파일을 찾는다.

프로젝트 폴더의 터미널에서 실행한다.

```powershell
# Windows
.\gradlew.bat testDebugUnitTest assembleDebug
```

```bash
# macOS / Linux
./gradlew testDebugUnitTest assembleDebug
```

일반적인 APK 경로는 다음과 같다. 실제 모듈 이름이 다르면 `app` 폴더 대신 프로젝트에 있는 앱 모듈 폴더를 사용한다.

```text
app/build/outputs/apk/debug/app-debug.apk
```

APK를 USB나 본인 클라우드 드라이브로 휴대폰에 옮겨 열고 설치한다. Android가 안내하면 해당 파일 앱에 **이 출처의 앱 설치 허용**을 켠다. 이 APK는 개인 테스트용이며 Google Play 출시용 서명 APK/AAB를 대신하지 않는다.

v0.1.17부터 Debug 앱 ID는 `com.moasseum.app.qa`다. 테스트가 개인 원장을 수정하지 않도록 별도 설치되며, 기존 개인용 `com.moasseum.app` 앱의 업데이트가 아니다. 실제 사용 앱은 동일 키의 서명 Release APK로 업데이트한다.

---

## 7. GitHub에서 APK 빌드 결과 받기

GitHub Actions는 코드를 push할 때 Gradle 빌드를 자동 실행하고, 성공한 APK를 해당 실행 화면에서 내려받는 데 쓸 수 있다. 아래 파일을 저장소의 `.github/workflows/android-apk.yml`에 둔다.

> 아래 예시는 앱 모듈이 `app`, 기본 브랜치가 `main`, 프로젝트 Gradle이 JDK 17을 사용하는 경우다. 프로젝트의 모듈명·JDK·브랜치가 다르면 그 설정에 맞춰 바꾼다.

```yaml
name: Android Debug APK

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Get source code
        uses: actions/checkout@v6

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@017a9effdb900e5b5b2fddfb590a105619dca3c3 # v4.4.2

      - name: Allow Gradle Wrapper to run
        run: chmod +x ./gradlew

      - name: Test and build Debug APK
        run: ./gradlew testDebugUnitTest assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: budget-app-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

파일을 GitHub에 push한 후:

1. GitHub 저장소의 **Actions** 탭을 연다.
2. 가장 최근 `Android Debug APK` 실행을 선택한다.
3. 빌드가 초록색으로 성공했는지 확인한다.
4. 실행 화면 아래 **Artifacts**에서 `budget-app-debug-apk`를 내려받는다.
5. 내려받은 ZIP을 풀고 APK를 휴대폰에 설치한다.

Workflow artifact는 영구 배포물이 아니라 해당 실행에 붙는 빌드 결과이며, 보관 기간은 저장소 설정의 제한을 받는다. 계속 공유할 버전은 아래의 서명된 릴리스 절차를 사용한다. [GitHub Gradle 빌드 안내](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle), [GitHub workflow artifact 안내](https://docs.github.com/en/actions/tutorials/store-and-share-data)

---

## 8. 무료 개인용 서명 릴리스와 앱 내 업데이트

이 저장소의 `docs/android-release.yml`은 `main` push마다 서명 APK/AAB를 만들고 GitHub Release를 생성하는 workflow 템플릿이다. 현재 GitHub 토큰에 workflow 파일 등록 scope가 없으면 이 파일을 먼저 `.github/workflows/android-release.yml`로 옮길 수 없으므로, 권한을 추가한 뒤 이동한다.

### 한 번만 준비할 값

- Android upload keystore를 만든다. 키스토어와 비밀번호는 안전한 곳에 보관한다.
- GitHub 저장소 Settings → Secrets and variables → Actions에 아래 네 값을 등록한다.

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

`ANDROID_KEYSTORE_BASE64`에는 keystore 바이너리를 Base64로 변환한 값을 넣는다. 비밀번호·키스토어 파일은 README, GitHub 소스, APK, 빌드 로그에 절대 넣지 않는다.

### 릴리스가 만들어지는 과정

1. `main`에 기능을 push한다.
2. Actions가 unit test와 lint를 실행한다.
3. 템플릿은 Actions 실행 번호로 버전을 만든다. 활성화 전 기존 공개 APK보다 항상 큰 `versionCode`가 되도록 기준값을 조정해야 한다. 현재 수동 릴리스는 `gradle.properties`의 버전을 사용한다.
4. GitHub Release에 `app-release.apk`와 `app-release.aab`가 첨부된다.
5. 폰에서 모아씀의 `관리 → 앱 업데이트`를 열고 `업데이트 확인`을 누른다.
6. 앱이 GitHub Release의 APK를 직접 내려받아 크기·SHA-256(제공된 경우)·앱 ID·버전·서명을 검증한 다음 Android 시스템 설치 화면을 연다.
7. 처음이면 Android 설정에서 모아씀에 `이 출처의 앱 설치 허용`을 켜고, 시스템 설치 화면에서 업데이트를 승인한다.

APK 업데이트는 앱의 기존 Room 데이터를 삭제하지 않는다. 다만 Android는 보안상 앱이 설치 확인을 무음으로 승인하지 못하므로, 이 방식은 Play Store의 완전 자동 업데이트와 다르다.
Google Play Protect가 개인 APK를 위험 가능성으로 차단하는 경우에는 시스템 안내에서 APK 출처와 서명을 확인한 뒤 설치를 선택해야 한다. 앱에서 Play Protect를 우회하거나 대신 승인할 수는 없다.

## 9. 다른 사람에게 배포하기

### 제한된 인원에게 직접 전달

- 처음에는 Android Studio에서 **Build → Generate Signed Bundle / APK**로 서명된 APK를 만든다.
- APK를 GitHub Release에 첨부하거나 직접 전달한다.
- 새 버전을 업데이트로 설치하려면 같은 앱 ID와 같은 서명 키를 유지해야 한다.
- 서명 키 파일과 비밀번호는 저장소에 올리지 말고 안전한 곳에 백업한다. 서명 키를 잃거나 바꾸면 기존 설치 앱을 업데이트하지 못할 수 있다.

### Google Play에 올리기

- Play Console 배포에는 보통 서명된 Android App Bundle(`.aab`)을 사용한다.
- Android Studio에서 **Build → Generate Signed Bundle / APK → Android App Bundle**을 선택한다.
- 업로드 키/keystore를 안전하게 보관하고 GitHub에 올리지 않는다.
- Play Console의 내부 테스트 트랙에 먼저 올려 본인 기기에서 확인한 다음 정식 공개 여부를 결정한다.
- Play App Signing을 사용하는 경우 Google Play가 사용자에게 전달할 APK 서명을 처리하고, 업로드 키는 업로드 검증에 사용된다. 최신 요구사항은 [Android 앱 서명 공식 안내](https://developer.android.com/studio/publish/app-signing)에서 다시 확인한다.

> `app-debug.apk`를 GitHub Release나 Play Store의 정식 공개본으로 사용하지 않는다. Debug와 Release는 서명이 다를 수 있어 앱 업데이트가 막히거나 기존 로컬 데이터가 사라질 수 있다.

---

## 9. 배포 완료 전 체크리스트

### Cloudflare API

- [ ] Worker가 GitHub의 올바른 저장소·브랜치·Root directory를 보고 있다.
- [ ] Cloudflare 대시보드의 Worker 이름과 Wrangler 설정의 `name`이 일치한다.
- [ ] `GEMINI_API_KEY`가 Cloudflare runtime Secret으로 등록되어 있다.
- [ ] 테스트 AI 요청이 성공하며 사용량/오류 제한이 설정되어 있다.
- [ ] 인증하지 않은 사용자는 AI API를 호출할 수 없다.
- [ ] Worker 로그에 키나 실제 가계부 거래 내용이 남지 않는다.

### Supabase

- [ ] 필요한 테이블에 RLS가 활성화되어 있다.
- [ ] 사용자 A가 사용자 B의 거래를 읽거나 수정할 수 없다.
- [ ] Service Role/Secret Key가 앱이나 GitHub에 포함되지 않았다.

### Android APK

- [ ] `testDebugUnitTest`와 `assembleDebug`가 성공한다.
- [ ] Worker 주소가 올바른 환경(테스트/운영)을 가리킨다.
- [ ] 실제 Galaxy에서 설치, 시작, 거래 저장, 앱 재실행을 확인했다.
- [ ] 릴리스용 keystore를 만들었다면 저장소 밖에 안전하게 백업했다.
- [ ] APK를 다른 사람에게 전달하기 전 계정 삭제·로그아웃·샘플 데이터 여부를 확인했다.

---

## 10. 자주 막히는 문제

| 증상 | 먼저 확인할 것 |
|---|---|
| Cloudflare Worker 빌드 실패 | Root directory, Wrangler 설정 파일 위치, 대시보드 이름과 설정의 `name` 일치 여부 |
| push했는데 Worker가 배포되지 않음 | 연결된 GitHub 저장소와 브랜치, Workers & Pages → Worker → Settings → Builds의 상태 |
| AI 호출이 401/403으로 실패 | Secret 이름이 코드의 환경변수명과 같은지, Cloudflare에서 Secret 추가 뒤 Deploy했는지 |
| AI 호출이 429로 실패 | 무료 사용량/호출 한도와 재시도 정책 확인; 민감 정보나 키를 로그로 출력하지 말 것 |
| GitHub Actions가 APK를 못 찾음 | `app/build/outputs/apk/debug/app-debug.apk` 경로와 앱 모듈 이름 확인 |
| 휴대폰 설치가 거부됨 | Android의 출처별 앱 설치 허용, APK 다운로드가 완전히 끝났는지 확인 |
| 새 APK가 기존 앱 위에 설치되지 않음 | Debug/Release 서명 또는 앱 ID가 달라졌을 수 있음. 정식 업데이트는 같은 서명 키를 사용 |
| 앱에서 API 연결 실패 | APK에 들어간 Worker 기본 주소, HTTPS, 인증 토큰 전달, Worker 상태 확인 |

키가 로그나 공개 저장소에 노출되었다면 우선 키를 폐기하고 새 키로 교체한다. 단순히 파일에서 지우거나 저장소를 비공개로 바꾸는 것만으로는 노출된 키가 안전해지지 않는다.

## 11. 이 저장소에서 기능을 추가한 뒤 배포하는 순서

앞으로 기능을 만들 때 변경 위치에 따라 아래 순서를 적용한다.

### Android 로컬 기능 또는 화면만 변경

이 Mac에서 터미널로 빌드할 때 Android SDK/JDK가 자동으로 잡히지 않으면 아래처럼 해당 명령에만 경로를 지정한다. 시스템 글꼴이나 휴대폰 설정을 바꾸는 작업은 빌드·배포에 필요하지 않다.

```bash
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Android Studio나 다른 컴퓨터에서 SDK/JDK가 이미 설정되어 있으면 기존 명령만 실행해도 된다.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Room Entity/테이블을 바꾸면 `FinanceDatabase` 버전과 이전 버전에서 올라오는 Migration을 함께 추가한다. 기존 사용자 데이터가 유지되는지 Migration 경로를 확인한 뒤 릴리스한다.

이번 카드 이용기간·고정비 레이더·내역 정렬은 **Android APK만 배포**하는 변경이다. Worker/API와 Room 테이블은 바뀌지 않는다. 카드 설정은 기존 3필드 형식을 읽는 호환 디코더를 유지하고 새 저장 형식으로 확장했으므로, DataStore 변경도 기존 설정 읽기와 새 형식 재저장 테스트를 함께 수행한다. 폰의 시스템 글꼴 크기는 변경하지 않는다.

v0.1.17의 계좌·이체·전체 복원·PDF·카메라·예산 이월은 APK 변경이다. Room 3→4 Migration을 적용해 기존 거래에 nullable 계좌 연결을 추가하고 계좌 테이블을 생성한다. 설치 전 앱 삭제/데이터 초기화를 하지 않는다. 계좌 연결이 없는 기존 거래도 그대로 읽는다. 앱 글꼴 토큰과 시스템 글꼴 크기는 바꾸지 않는다. 로그인 코드 추가만으로 서버가 만들어지지는 않으며 아래의 별도 설정이 필요하다.

### AI Worker/API 변경

```bash
cd cloudflare/ai-worker
npm run typecheck
npm run deploy
```

기존 `GEMINI_API_KEY`는 Cloudflare Worker Secret으로 남아 있어야 한다. 새 코드를 배포한다고 Secret을 다시 출력하거나 앱에 복사하지 않는다. Worker 응답 형식도 바꾸면 앱 파서와 테스트를 함께 바꾼다.

### 앱 업데이트를 Galaxy에 전달

1. `gradle.properties`의 `VERSION_CODE`를 이전보다 1 이상 올리고 `VERSION_NAME`을 바꾼다.
2. `app/src/main/java/com/moasseum/app/ui/screens/WhatsNewDialog.kt`의 `ReleaseNotesCatalog`에 새 버전 슬라이드를 추가한다. 실제 QA 앱에서 변경 화면을 캡처하고, 개인정보·실사용 거래가 없는지 확인한 뒤 `app/src/main/res/drawable-nodpi/whats_new_<버전>_<기능>.jpg`로 포함한다. 슬라이드 순서는 팝업의 다음/이전 순서이며 제목·설명은 이미지 아래에 표시된다. 현재 버전의 카탈로그가 빠지면 `ReleaseNotesPolicyTest`가 빌드를 실패시킨다.
3. 같은 개인용 서명 키를 사용해 `assembleRelease`를 빌드한다.
4. 테스트가 통과한 뒤 코드를 `main`에 push하고 `app/build/outputs/apk/release/app-release.apk`를 [GitHub Releases](https://github.com/blossom0948/gagebu2/releases)에 새 버전으로 첨부한다.
5. 휴대폰에서 `관리 → 앱 업데이트 → 업데이트 확인`을 누르면 앱이 APK를 직접 내려받고 검증한 뒤 Android 설치 화면을 연다. 설치를 마치고 앱을 열면 해당 버전의 신기능 팝업이 한 번 표시된다. 필요한 경우 모아씀의 앱 설치 허용을 켜고, 시스템 설치 확인을 누른다.

기존 설치본을 유지한 채 업데이트하려면 앱 ID와 서명 키를 바꾸지 않는다. 현재 공개 v0.1.21/v0.1.22는 기본 Android Debug keystore의 SHA-256 `669674a2114fb59d85c56bba774b89fec9ce7f7ee79825769826130362a091c9`로 서명되어 있다. 별도 `moasseum-upload.jks`처럼 지문이 다른 키를 설정하면 빌드는 되더라도 기존 설치본의 업데이트가 거부된다. 릴리스 자동화 secret을 설정할 때도 이 지문과 일치하는 키만 사용하고, 매 릴리스 `apksigner verify --print-certs` 결과를 확인한다. 이 저장소는 Play Console 없는 개인 배포이므로 출처 허용과 설치 확인 단계는 자동으로 생략되지 않는다.

### 공동 계정/서버 동기화

모아씀 전용 Supabase 프로젝트와 앱 로그인은 v0.1.18에서 연결했다. Worker도 `REQUIRE_AUTH=true`로 배포했다. 다만 원장 테이블·RLS 정책·사용자 간 데이터 격리와 충돌 테스트를 하기 전에는 함께 쓰기/서버 동기화를 활성화하지 않는다. 로그인만으로 금융 기록을 자동 전송하지 않는다.

### 앱 로그인 연결

현재 연결: Supabase Free `moasseum` / 서울 / `https://nbkedjtzbdtjlkkkxrta.supabase.co`. 공개 URL/publishable key는 `gradle.properties`와 Worker vars에 있고, 비밀 키는 APK에 없다. 기존 `tngodvudrk` 등 다른 프로젝트는 수정하지 않았다. Supabase 관리자 로그인과 휴대폰 앱의 가입/로그인은 별개다.

1. 브라우저에서 사용자가 GitHub→Supabase 관리 계정 로그인/MFA를 마친다. 비밀번호/OTP를 채팅에 보내거나 저장소에 기록하지 않는다.
2. **모아씀 전용 무료 프로젝트**를 새로 만든다. 기존 `tngodvudrk` 프로젝트는 수정하지 않는다. 현재 Free는 활성 프로젝트 2개 한도이며, 1주 비활성 시 프로젝트가 일시 중지될 수 있다. 생성 제한에 걸리면 멈추고 사용자에게 선택을 요청한다. 유료 전환이나 다른 프로젝트 삭제를 대신 하지 않는다. [공식 Free 플랜/한도](https://supabase.com/pricing)
3. 프로젝트 URL과 **publishable key**를 확인한다. legacy anon 키도 허용하지만 secret/service_role 키는 APK에 금지한다. `GEMINI_API_KEY`는 기존 Worker Secret에만 유지한다.
4. 이메일 가입/로그인과 가입 확인을 켠 상태로 유지한다. 현재 새 Free 프로젝트는 기본 메일 템플릿 변경이 제한되므로, 기본 메일의 확인/재설정 링크를 **PKCE S256**으로 앱에 반환한다. 메일 링크는 요청한 휴대폰에서 열어야 하며, 앱의 암호화된 요청 검증값은 1시간 후 만료된다. 가입 인증 번호 입력은 향후 사용자 SMTP/템플릿을 설정한 경우의 보조 경로이며 현재 기본 흐름은 링크다. [PKCE 공식 문서](https://supabase.com/docs/guides/auth/sessions/pkce-flow), [Free 템플릿 변경 제한](https://supabase.com/changelog/46599-changes-to-email-template-customisation-on-free-tier)
5. Supabase `Authentication → URL Configuration`은 아래와 같다. 새 패키지/QA 패키지를 추가한다면 실제 콜백 주소만 허용하고 임의 웹 도메인 전체를 허용하지 않는다.

```text
Site URL: com.moasseum.app://auth/callback
Redirect URLs:
com.moasseum.app://auth/callback?**
com.moasseum.app.qa://auth/callback?**
```

6. 기본 SMTP는 프로젝트 조직의 허용 이메일만 대상으로 하며 시간당 2통 제한이다. 본인 휴대폰의 `관리 → 로그인·계정 → 회원가입`에서 Supabase 조직에 등록된 본인 이메일과 직접 정한 비밀번호를 사용하고, 그 휴대폰에서 메일 링크를 연다. 임의 이메일의 공개 회원가입은 별도 SMTP 공급자/발신자 설정이 필요하므로 비용·무료 한도와 계정 인증을 먼저 확인한다. 무료 개인 사용 요청에 맞춰 유료 전환/SMTP 구매/이메일 확인 해제는 하지 않았다. [SMTP 공식 문서](https://supabase.com/docs/guides/auth/auth-smtp)
7. URL/publishable key는 공개 가능한 설정이다. 다른 앱 전용 프로젝트로 바꿀 때 `gradle.properties` 또는 해당 빌드에 새 값을 전달하고 반드시 새 APK를 빌드한다. service_role/secret/Gemini 키는 파일·명령·로그·APK에 넣지 않는다.

```bash
./gradlew :app:assembleDebug \
  -PSUPABASE_URL=https://<새-모아씀-프로젝트>.supabase.co \
  -PSUPABASE_PUBLISHABLE_KEY=<publishable-key>
```

8. 격리된 QA 앱에서 로그인 → 암호화 저장 → 앱 재시작 → 세션 갱신 → 로그아웃과 잘못된 비밀번호/위조 링크 거부를 검증한다. 별도로 소유자 이메일의 실제 가입/복구 메일 수신 → 링크 완료를 확인한다. 현재 합성 계정의 실제 서버 로그인과 AI 호출은 통과했지만 **본인 이메일의 실수신/링크 완료는 아직 확인하지 않았다**. 자동 테스트를 실제 메일 수신 성공으로 대신 기록하지 않는다. 테스트 계정/임시 비밀번호는 확인 후 사용자 승인 아래 정리한다.
9. 동일 공개 설정으로 버전을 올린 서명 Release APK를 빌드/설치/게시한다. URL/key가 APK의 BuildConfig이므로 서버만 설정하고 v0.1.17 이하 APK를 유지하면 로그인이 켜지지 않는다. 기존 패키지/서명과 기기 기록을 유지한다.
10. 앱 로그인과 실제 서버 AI 호출이 확인되면 Worker에 같은 프로젝트 URL/publishable key와 `REQUIRE_AUTH=true`를 배포한다. v0.1.18에서 완료했다. 무인증/위조 401, 인증 장애 503, 올바른 토큰으로 네 AI 경로의 200을 확인했고 실제 Workerd 회귀 테스트도 추가했다. v0.1.17 이하의 서버 AI 사용자는 업데이트/앱 로그인이 필요하다. 기본 수동 기록·기기 내 알림 감지와 로컬 파싱 fallback은 유지된다.

로그인만으로 로컬 거래를 자동 동기화하지 않는다. 공동 장부 배포 상태는 아래 섹션을 따른다. [RLS 공식 문서](https://supabase.com/docs/guides/database/postgres/row-level-security)

### 구글 로그인 배포 (v0.1.19)

현재 모아씀 전용 Google Cloud 프로젝트 `moasseum-ai`의 Web application OAuth 클라이언트를 새 Supabase `moasseum`에 연결했다. Google OAuth 클라이언트 생성·비밀 키 서버 저장·소유자 테스트 사용자 등록은 각각 사용자 승인 후 수행했다. 다른 프로젝트/기존 Gemini 키/요금제는 변경하지 않는다.

1. Google Auth Platform의 Web application 클라이언트에는 아래 Supabase HTTPS 반환 주소 **한 곳만** 등록한다. Android 앱의 커스텀 스킴은 Google에 직접 등록하지 않는다.

```text
https://nbkedjtzbdtjlkkkxrta.supabase.co/auth/v1/callback
```

2. Client ID와 Client Secret을 **Supabase → Authentication → Providers → Google**에 저장하고 활성화한다. Client Secret은 APK·BuildConfig·GitHub·채팅·Worker 코드에 넣지 않는다. `Skip nonce checks`와 `Allow users without an email`은 끈 상태를 유지한다. 이메일 가입 확인도 켠 상태를 유지한다.
3. Google 인증은 현재 External/Testing이며 소유자 계정이 테스트 목록에 등록돼 있다. 다만 앱은 `openid email profile` 기본 로그인 범위만 요청한다. Google 정책상 이 기본 신원 범위만 사용하는 앱은 Testing 상태에서도 테스트 사용자 목록 밖 계정이 접근할 수 있는 예외가 있으므로, 파트너 이메일을 미리 추가해야 한다고 단정하지 않는다. 두 번째 계정 실제 로그인은 아직 검증되지 않았다. APK 공개 다운로드와 Google 인증 앱의 전체 공개 전환은 서로 다른 작업이다. [Google OAuth 테스트 사용자 예외](https://developers.google.com/identity/protocols/oauth2/production-readiness/overview)
4. 앱은 PKCE S256과 요청 식별값을 사용해 시스템 브라우저에서 인증한 뒤 기존 본앱/QA 반환 주소로 돌아온다. 검증값은 기기에 암호화하여 저장하고 1시간 후 만료한다. 권한은 `openid email profile`뿐이며 Gmail/Drive/오프라인 Google 접근을 요청하지 않는다. Google 공급자의 별도 access/refresh token은 저장하지 않는다.
5. 다음 검사는 실제 서버에서 공급자 활성화·이메일 확인 유지·두 패키지의 Google 리다이렉트/클라이언트/서버 반환 주소/권한 범위를 확인한다. 사용자 계정으로 로그인하거나 동의를 누르는 검사는 아니다.

```bash
node scripts/check-google-oauth.mjs
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

6. 구글 버튼이 포함된 v0.1.19 이상을 기존과 같은 앱 ID/서명 키로 빌드하고 GitHub Release에 올린다. 이번에는 Worker 코드와 Room 스키마 변경이 없어 Worker 재배포/Migration은 필요하지 않다. 구글 로그인은 무료 이메일 가입 메일 발송을 거치지 않으며 별도 SMTP 구매를 하지 않았다.
7. 휴대폰에서 업데이트 → `관리 → 로그인·계정 → Google로 계속하기` → 허용 계정 선택/사용자 동의 → 앱 복귀 → 로그인 표시 → 앱 재시작 → AI 호출 → 로그아웃을 검증한다. 계정 비밀번호/MFA는 사용자가 Google 화면에서 직접 처리한다. **2026-10-07에는 휴대폰 미연결로 실제 Google 로그인/복귀/설치를 실행하지 않았다.** 단위 테스트나 서버 리다이렉트 검사를 이 실기기 성공으로 대신 적지 않는다. [구글 로그인 QA](QA_GOOGLE_AUTH_REPORT_2026-10-07.md)

공급자/클라이언트만 교체하는 후속 작업은 Google/Supabase 설정과 위 실제 반환 주소 검사가 필요하다. 앱의 공개 프로젝트 URL/키·콜백 패키지·UI 코드가 바뀌면 APK도 새로 배포한다. [Supabase Google 로그인](https://supabase.com/docs/guides/auth/social-login/auth-google), [Google 버튼 가이드](https://developers.google.com/identity/branding-guidelines)

### v0.1.20 앱 오류 수정 릴리스

v0.1.20은 Android 앱 코드와 문구 리소스만 바꿨다. Worker/API 계약, Supabase 설정, Room Entity/스키마, 앱 ID, 기존 서명 키는 바뀌지 않아 Worker 재배포·Supabase 변경·Room Migration은 하지 않는다.

다음 유사 수정 때는 아래 순서로 진행한다.

1. `gradle.properties`에서 `VERSION_CODE`를 올리고 `VERSION_NAME`을 정한다.
2. `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebugAndroidTest`, `:app:assembleRelease`를 실행한다. 계정·기기·OS 의존 동작은 연결 기기에서 별도 검증하고, 미실행은 보고서에 명확히 남긴다.
3. Release APK의 패키지/versionCode/versionName과 서명 인증서 지문을 확인하고 SHA-256/파일 크기를 기록한다. 기존 개인 설치본 업데이트를 위해 앱 ID와 릴리스 키를 유지한다.
4. 범위가 앱 코드만이면 같은 소스 커밋을 `main`에 push하고, Release tag를 해당 커밋에 붙여 `app-release.apk`와 같은 버전 릴리스 노트를 GitHub Release에 게시한다. AI Worker 코드가 바뀐 경우에는 위 Worker 절차로 별도 테스트·배포한다.
5. 공개 Release API의 latest 태그와 asset digest를 확인하고, 공개 URL에서 APK를 다시 내려받아 로컬 SHA-256/크기와 비교한다. 그런 뒤 앱의 `관리 → 앱 업데이트` 경로와 실제 폰 설치는 각각 구분해 검증한다.

v0.1.20 빌드는 unit 114개 통과, lint 오류 0(경고 21개), Release APK 성공이다. Android instrumentation 10개는 APK에 컴파일됐으나 연결 기기가 없어 테스트 실행은 하지 않았다. 공개 배포/재다운로드 결과는 [2026-10-08 QA 보고서](QA_APP_AUDIT_REPORT_2026-10-08.md)에 기록한다.

### 다음 기능별 배포 판단

| 변경 | 필요한 배포/검증 |
| --- | --- |
| 화면·로컬 계산·PDF·카메라 | 테스트 후 새 버전 동일 키 APK |
| Room 필드/테이블 | Migration과 이전 데이터 보존 검사 후 APK |
| AI 프롬프트/API/인증 검사 | Worker typecheck·배포, 계약/앱 설정이 바뀌면 APK도 배포 |
| 로그인 프로젝트/공개 설정 | Supabase 이메일 설정·실제 인증 검사 후 새 설정 APK |
| Google 로그인/반환 주소 | Google OAuth·Supabase 공급자/기본 scope·PKCE 검사; 앱 코드/공개 설정 변경 시 APK, 실제 계정/휴대폰 복귀는 별도 검증 |
| 동기화/공동 원장 | 서버 Migration·RLS 격리/두 기기 충돌 검사 + Worker/앱 배포 |

현재 수동 GitHub Release 방식은 무료 개인 설치 경로이며, 관리→앱 업데이트에서 직접 다운로드한다. 무음 설치나 Android 보안 팝업 생략을 보장하지 않는다.

### 공동 장부 schema / 선택 공유 (2026-10-08)

- `supabase/migrations/202610080001_shared_ledger.sql`을 전용 `moasseum` Supabase SQL Editor에서 한 번 실행했다. 사전 검사에서는 대상 테이블이 없었고, 실행 후 `shared_*` 다섯 테이블 모두 RLS가 켜졌으며 초대 RPC 3개가 `SECURITY DEFINER`로 등록된 것을 확인했다. 초대 테이블은 직접 API 정책 없이 서버 RPC만 사용한다.
- DB에는 개인 거래를 넣지 않았다. 앱은 사용자가 선택한 거래와 공동 화면에서 직접 저장한 항목만 올리며, 계좌 식별자·알림 원문·개인 예산·반복 규칙은 공유하지 않는다. 한 장부 최대 2명, 20자리 초대 코드 24시간 만료·1회 사용이다.
- Room `5→6` Migration과 탭 UI·선택 공유·공동 월 합산/카테고리·목표/재정 설정·AI 분석/Q&A·PDF·할부·일괄 입력 코드를 추가했다. 공동 재정 항목은 해당 화면에서 직접 저장한 값만 동기화한다. v0.1.21 APK를 먼저 공개했지만 `202610080002_shared_goal_collaboration.sql`과 `202610080003_shared_finance_items.sql`은 아직 미적용이므로 공동 목표 진행액·공동 재정 저장은 사용할 수 없거나 오류가 날 수 있다. 두 계정 RLS 격리·오프라인 충돌·기존 DB 보존·실기기 UI도 미검증이다.
- 후속 조치: 전용 `moasseum` Supabase SQL Editor에서 두 migration을 번호 순으로 적용하고 RLS/컬럼을 확인 → 공동 목표·재정 CRUD 및 두 계정 격리, 오프라인 동기화, 기존 Room 데이터 보존, 실기기 업데이트를 검증한다. APK는 이미 v0.1.21로 게시했으므로 migration 적용 후 별도 APK 재배포는 필요하지 않다. 추가 앱 코드는 기존 절차대로 versionCode를 올리고 같은 서명키로 빌드·해시 검증한 뒤 GitHub Release에 올린다.

---

## 공식 참고 문서

- [Cloudflare Workers Builds 시작 및 GitHub 연결](https://developers.cloudflare.com/workers/ci-cd/builds/)
- [Cloudflare Workers Git integration](https://developers.cloudflare.com/workers/ci-cd/builds/git-integration/)
- [Cloudflare Workers 빌드 설정과 Root directory](https://developers.cloudflare.com/workers/ci-cd/builds/configuration/)
- [Cloudflare Workers Secrets](https://developers.cloudflare.com/workers/configuration/secrets/)
- [Supabase 데이터 보안과 RLS](https://supabase.com/docs/guides/database/secure-data)
- [GitHub Actions Gradle 빌드](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle)
- [GitHub Actions artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data)
- [Android 앱 서명](https://developer.android.com/studio/publish/app-signing)
