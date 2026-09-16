# 모아씀 구현 상태

기준 문서: `WEEPLE_INSPIRED_BUDGET_APP_MASTER_PLAN.md`

## 구현된 개인용 기능

- Kotlin + Jetpack Compose, Room, DataStore 기반 Android 앱과 홈·내역·함께·관리 화면
- 월 목표 지출, 진행 막대, 수입·지출 합계, 카테고리 리포트
- 카테고리별 예산 한도와 홈 카테고리 사용률 표시
- 거래 추가, 상세, 수정, soft delete와 삭제 직후 실행 취소
- 카드 이름·결제일 관리와 결제수단 관리
- 날짜·유형·카테고리·가맹점·메모·결제수단을 CSV로 내보내고, 전체 거래를 JSON으로 백업
- CSV 중복 가져오기는 동일 거래 필드를 기준으로 건너뜀
- 7개 기본 카테고리 이름 변경과 사용자 카테고리 추가·수정·삭제(최대 20개); 삭제 시 기존 거래·반복 규칙·알림 후보를 기타로 이동
- 반복 지출/수입 규칙을 매월 지정일에 기록; 앱 미실행 중에도 WorkManager가 하루 한 번 처리하고 앱 시작 시 놓친 회차를 보충
- 한국어 자연어 입력을 거래 후보로 해석한 뒤 사용자가 검토하고 저장
- Android 시스템 음성 인식 앱을 호출하는 음성 입력
- 번들된 ML Kit 한국어 OCR로 사진 선택 후 기기 내 영수증 인식; 이미지는 Worker로 보내지 않음
- 사용자가 실행한 AI 월간 분석: 합계·예산·카테고리 합계만 Worker로 전송, 가맹점/메모/거래별 내역은 제외
- 사용자가 실행한 월간 소비 Q&A: 집계 수치만 Worker로 전송하고 답변을 후보 거래로 저장하지 않음
- Android 알림 접근 설정과 앱 알림 권한 안내, 금융 알림 후보를 앱 팝업/시스템 알림으로 확인 후 저장
- 제조사별 알림 텍스트 필드·메시지형 알림 추출, 서비스 연결/마지막 수신/후보 생성 진단과 재연결
- 알림 AI 분류는 선택 동의 기능: 기기에서 금액/금융 단서로 미리 거른 제목·내용만 Gemini에 보내며, 숫자만 있는 알림·광고·쿠폰·예정·취소는 제외하도록 분류
- 다크 모드·모션 줄이기, 앱 내 APK 직접 다운로드·파일 검증·Android 설치 화면 연결
- 관리 화면에서 거래·예산·알림 후보·반복 규칙을 확인 후 삭제 가능(설정은 보존)

## 외부 연결 상태와 제한

- Cloudflare Worker: `https://moasseum-ai-worker.blossom0948.workers.dev`
- Worker 경로: `/health`, `/v1/parse-transaction`, `/v1/analyze-spending`, `/v1/ask-spending`, `/v1/classify-notification`
- Gemini 키는 Worker Secret `GEMINI_API_KEY`로만 관리하며 APK에 포함하지 않음
- 현재 `REQUIRE_AUTH=false`인 개인 테스트 설정. 공개 다중 사용자 서비스로 사용하면 안 됨
- Supabase 인증/RLS/오프라인 동기화가 연결되지 않아 함께 쓰기·초대 코드 기능은 아직 실제 동작하지 않음
- 알림 읽기는 Android 특수 접근 권한이라 사용자가 시스템 화면에서 직접 허용해야 함. Android 13 이상에서는 모아씀 알림 표시 런타임 권한도 필요
- AI 알림 분류는 기본 꺼짐. 켜면 해당 알림 제목·내용이 외부 AI 서버로 전송되고 Gemini 사용량이 발생할 수 있음
- OCR은 사진 보관 기능이 아니며 사진 선택기로 가져온 이미지에서 후보를 만들 뿐. 인식 결과를 반드시 확인해야 함
- WorkManager 반복 거래는 Android 백그라운드 정책에 따른 예약 작업이라 정확히 정각 실행을 보장하지 않음

## 이번 작업 검증

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — 성공
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — 성공
- `./gradlew :app:assembleRelease` (VERSION_CODE=11, VERSION_NAME=0.1.11) — 성공 (`lintVitalRelease` 포함)
- APK 메타데이터 확인: `com.moasseum.app`, `versionCode=11`, `versionName=0.1.11`; 서명 인증서가 기존 릴리스 키와 일치
- GitHub Release `v0.1.11` 및 `app-release.apk` 업로드 예정
- `npm run typecheck` (`cloudflare/ai-worker`) — 성공; Worker 배포 버전 `170aa326-e7e8-400b-8d0b-5c77f066af6e`
- Worker `/health` 확인 — 성공; 잘못된 알림 분류·소비 Q&A 요청은 400 반환(모델 호출 없이)
- 실제 Gemini 생성 호출은 사용량을 발생시킬 수 있어 이 작업에서 실행하지 않음
- 연결 Android 기기 — `adb devices`에서 확인되지 않아 이 작업 중 설치/알림 실기기 검증은 미실행
- Debug APK — `app/build/outputs/apk/debug/app-debug.apk`
- 서명된 Release APK — `app/build/outputs/apk/release/app-release.apk` (56,741,358 bytes; SHA-256 `203239dc37b9fb069b7fef10d9e529521058a284f54103a48e62d95ff32cb3df`; 오프라인 한국어 OCR 모델 포함)

## 다음 작업/배포 안내

- Android 코드·UI·Room 스키마 변경: 테스트 후 `versionCode`를 올리고 서명 APK를 GitHub Release에 올림
- Worker 코드·프롬프트·API 응답 변경: `cd cloudflare/ai-worker`, `npm run typecheck`, `npm run deploy`; 이어 앱과 계약이 바뀌면 APK도 새로 릴리스
- Room Entity 추가/변경: 반드시 `FinanceDatabase` 버전을 올리고 이전 설치 데이터용 Migration 작성
- 함께 쓰기 활성화 전: Supabase 프로젝트 생성, 인증/RLS 정책과 격리 테스트, Worker `REQUIRE_AUTH=true`, 앱 로그인/동기화가 선행
- 개인 배포는 GitHub Release 기반. 앱이 새 APK를 직접 내려받고 크기·SHA-256(제공 시)·패키지·버전·서명을 확인한 뒤 설치 화면을 열며, 출처 허용과 설치 승인은 Android 보안상 사용자가 해야 함
