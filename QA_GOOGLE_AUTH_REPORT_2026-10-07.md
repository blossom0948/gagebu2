# 구글 로그인 구현·서버 설정 QA — 2026-10-07

## 범위와 결과

v0.1.19에서 `관리 → 로그인·계정 → Google로 계속하기`를 추가했다. 모아씀 전용 Google/Supabase 서버 설정과 실제 인증 시작 검사는 완료했다. 휴대폰이 연결되지 않았으므로 실제 Google 계정 로그인·사용자 동의·휴대폰 앱 복귀·APK 설치는 아직 검증하지 않았다.

기존 전역 글꼴 크기·민트 테마·가계부 테이블은 변경하지 않았다. Google 버튼만 공식 로고와 Google Sans를 사용하고 라이선스를 APK에 포함했다. 같은 앱 ID와 기존 서명 키를 유지한다. 기존 기기 데이터 유지 여부를 v0.1.19 실기기에서 확인했다고 주장하지 않는다.

## 승인한 서버 변경

- 기존 앱 전용 Google Cloud 프로젝트 `moasseum-ai`에 Web application 클라이언트 `모아씀 Supabase 로그인` 한 개 생성. JavaScript origin은 추가하지 않고 반환 주소는 `https://nbkedjtzbdtjlkkkxrta.supabase.co/auth/v1/callback` 한 곳만 허용
- 생성한 Client ID/Secret을 Supabase Free `moasseum` Google 공급자에 저장하고 활성화. 비밀 키는 서버 입력에서만 사용하고 파일·APK·GitHub·채팅에 공개하지 않음
- Google External/Testing 상태 유지. 사용자 승인받은 소유자 계정 하나만 테스트 사용자에 등록하며 다른 계정 초대/전체 공개/유료 전환은 하지 않음
- Supabase 이메일 가입 확인 유지. Google nonce 검사와 이메일 없는 계정 거부 유지. 기존 본앱/QA 반환 allowlist 유지
- 다른 프로젝트·기존 Gemini 키·Worker 코드·Room 스키마 변경 없음. 금융 원장을 서버에 업로드하지 않음

## 앱 구현과 안전장치

- 시스템 브라우저의 Google 계정 선택을 사용하며 WebView에 자격 증명을 입력받지 않음
- PKCE S256 검증값/무작위 요청 식별값을 기기 내 암호화 저장. 최신 요청/정확한 콜백 스킴·호스트·경로/1시간 만료/재사용 거부 확인
- 콜백의 토큰을 직접 받아 로그인하지 않고 서버에서 code+verifier를 교환. Google identity가 있는 서버 세션만 Google 요청 완료로 저장
- 기존 이메일 인증 요청/세션 저장 형식과 호환. Google 공급자 미준비 시 기존 이메일 요청을 덮어쓰지 않음
- 브라우저 미설치/실행 거부/로그인 취소/오래된 요청/잘못된 공급자/로그아웃 후 반환을 처리
- 권한은 OpenID·이메일·기본 프로필만 요청. Gmail·Drive·가계부 권한과 offline access는 요청하지 않고 별도 Google access/refresh token도 저장하지 않음
- 세션 암호화/OS 백업 제외 유지. 로그인만으로 동기화/공동 장부가 활성화되지 않음

코드 점검 중 손상된 인증 요청 저장소에서 다이얼로그를 닫을 때 예외가 전파될 수 있는 잠재 오류를 보완했다. 인증 요청 정리 오류를 안내하고 앱 충돌을 막는 회귀 테스트를 추가했다. 실사용자 폰에서 발생한 오류를 재현한 것은 아니다.

## 실행한 검사

| 검사 | 결과 |
| --- | --- |
| JVM 단위 테스트 | 95개, 실패/오류/건너뜀 0; Google 관련 11개 포함 |
| Android lint | 오류 0, 기존 경고 22개 |
| 서명 Release 빌드/서명 검증 | 성공; 패키지 `com.moasseum.app`, versionCode 19, versionName 0.1.19 |
| Android 테스트 APK | 빌드 성공; 기기 테스트 실행은 미실행 |
| 실제 Supabase settings | Google/이메일 활성화, 이메일 가입 확인 유지 |
| 실제 본앱/QA authorize 시작 | Google 리다이렉트, 설정한 클라이언트·Supabase 반환 주소·계정 선택·기본 권한 범위 확인 |
| 실제 Google 사용자 로그인·동의·토큰 교환·폰 복귀 | 미실행 — 휴대폰 미연결/사용자 인증 필요 |

서버 검사: `node scripts/check-google-oauth.mjs`. 무작위 합성 PKCE 요청만 시작하며 Google 계정에 로그인하지 않고 state/token/전체 인증 URL을 출력하지 않는다. 새 테스트 사용자 계정이나 금융 기록도 생성하지 않는다. 서버 요청 생성 기록은 실제 Google 사용자 인증 성공을 의미하지 않는다.

v0.1.18에서 실행한 Android 9개·Worker 10개 테스트와 합성 계정 실제 로그인/AI 검증은 이전 버전의 결과다. 이를 v0.1.19 Google 실기기 검증으로 간주하지 않는다.

## APK와 공개 배포

- APK: `app/build/outputs/apk/release/app-release.apk`
- 크기: 59,131,229 bytes
- SHA-256: `d9dee61e2c9a954f916d9afa94b40267032d485262e380d419008909530d416c`
- 기존 서명 인증서 SHA-256: `669674a2114fb59d85c56bba774b89fec9ce7f7ee79825769826130362a091c9`
- 공개 게시/latest/asset digest/재다운로드 확인 결과는 게시 후 아래에 기록한다. 빌드 성공만으로 공개 배포 성공으로 간주하지 않는다.

## 남은 확인

1. 허용한 본인 계정으로 Google 로그인/동의 후 휴대폰 앱 복귀 및 로그인 표시
2. 앱 재시작 세션 복원·인증된 AI 호출·로그아웃 및 취소/다른 계정 선택 안내
3. 기존 설치본 업데이트와 개인 기록/목표 유지, 좁은 화면/다크 모드 버튼 표시
4. 이메일 실제 가입/복구 메일 수신·링크 완료는 기존 미검증 항목으로 유지

함께 쓰기·서버 금융 원장·두 계정 RLS 격리·오프라인 동기화는 아직 미구현이다. APK의 공개 다운로드는 Google 인증의 전체 공개/심사 완료를 뜻하지 않는다. 후속 배포 절차는 [배포 가이드](BUDGET_APP_DEPLOYMENT_GUIDE.md)의 구글 로그인 절을 따른다.
