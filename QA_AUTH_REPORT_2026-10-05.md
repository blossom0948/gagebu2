# 모아씀 v0.1.18 로그인·AI 연결 검증 — 2026-10-05

## 구현 / 변경 범위

- 별도 Supabase Free `moasseum` 프로젝트(서울)를 생성하고 앱 URL/publishable key를 연결했다. 기존 `tngodvudrk`와 다른 프로젝트는 변경하지 않았다. 유료 업그레이드/SMTP 구매 없이 이메일 가입 확인을 유지했다.
- 앱 기본 이메일 링크의 PKCE S256 검증값과 요청 식별값을 Android Keystore AES-GCM/AtomicFile로 저장했다. OS 백업에서 제외하며, 1시간 만료·재전송·앱 프로세스 재생성·다른 요청/계정·중복 파라미터·토큰 주입·위조/재사용을 검사한다. 복구 세션은 새 비밀번호 설정 전 일반 로그인으로 취급하지 않는다.
- 생산/QA 패키지별 `://auth/callback` 반환 주소를 설정했다. 앱에서 요청하지 않은 링크는 성공 화면 대신 오류 안내를 표시한다.
- Worker `REQUIRE_AUTH=true`로 실제 Supabase 사용자 확인 후 AI 호출을 허용한다. APK에는 publishable key만 들어가며 Gemini/관리자 secret은 포함하지 않았다.
- 기본 기록/기기 내 금융 알림 감지와 기존 로컬 fallback을 유지한다. 로그인은 원장 업로드·공동 공유·동기화가 아니다.

## 발견한 오류 / 수정 / 재검증

| 발견 | 원인 / 수정 | 재검증 |
| --- | --- | --- |
| Supabase 직접 로그인은 성공하지만 Worker 인증에서 503, 앱은 로컬 인식으로 fallback | 실제 Workerd가 `fetch`의 `redirect: "error"`를 거부했다. `manual`로 바꾸고 모든 3xx를 거부해 토큰을 다른 주소로 넘기지 않도록 했다. 장애 응답은 503으로 구분하며 로그에 요청/토큰/본문을 쓰지 않는다. | 실제 Worker 네 AI 경로의 인증 200, 무인증/위조 401, native AiClient 결과 `SERVER` 확인. Miniflare/Workerd 회귀 테스트 추가. |
| 과거/재설치/다른 기기의 링크가 홈만 열어 인증 성공으로 오해할 수 있음 | 올바른 형태의 반환 링크 이벤트에 인증 화면을 띄우고 저장된 요청 유무/식별값/만료를 안내하도록 수정 | 위조 링크 실기기 실행에서 성공 로그인 없이 명시적 오류 표시, 단위 테스트 통과 |
| 새 무료 프로젝트에서 이메일 인증 번호 템플릿 수정 불가 | 기본 템플릿 링크를 지원하는 PKCE 흐름 추가. 이메일 확인을 끄거나 유료로 전환하지 않음 | 반환 주소 저장 확인, 링크 보안 단위 테스트 통과. 실제 소유자 메일 수신/링크 완료는 미실행 |

## 실행한 검사

- Android 단위 테스트 **84개**: 실패/오류/skip 0.
- Galaxy Z Fold4 / Android 16(API 36) 격리된 `com.moasseum.app.qa` 실기기 테스트 **9개**: 전체 통과. 금융/계좌/이체/복원 검사와 실제 서버 로그인·암호화 저장·Repository 재생성·갱신·로그아웃, 실제 서버 AI 사용 포함. 생산 앱 데이터를 테스트로 수정하지 않았다.
- Worker TypeScript typecheck 성공, Node + 실제 Workerd 테스트 **10개** 통과. 없는/위조된 토큰, 잘못된 사용자 응답, 리다이렉트, 인증 장애, 호출 제한, 정상 Gemini 후보, health 검사.
- 실제 Supabase: 이메일 활성/가입 확인 유지, 잘못된 비밀번호 400, 로그인/사용자 조회/갱신 200, 로그아웃 204, 이후 refresh 거부, 무인증 사용자 조회/위조 PKCE 거부.
- 실제 AI 네 경로(`/v1/parse-transaction`, `/v1/analyze-spending`, `/v1/ask-spending`, `/v1/classify-notification`): 무인증·위조 401, 올바른 QA 토큰 200. 실제 금융 기록이 아닌 합성 입력만 전송했다.
- `lintDebug`: 오류 **0**, 경고 22(기존 dependency/API 등). Release 빌드/lintVital 성공. `git diff --check` 별도 검사.
- 배포 Worker 버전: `6618c22f-aff7-4f69-ad72-634019c59750`. `/health`는 공개이며 `authRequired: true`를 반환한다.

재현용 `scripts/check-remote-auth.mjs`는 명시적으로 전달한 `@example.invalid` 합성 계정 파일만 허용하고 비밀번호/토큰을 출력하지 않는다. 새 테스트 계정 생성과 실서비스 호출에는 별도 승인과 사용량 확인이 필요하다. 검사 후 임시 파일을 저장소에 남기지 않는다.

## 실기기 업데이트 / 정리

- 같은 패키지 `com.moasseum.app`에 `adb install -r`로 v0.1.18 설치 성공. 기존 월 기록/합계·목표 지출 유지 확인. 시스템 `font_scale=0.8`을 변경하지 않았다.
- 생산 앱 `관리 → 로그인·계정`에서 서버 연결된 로그인/회원가입 화면 확인. 긴 설명/버튼이 커버 화면 밖으로 벗어나지 않음. 사용자 실제 자격 증명을 입력하지 않았다.
- 검증용 합성 Supabase 계정 하나를 사용자 삭제 승인 후 삭제하고 사용자 목록에서 제거를 확인했다. 임시 0600 자격 증명 파일과 본 작업의 격리된 QA APK 두 개도 제거했다. 실제 계정/가계부 데이터/프로젝트는 유지했다. 테스트 계정 삭제는 되돌릴 수 없으며 필요 시 별도 승인 아래 다시 생성한다.
- APK: versionCode **18**, versionName **0.1.18**, **56,905,458 bytes**.
- SHA-256: `172d481f7915d9916a27c4d809d6618ce62ced3830c1c7722a3fed91e3a23521`.
- 서명 인증서 SHA-256: `669674a2114fb59d85c56bba774b89fec9ce7f7ee79825769826130362a091c9`(기존 릴리스와 동일).
- [공개 v0.1.18 Release](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.18)를 게시했다. 무인증 GitHub API에서 latest/draft=false/prerelease=false와 크기·asset digest를 확인했고, 공개 APK를 다시 내려받아 빌드·설치 APK 세 파일의 SHA-256 일치를 확인했다. 소스 커밋 `cec509c`.

## 미검증 / 미구현 — 완료로 간주하지 않음

- 실제 본인 이메일의 회원가입/복구 메일 **수신 및 긍정 PKCE 링크 완료는 아직 확인하지 않았다**. 합성 계정은 메일 없이 관리자 확인한 검사 전용 계정이었다. 실제 앱 로그인 성공을 이메일 실수신 성공으로 확대하지 않는다.
- 무료 기본 SMTP는 프로젝트 조직의 등록 이메일만 대상이고 시간당 2통 제한이다. 임의 이메일을 대상으로 하는 공개 회원가입에는 별도 SMTP 설정이 필요하다. [공식 SMTP 문서](https://supabase.com/docs/guides/auth/auth-smtp), [새 Free 프로젝트 템플릿 제한](https://supabase.com/changelog/46599-changes-to-email-template-customisation-on-free-tier).
- 공동 장부·초대 QR/코드·서버 원장·두 사용자 RLS 격리·오프라인 동기화는 미구현. 로그인만으로 자동 공유하지 않는다.
- Worker 호출 제한은 사용자별 **isolate 메모리** 기준이며 전 세계 단일 누적 쿼터나 청구 차단 장치가 아니다. Supabase/Cloudflare/Gemini 무료 사용량 범위 내 개인용 구성이며 무제한 무료를 보장하지 않는다.
- v0.1.17 이하의 서버 AI는 새 인증 요구사항 때문에 업데이트/앱 로그인이 필요하다. 기기 내 기본 금융 감지와 기록은 유지한다.
