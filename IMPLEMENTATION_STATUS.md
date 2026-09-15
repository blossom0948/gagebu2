# 모아씀 구현 상태

기준 문서: `WEEPLE_INSPIRED_BUDGET_APP_MASTER_PLAN.md`

## Phase 1 — 첫 실행 가능한 앱

상태: 완료

- 실제 Kotlin + Jetpack Compose Android Studio 프로젝트 생성
- 독자 브랜드 `모아씀`과 다크/라이트 의미 색 토큰 구성
- 홈·내역·함께·관리 하단 탭과 중앙 추가 FAB 연결
- Room `transactions`/`budgets` Entity, DAO, Database, Repository 구성
- 직접 입력으로 지출·수입 거래 저장
- 홈과 내역에서 같은 Room 거래 데이터 조회
- 월별 합계·수입·카테고리·예산 진행률 계산
- 한 달 목표 지출 편집과 현재 사용률 진행 바
- 거래 상세 확인과 soft delete 구현
- 다크 모드와 모션 줄이기 설정을 DataStore에 저장
- 자연어 거래 후보 생성(로컬 한국어 파서 + 선택적 HTTPS AI Worker 연동)
- AI 후보 확인 후 저장 흐름과 서버 JSON schema 검증
- Android NotificationListenerService, 결제 패턴 파서, Room 알림 후보함
- 서버/API 키 없이도 기본 가계부와 로컬 자연어 파서 사용 가능
- 영수증·공동 기능은 서버/권한 설정 전까지 화면에 명확히 안내

## 이번 확장 — Phase 4 기반

상태: 구현 완료(외부 secret·권한 설정 전 검증 대기)

- `cloudflare/ai-worker`: Gemini 구조화 출력용 `/v1/parse-transaction`, `/health`
- Worker는 `GEMINI_API_KEY`를 secret으로만 읽고 Supabase Bearer token 검증을 기본 요구
- Android Manifest에 HTTPS 인터넷 권한과 NotificationListenerService 등록
- 관리 화면에서 알림 접근 설정을 열고 보류 후보를 저장/무시
- 첫 실행 알림 안내 팝업과 Galaxy 알림 접근 설정 상세 화면 바로가기
- 결제 알림은 중복 fingerprint를 사용하며 사용자 확인 없이 원장에 넣지 않음

## 검증 결과

- `./gradlew :app:assembleDebug` — 성공
- `./gradlew testDebugUnitTest lintDebug` — 성공
- `npm run typecheck` (`cloudflare/ai-worker`) — 성공
- APK — `app/build/outputs/apk/debug/app-debug.apk`
- 기기 설치 — 미실행: 현재 `adb devices`에 연결된 기기와 AVD가 없음

## 다음 단계

- Phase 2: 거래 수정/Undo, 실제 CSV 내보내기, 카테고리·결제수단 편집, 통계 테스트 확장
- Phase 3: Supabase 인증/RLS와 오프라인 outbox 동기화
- Phase 4 후속: ML Kit OCR, 음성 입력, AI 분석 탭, 공동 가계부 서버 연결
- Phase 5 이후: 공통 Motion System과 공유 데이터 모델 고도화
