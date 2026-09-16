# 모아씀 구현 상태

기준 문서: `WEEPLE_INSPIRED_BUDGET_APP_MASTER_PLAN.md`

## 구현된 개인용 기능

- Kotlin + Jetpack Compose, Room, DataStore 기반 Android 앱과 홈·내역·함께·관리 화면
- 월 목표 지출, 진행 막대, 수입·지출 합계, 카테고리 리포트
- 거래 추가, 상세, 수정, soft delete와 삭제 직후 실행 취소
- 날짜·유형·카테고리·가맹점·메모·결제수단을 CSV로 내보내고 가져오기
- CSV 중복 가져오기는 동일 거래 필드를 기준으로 건너뜀
- 7개 기본 카테고리의 이름 변경, 결제수단 추가·삭제(최대 12개)
- 반복 지출/수입 규칙을 매월 지정일에 기록; 앱을 열 때 놓친 회차를 최대 120개월 단위로 보충
- 한국어 자연어 입력을 거래 후보로 해석한 뒤 사용자가 검토하고 저장
- Android 시스템 음성 인식 앱을 호출하는 음성 입력
- 번들된 ML Kit 한국어 OCR로 사진 선택 후 기기 내 영수증 인식; 이미지는 Worker로 보내지 않음
- 사용자가 실행한 AI 월간 분석: 합계·예산·카테고리 합계만 Worker로 전송, 가맹점/메모/거래별 내역은 제외
- 첫 실행 알림 권한 안내, Android 알림 접근 설정, 결제 알림 후보 확인 후 수동 저장
- 다크 모드·모션 줄이기, 앱 내 GitHub 릴리스 확인과 Android 수동 업데이트 흐름
- 관리 화면에서 거래·예산·알림 후보·반복 규칙을 확인 후 삭제 가능(설정은 보존)

## 외부 연결 상태와 제한

- Cloudflare Worker: `https://moasseum-ai-worker.blossom0948.workers.dev`
- Worker 경로: `/health`, `/v1/parse-transaction`, `/v1/analyze-spending`
- Gemini 키는 Worker Secret `GEMINI_API_KEY`로만 관리하며 APK에 포함하지 않음
- 현재 `REQUIRE_AUTH=false`인 개인 테스트 설정. 공개 다중 사용자 서비스로 사용하면 안 됨
- Supabase 인증/RLS/오프라인 동기화가 연결되지 않아 함께 쓰기·초대 코드 기능은 아직 실제 동작하지 않음
- 알림 읽기는 Android 특수 접근 권한이라 첫 팝업 뒤에도 사용자가 시스템 화면에서 직접 허용해야 함
- OCR은 사진 보관 기능이 아니며 사진 선택기로 가져온 이미지에서 후보를 만들 뿐. 인식 결과를 반드시 확인해야 함
- 반복 거래는 백그라운드 정각 실행이 아니라 앱 시작 시 밀린 회차를 처리함
- 카테고리는 기본 7개 이름만 바꿀 수 있고, 임의 카테고리 생성/삭제는 아직 없음

## 이번 작업 검증

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — 성공
- `./gradlew -PVERSION_CODE=7 -PVERSION_NAME=0.1.7 :app:assembleRelease` — 성공 (`lintVitalRelease` 포함)
- 0.1.7 APK는 기존 0.1.6과 같은 서명 키로 빌드해 앱 데이터 유지 업데이트 가능
- APK 버전 확인: `com.moasseum.app`, `versionCode=7`, `versionName=0.1.7`
- GitHub Release `v0.1.7` 및 `app-release.apk` 업로드 완료: https://github.com/blossom0948/gagebu2/releases/tag/v0.1.7
- `npm run typecheck` (`cloudflare/ai-worker`) — 성공
- Wrangler dry-run — 성공
- Cloudflare Worker 배포 — 완료
- `/health` 확인 — 성공
- AI 분석 입력 검증 smoke test — 잘못된 요청에 400 반환 확인(모델 호출 없이)
- 실제 Gemini 생성 호출은 사용량을 발생시킬 수 있어 이 작업에서 실행하지 않음
- 연결 Android 기기 — `adb devices`에서 확인되지 않아 이 작업 중 설치 검증은 미실행
- Debug APK — `app/build/outputs/apk/debug/app-debug.apk`
- 서명된 Release APK — `app/build/outputs/apk/release/app-release.apk` (약 54 MB; 오프라인 한국어 OCR 모델 포함)

## 다음 작업/배포 안내

- Android 코드·UI·Room 스키마 변경: 테스트 후 `versionCode`를 올리고 서명 APK를 GitHub Release에 올림
- Worker 코드·프롬프트·API 응답 변경: `cd cloudflare/ai-worker`, `npm run typecheck`, `npm run deploy`; 이어 앱과 계약이 바뀌면 APK도 새로 릴리스
- Room Entity 추가/변경: 반드시 `FinanceDatabase` 버전을 올리고 이전 설치 데이터용 Migration 작성
- 함께 쓰기 활성화 전: Supabase 프로젝트 생성, 인증/RLS 정책과 격리 테스트, Worker `REQUIRE_AUTH=true`, 앱 로그인/동기화가 선행
- 개인 배포는 GitHub Release 수동 업데이트 방식. 앱에서 업데이트를 확인·다운로드할 수 있지만 설치 화면에서 승인하는 동작은 Android 보안상 사용자 확인이 필요
