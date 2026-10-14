# 모아씀 Google Play 준비 체크리스트

상태: 준비 메모만 작성됨. Play Console 앱 생성·검토 제출·공개는 아직 하지 않았고, 아래 항목도 완료로 간주하지 않습니다. 사이드로드 v0.1.56 공개 기준, 2026-10-15입니다.

## 저장소에서 확인한 현재 상태

- 앱 ID: `com.moasseum.app`; 이번 릴리스 기준 공개 사이드로드 APK는 v0.1.56 / versionCode 56.
- Android 설정: minSdk 26, compileSdk/targetSdk 36, Android Gradle Plugin 8.10.1, Gradle wrapper 8.11.1, JDK 17. 2026-10-15에 Play 제출 target API와 Android 16 대응을 위해 올렸습니다. v0.1.56 검증에서 Fold4(Android 16) 격리 `.qa` 계측은 75건 중 74건 통과(실패 0, 외부 인증 E2E 1건 skip), 단위 테스트 296건, `:app:check`와 `lintDebug`를 통과했습니다. Lint는 오류 0·경고 34·힌트 7입니다. Play 병합 매니페스트는 targetSdk 36이며 자체 APK 설치 권한이 없습니다. 재현 검사는 `:app:verifyPlayStoreManifest` 또는 `:app:check`입니다.
- 로컬 빌드 환경: 이 Mac의 Android Studio JBR 25.0.3에서는 Kotlin 2.0.21 KAPT가 Java 버전을 파싱하지 못해 실패했습니다. 이번 검증은 저장소 설정을 바꾸지 않고 임시 JDK 17로 실행했습니다. 다음 toolchain 정리에서 JDK 17 Gradle 환경을 고정하거나 Kotlin plugin을 호환 버전으로 올려야 합니다.
- 배포 경로: 현재 공개 배포는 서명 APK/GitHub 업데이트 흐름입니다. `playStore` 빌드 타입은 Play 업데이트 경로를 사용하도록 자체 APK 업데이트 확인을 끄고 `REQUEST_INSTALL_PACKAGES`를 제거합니다. 기존 GitHub APK 배포 설정은 그대로 둡니다.
- 서명: Play 전용 upload key 설정값이 아직 제공되지 않았습니다. 기존 APK의 서명 키를 Play 업로드 키로 재사용하지 않습니다. Play App Signing을 선택하고 별도 upload key를 안전한 비밀 저장소에 보관해야 합니다.
- 기존 APK 사용자 전환 위험: 현재 GitHub APK는 기존 Gradle 설정상 Android debug 인증서로 서명되어 있습니다. Android는 설치된 앱과 서명 호환성이 없는 업데이트를 거부하며, 제거 후 다시 설치하면 기기 안의 개인 원장이 지워질 수 있습니다. Play App Signing에서는 업로드 키와 최종 APK를 서명하는 앱 서명 키가 별개이므로, 첫 Play 업로드 전 기존 사용자 업데이트/백업 복구 경로를 설계하고 격리 QA에서 검증해야 합니다. 키를 무심코 바꾸거나 운영 앱 위에 시험 설치하지 않습니다.
- QA 분리: debug 빌드는 `.qa` applicationId suffix를 유지합니다. Play variant는 운영 앱 ID를 사용하므로 실제 설치·업로드 검증 전까지 운영 앱에 설치하지 않습니다.
- 개인정보 흐름: 현재 방침은 로그인 식별 정보(Supabase 인증), 사용자가 선택한 공유 장부 항목·앨범 사진(Supabase), 사용자가 실행한 AI 요청(Gemini 중계), 별도 설정을 켠 경우의 금융 알림 텍스트 판별 전송, 기기 내 거래·설정 및 사진 OCR을 설명합니다. 코드 기준의 제출 전 분류 후보와 미확인 항목은 [Data safety 초안](PLAY_STORE_DATA_SAFETY_DRAFT.md)에 정리했습니다. SDK·서버 보존 설정까지 재확인한 뒤 Data safety 답변과 방침을 일치시켜야 합니다.

## 실제 제출 전에 끝낼 일

1. **계정 삭제 웹 경로**: 앱 안에는 관리 → 로그인·계정의 계정 삭제 요청 흐름이 있습니다. Play Console에 기입할 공개 HTTPS 삭제 요청 페이지는 아직 없습니다. 개인정보처리방침도 공개 URL에서 접근 가능하도록 해야 합니다. 공개 페이지에는 모아씀 식별, 삭제 요청 시작 방법, 필요한 본인 확인, 처리/보존 범위, 문의 연락처가 있어야 합니다.
2. **데이터·정책 선언**: Data safety와 데이터 삭제 질문을 실제 앱/네트워크/SDK 기준으로 작성합니다. 금융 기능 선언은 제공 기능에 맞게 작성해야 하며, 금융 기능이 없는 앱도 선언 제출 대상입니다. 알림 접근 권한, 사진 선택, 계정, AI/서버 전송의 고지·동의와 Play 정책을 함께 검토합니다. 대출·결제·송금 제공 앱이라고 잘못 분류하거나 주장하지 않습니다.
3. **스토어 정보**: 앱 이름/간단·전체 설명, 앱 아이콘, 1024×500 기능 그래픽, 휴대폰/태블릿 스크린샷, 카테고리, 콘텐츠 등급, 광고 여부, 대상 연령, 앱 액세스 검토 안내와 지원 연락처를 준비합니다. 실제 앱 화면만 사용하고 “완전 무료/무제한”처럼 서버 무료 한도를 넘어서 보장하는 문구는 쓰지 않습니다.
4. **번들 및 서명**: 다음 Play 후보의 versionCode를 56보다 올리고, Play 전용 upload key로 AAB를 빌드합니다. 키는 저장소·APK·채팅에 넣지 않고 백업/복구 절차를 마련합니다. Play App Signing에 등록한 후 내부 테스트 트랙에서 설치·업데이트를 확인합니다.
5. **개발자 계정 테스트 조건 확인**: 개인 개발자 계정 생성일이 2023-11-13 이후라면, 현재 Google 안내상 프로덕션 액세스 신청 전 비공개 테스트에 12명 이상이 14일 연속 옵트인해야 합니다. 계정 유형/생성일은 이 저장소에서 확인할 수 없습니다.
6. **계정·본인 확인 비용과 공개 정보**: Play Console 계정은 현재 1회 US$25 등록비가 안내되어 있습니다. 개인 계정은 결제 프로필의 법적 이름/주소와 확인 가능한 연락처 등이 필요하고, 새 개인 계정은 Play Console 앱으로 실제 Android 기기 접근을 확인해야 할 수 있습니다. 개발자 이름/지원 연락처로 무엇이 공개되는지 및 개인정보 영향을 가입 직전에 확인합니다. 계정 유형과 한국 지역의 추가 조건은 실제 제공 기능·수익화 여부·Play Console 안내에 맞춥니다.

## 공식 기준

- [Play target API 요구사항](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en) — 2026-08-31 이후 신규 앱/업데이트 API 36 이상.
- [AGP 8.10 호환성](https://developer.android.com/build/releases/past-releases/agp-8-10-0) — API 36 지원, Gradle 8.11.1 이상 및 JDK 17.
- [새 개인 개발자 계정 비공개 테스트](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) — 해당 계정은 최소 12명, 14일 연속 옵트인.
- [Play Console 계정 가입](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en) · [개발자 계정 인증 정보](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en) — 현재 등록비·신원/연락처 요구사항.
- [앱 계정 삭제 요구사항](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en) — 앱 안과 외부 웹 경로 모두 필요.
- [Data safety 작성 안내](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) — 제3자 SDK의 데이터 처리도 포함해 정확하게 작성.
- [금융 기능 선언](https://support.google.com/googleplay/android-developer/answer/13849271?hl=en) — 게시 앱은 해당 선언을 제출해야 함.
- [Android App Bundle](https://support.google.com/googleplay/android-developer/answer/9844679?hl=en) · [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en).
- [Android 앱 업데이트의 서명 조건](https://developer.android.com/google/play/app-updates) — 기존 설치와 서명 호환이 없으면 앱 제거가 필요할 수 있고 기기 데이터가 삭제됩니다.

이 체크리스트는 콘솔 정책 답변을 대신하지 않습니다. Play 등록을 시작할 때 계정 상태와 최신 콘솔 질문을 다시 확인합니다.
