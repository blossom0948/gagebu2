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
- 거래 상세 확인과 soft delete 구현
- 다크 모드와 모션 줄이기 설정을 DataStore에 저장
- 서버/API 키 없이도 기본 가계부 기능 사용 가능
- AI 문장·영수증·공동 기능은 미구현 상태를 화면에 명확히 안내

## 검증 결과

- `./gradlew :app:assembleDebug` — 성공
- `./gradlew testDebugUnitTest lintDebug` — 성공
- APK — `app/build/outputs/apk/debug/app-debug.apk`
- 기기 설치 — 미실행: 현재 `adb devices`에 연결된 기기와 AVD가 없음

## 다음 단계

- Phase 2: 거래 수정/Undo, 실제 CSV 내보내기, 카테고리·결제수단 편집, 통계 테스트 확장
- Phase 3: Supabase 인증/RLS와 오프라인 outbox 동기화
- Phase 4: 서버를 통한 자연어 파싱, ML Kit OCR, 알림 후보함
- Phase 5 이후: AI 분석·공동 가계부·공통 Motion System 고도화
