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
3. `versionCode`에는 Actions 실행 번호가 들어가고, 서명 APK/AAB가 만들어진다.
4. GitHub Release에 `app-release.apk`와 `app-release.aab`가 첨부된다.
5. 폰에서 모아씀의 `관리 → 앱 업데이트`를 열고 `업데이트 확인`을 누른다.
6. 앱이 GitHub Release의 APK를 직접 내려받아 크기·SHA-256(제공된 경우)·앱 ID·버전·서명을 검증한 다음 Android 시스템 설치 화면을 연다.
7. 처음이면 Android 설정에서 모아씀에 `이 출처의 앱 설치 허용`을 켜고, 시스템 설치 화면에서 업데이트를 승인한다.

APK 업데이트는 앱의 기존 Room 데이터를 삭제하지 않는다. 다만 Android는 보안상 앱이 설치 확인을 무음으로 승인하지 못하므로, 이 방식은 Play Store의 완전 자동 업데이트와 다르다.

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

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Room Entity/테이블을 바꾸면 `FinanceDatabase` 버전과 이전 버전에서 올라오는 Migration을 함께 추가한다. 기존 사용자 데이터가 유지되는지 Migration 경로를 확인한 뒤 릴리스한다.

### AI Worker/API 변경

```bash
cd cloudflare/ai-worker
npm run typecheck
npm run deploy
```

기존 `GEMINI_API_KEY`는 Cloudflare Worker Secret으로 남아 있어야 한다. 새 코드를 배포한다고 Secret을 다시 출력하거나 앱에 복사하지 않는다. Worker 응답 형식도 바꾸면 앱 파서와 테스트를 함께 바꾼다.

### 앱 업데이트를 Galaxy에 전달

1. `gradle.properties`의 `VERSION_CODE`를 이전보다 1 이상 올리고 `VERSION_NAME`을 바꾼다.
2. 같은 개인용 서명 키를 사용해 `assembleRelease`를 빌드한다.
3. 테스트가 통과한 뒤 코드를 `main`에 push하고 `app/build/outputs/apk/release/app-release.apk`를 [GitHub Releases](https://github.com/blossom0948/gagebu2/releases)에 새 버전으로 첨부한다.
4. 휴대폰에서 `관리 → 앱 업데이트 → 업데이트 확인`을 누르면 앱이 APK를 직접 내려받고 검증한 뒤 Android 설치 화면을 연다. 필요한 경우 모아씀의 앱 설치 허용을 켜고, 시스템 설치 확인을 누른다.

기존 설치본을 유지한 채 업데이트하려면 앱 ID와 서명 키를 바꾸지 않는다. 이 저장소는 Play Console 없는 개인 배포이므로 출처 허용과 설치 확인 단계는 자동으로 생략되지 않는다.

### 공동 계정/서버 동기화

Supabase 프로젝트와 RLS 정책을 실제로 만들고 사용자 간 데이터 격리 테스트를 하기 전에는 함께 쓰기 기능을 활성화하지 않는다. 현재 Worker의 `REQUIRE_AUTH=false`는 개인 테스트 전용이며 공개 서비스용 설정이 아니다.

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
