# 모아씀 구현 상태

기준 문서: `WEEPLE_INSPIRED_BUDGET_APP_MASTER_PLAN.md`

## 구현된 개인용 기능

- Kotlin + Jetpack Compose, Room, DataStore 기반 Android 앱과 홈·내역·알림 후보함·관리 화면
- 월 목표 지출, 진행 막대, 수입·지출 합계, 카테고리 리포트
- 카테고리별 예산 한도와 홈 카테고리 사용률 표시
- 거래 추가, 상세, 수정, soft delete와 삭제 직후 실행 취소
- 카드 이름·결제일 관리와 결제수단 관리
- 카드 이용기간 종료일/종료월과 연결 결제수단 설정, 결제월별 사용액 합산·거래 내역 펼치기(카드사 청구액 아님)
- 구독·고정비 레이더: 월 예정액, 7일 예정액, 30일 일정, 기록된 반복/그 외 지출
- 반복 거래 규칙 수정(금액·날짜·결제수단·메모 등), 기존 기록·활성 상태 보존과 처리 월 중복 방지
- 내역 결제수단 필터와 정렬 4종, 검색/필터 결과의 정확한 건수·수입/지출 합계
- 날짜·유형·카테고리·가맹점·메모·결제수단·계좌 연결을 CSV로 내보내고 중복 없이 가져오기
- 계좌·지갑 등록/수정/보관, 시작 잔액과 연결 거래를 통한 잔액 계산, 이체 추가/수정/삭제 취소; 이체는 수입·지출 통계에서 제외(실제 은행 송금 아님)
- 월 예산 이월, 남은 하루 사용 가능 금액, 월말 예상 지출과 지난달 같은 기간 비교
- JSON 전체 백업/복원: 거래(삭제 표시 포함)·계좌·월 예산·반복 규칙·결제수단·카드·카테고리·화면 설정. 이전 거래 전용 JSON은 기존 데이터를 대체하지 않고 합침
- 전체 복원 전 내부 안전 백업 생성과 내보내기. 로그인 토큰·비밀번호·AI 알림 전송 동의·원문 알림 후보는 백업에서 제외
- 선택 월의 카테고리 합계와 내역을 한국어 다중 페이지 PDF로 내보내기
- CSV 중복 가져오기는 동일 거래 필드를 기준으로 건너뜀
- 7개 기본 카테고리 이름 변경과 사용자 카테고리 추가·수정·삭제(최대 20개); 삭제 시 기존 거래·반복 규칙·알림 후보를 기타로 이동
- 반복 지출/수입 규칙을 매월 지정일에 기록; 앱 미실행 중에도 WorkManager가 하루 한 번 처리하고 앱 시작 시 놓친 회차를 보충
- 한국어 자연어 입력을 거래 후보로 해석한 뒤 사용자가 검토하고 저장
- Android 시스템 음성 인식 앱을 호출하는 음성 입력
- 번들된 ML Kit 한국어 OCR로 사진 선택 또는 시스템 카메라 촬영 후 기기 내 영수증 인식; 이미지는 Worker로 보내지 않음
- 사용자가 실행한 AI 월간 분석: 합계·예산·카테고리 합계만 Worker로 전송, 가맹점/메모/거래별 내역은 제외
- 사용자가 실행한 월간 소비 Q&A: 집계 수치만 Worker로 전송하고 답변을 후보 거래로 저장하지 않음
- Android 알림 접근 설정과 앱 알림 권한 안내, 금융 알림 후보를 앱 팝업/시스템 알림으로 확인 후 저장
- 제조사별 알림 텍스트 필드·메시지형 알림 추출, 서비스 연결/마지막 수신/후보 생성 진단과 재연결
- Galaxy 알림 접근 설정의 제조사별 진입 차이를 고려한 fallback, 정확한 리스너 컴포넌트 확인과 bounded 자동 재바인딩
- 알림 AI 분류는 선택 동의 기능: 기기에서 금액/금융 단서로 미리 거른 제목·내용만 Gemini에 보내며, 숫자만 있는 알림·광고·쿠폰·예정·취소는 제외하도록 분류
- 다크 모드·모션 줄이기, 앱 내 APK 직접 다운로드·파일 검증·Android 설치 화면 연결
- 관리 화면에서 거래·계좌·예산·알림 후보·반복 규칙을 확인 후 삭제 가능(설정과 로그인 상태는 보존)

## 로그인 구현과 아직 완료되지 않은 부분

- `관리 → 로그인·계정`에 이메일 로그인·회원가입·인증 번호 확인/재발송·비밀번호 재설정·로그아웃 화면과 Supabase Auth REST 클라이언트를 구현
- 세션은 Android Keystore AES-GCM으로 암호화하고 OS 백업에서 제외. 비밀번호는 저장하지 않음. 토큰 갱신/만료/오프라인 실패/로그아웃 처리를 자동 테스트
- v0.1.18: 모아씀 전용 Supabase Free 프로젝트 `moasseum`(서울) 생성, URL/publishable key 등록. 관리자 로그인과 앱 가입은 별개이며 다른 프로젝트 `tngodvudrk` 등은 수정하지 않음
- 기본 무료 메일의 소유자/조직 이메일 제한과 시간당 2통을 화면에 설명. 가입·복구 링크는 PKCE S256/기기 내 암호화 검증값/요청 식별값으로 검증하며, 앱 재실행·재전송·만료·위조·다른 계정·재사용을 검사
- 실제 서버 로그인·세션 갱신·앱 재시작 복구·로그아웃과 인증된 AI 호출을 합성 계정으로 검증. 실제 소유자 이메일의 가입/복구 메일 수신·링크 완료와 두 계정 RLS 격리 테스트는 미실행. 가짜 Auth API 단위 테스트를 메일 실수신 성공으로 간주하지 않음
- 공동 장부·만료되는 초대 코드/QR·공동 목표·기기 간 오프라인 동기화는 미구현. 로그인만으로 기기 금융 기록을 서버에 올리거나 공유하지 않음

## 외부 연결 상태와 제한

- Cloudflare Worker: `https://moasseum-ai-worker.blossom0948.workers.dev`
- Worker 경로: `/health`, `/v1/parse-transaction`, `/v1/analyze-spending`, `/v1/ask-spending`, `/v1/classify-notification`
- Gemini 키는 Worker Secret `GEMINI_API_KEY`로만 관리하며 APK에 포함하지 않음
- v0.1.18 Worker `REQUIRE_AUTH=true`: 같은 전용 Supabase에서 로그인 토큰 확인, 무인증/위조 401, 인증 장애 503, 인증 사용자별 호출 제한. 실제 네 경로의 인증된 200 응답 검증
- Supabase 인증은 연결됐지만 사용자별 원장 테이블/RLS/오프라인 동기화·함께 쓰기·초대 코드는 미구현이며, 해당 메뉴는 알림 후보함으로 유지
- 알림 읽기는 Android 특수 접근 권한이라 사용자가 시스템 화면에서 직접 허용해야 함. Android 13 이상에서는 모아씀 알림 표시 런타임 권한도 필요
- AI 알림 분류는 기본 꺼짐. 켜면 해당 알림 제목·내용이 외부 AI 서버로 전송되고 Gemini 사용량이 발생할 수 있음
- OCR은 사진 보관 기능이 아니며 선택/촬영 이미지에서 후보를 만들 뿐. 촬영 임시 파일은 처리/취소 후 제거. 인식 결과를 반드시 확인해야 함
- WorkManager 반복 거래는 Android 백그라운드 정책에 따른 예약 작업이라 정확히 정각 실행을 보장하지 않음

## 이번 작업 검증

- v0.1.14: UI·애니메이션 정리, 대화 알림 제외, 알림 후보 확인 큐/중복 저장 방어. 상세 범위와 한계는 [QA_UI_REPORT_2026-10-04.md](QA_UI_REPORT_2026-10-04.md) 참고
- v0.1.14: 단위 테스트 25개·lint·서명 Release 빌드 성공, Galaxy 기존 데이터 유지 설치와 설치 APK 해시 일치 확인
- v0.1.14 APK: 56,741,406 bytes; SHA-256 `2b0799e42ee06e1ad8b94f5a2d08102f5ea3b238bbbbed177c7ba4502d2e2ca3`
- v0.1.15: 이어서 점검한 달력 월 경계 날짜 필터 수정, 목표 지출 카드 전체 터치 적용. 단위 테스트 25개·lint·서명 Release 빌드 및 두 문제 실기기 재검증 성공
- v0.1.16: 카드 이용기간·고정비 레이더·반복 규칙 수정·내역 필터/정렬 추가, 민트 표면 구분과 라이트 선택 글자 대비 보완. 단위 테스트 46개·lint·서명 Release 빌드 성공. 실기기 등록/수정/일시 중지/재실행/정렬/알림 확인. 상세 범위는 [QA_FEATURES_REPORT_2026-10-04.md](QA_FEATURES_REPORT_2026-10-04.md)
- v0.1.17: 계좌·이체, 예산 이월/사용 속도, 전체 백업 복원, PDF·카메라와 로그인 코드 추가. 단위 76개와 별도 QA 앱의 Android 테스트 7개 통과, lint 오류 0, 서명 빌드 성공. 잠금 해제 후 홈 기존 기록/목표 유지와 신규 화면 확인. 최신 검증/배포 상태와 실제 서버 미연결 한계는 [QA_FEATURES_REPORT_2026-10-05.md](QA_FEATURES_REPORT_2026-10-05.md) 참고
- v0.1.18: 로그인 서버·PKCE 이메일 링크·암호화된 인증 요청·인증된 AI API 연결. 단위 84개, Android 9개(실제 로그인/서버 AI 포함), Worker 10개(실제 Workerd 포함) 통과. Cloudflare 리다이렉트 옵션의 런타임 오류 수정과 회귀 테스트 추가. 상세 결과와 메일 검증 한계는 [QA_AUTH_REPORT_2026-10-05.md](QA_AUTH_REPORT_2026-10-05.md)

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — 성공
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — 성공
- `./gradlew :app:assembleRelease` (VERSION_CODE=11, VERSION_NAME=0.1.11) — 성공 (`lintVitalRelease` 포함)
- `./gradlew :app:assembleRelease` (VERSION_CODE=12, VERSION_NAME=0.1.12) — 성공 (`lintVitalRelease` 포함)
- `./gradlew :app:assembleRelease` (VERSION_CODE=13, VERSION_NAME=0.1.13) — 성공 (`lintVitalRelease` 포함)
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (2026-10-04 장애 복구·오탐 방어 테스트 포함) — 성공
- APK 메타데이터 확인: `com.moasseum.app`, `versionCode=11`, `versionName=0.1.11`; 서명 인증서가 기존 릴리스 키와 일치
- APK 메타데이터 확인: `com.moasseum.app`, `versionCode=12`, `versionName=0.1.12`; 서비스 선언과 기존 릴리스 서명 인증서가 일치
- GitHub Release `v0.1.11` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.11
- GitHub Release `v0.1.12` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.12
- GitHub Release `v0.1.13` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.13
- GitHub Release `v0.1.14` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.14 (업로드된 asset digest가 설치 APK SHA-256과 일치)
- GitHub Release `v0.1.16` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.16 (latest 태그·크기·asset digest와 설치 APK 일치)
- GitHub Release `v0.1.17` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.17 (latest 태그·크기·digest와 재다운로드·설치 APK 일치; 로그인은 서버 연결 대기)
- `npm run typecheck` (`cloudflare/ai-worker`) — 성공; Worker 배포 버전 `170aa326-e7e8-400b-8d0b-5c77f066af6e`
- Worker `/health` 확인 — 성공; 잘못된 알림 분류·소비 Q&A 요청은 400 반환(모델 호출 없이)
- Worker `/v1/parse-transaction`, `/v1/analyze-spending`, `/v1/ask-spending`, `/v1/classify-notification` 실호출 — 모두 HTTP 200 확인 (2026-10-04)
- 연결 Android 기기 — `R3CT80B80MN` / Galaxy Z Fold4 / Android 16(API 36)에서 최종 릴리스 설치·기능·알림·재연결·백그라운드 알림 검증 완료
- 상세 실기기 오류 기록과 수정 결과 — [QA_TEST_REPORT_2026-10-04.md](QA_TEST_REPORT_2026-10-04.md)
- Debug APK — `app/build/outputs/apk/debug/app-debug.apk`
- 현재 설치 APK — v0.1.18, 56,905,458 bytes; SHA-256 `172d481f7915d9916a27c4d809d6618ce62ced3830c1c7722a3fed91e3a23521`. 기기에서 다시 가져온 설치 APK와 빌드 파일 일치. 공개 Release 최종 검증은 이번 로그인 QA 문서에 별도 기록

## 다음 작업/배포 안내

- Android 코드·UI·Room 스키마 변경: 테스트 후 `versionCode`를 올리고 서명 APK를 GitHub Release에 올림
- Worker 코드·프롬프트·API 응답 변경: `cd cloudflare/ai-worker`, `npm run typecheck`, `npm run deploy`; 이어 앱과 계약이 바뀌면 APK도 새로 릴리스
- Room Entity 추가/변경: 반드시 `FinanceDatabase` 버전을 올리고 이전 설치 데이터용 Migration 작성
- 함께 쓰기 활성화 전: 이미 연결한 Supabase에 원장 테이블/RLS 정책·두 계정 격리·오프라인 동기화/충돌 테스트가 추가로 필요. 로그인과 Worker 인증만으로 함께 쓰기가 완성되지 않음
- 개인 배포는 GitHub Release 기반. 앱이 새 APK를 직접 내려받고 크기·SHA-256(제공 시)·패키지·버전·서명을 확인한 뒤 설치 화면을 열며, 출처 허용과 설치 승인은 Android 보안상 사용자가 해야 함
