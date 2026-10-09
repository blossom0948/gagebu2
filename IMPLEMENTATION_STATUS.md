# 모아씀 구현 상태

기준 문서: `WEEPLE_INSPIRED_BUDGET_APP_MASTER_PLAN.md`

## 2026-10-10 최신 공개판 — v0.1.32

- 함께 쓰기 시작 화면에 무료 이용과 선택 공유를 명확히 표시하고, 버튼을 `무료로 시작`으로 정리했습니다.
- 함께 쓰는 방식에 `매월 저축`을 추가했습니다. 선택 월의 공동 목표액·현재 모은 금액을 합산해 진행 막대로 보여주며, 은행 자동이체나 실제 송금은 하지 않습니다.
- Galaxy Z Fold4 화면을 캡처해 v0.1.32 신기능 팝업에 넣었습니다.
- 운영 Supabase에 `202610080002`–`202610100005` migration을 승인받아 적용했습니다. 공동 목표 진행액·공동 재정·설정/연결 해제·저축 방식의 DB 구조와 RLS를 추가했고, 개인 거래 데이터는 업로드·삭제하지 않았습니다. `MONTHLY_SAVINGS` 제약조건을 읽기 전용 조회로 확인했습니다.
- 검증: Android 단위 테스트 192개, Fold4 계측 테스트 14개 통과/원격 실계정 로그인 1개 skip, lint 오류 0(경고 24·정보 7), Debug 및 서명 Release 빌드 성공.
- APK: versionCode `32`, versionName `0.1.32`, 60,739,126 bytes, SHA-256 `b4edc4def33579acf4312722887d54810480ef4dfd4233fbb5a5148d8e3baf30`. 서명 인증서 지문은 기존 공개판과 동일합니다.
- [v0.1.32 릴리스 노트](docs/releases/v0.1.32.md), [GitHub Release](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.32)
- 아직 계정 삭제, 실제 두 계정의 초대·동기화/RLS 격리 E2E, 모든 상세 화면과 애니메이션의 전수 실기기 검증은 완료되지 않았습니다. Weeple 전체와 완전히 동일하다고 보장하지 않습니다.

## 이전 공개판 — v0.1.31

- 카카오 로그인을 모아씀 전용 Supabase에 연결했습니다. 닉네임·프로필 사진은 선택 동의이며, 이메일 없이 로그인할 수 있습니다. 현재 개인 개발자 앱은 카카오 이메일 권한이 없어 이메일을 요청하지 않습니다.
- 사진 거래 가져오기에 후보별 근거 이미지 보기와 선택된 항목의 화면 범위 캡처를 추가했습니다. 확인이 필요한 행도 선택·수정할 수 있고, 중복 제외와 다중 이미지 처리는 유지합니다.
- 앱 내 APK 업데이트에 원형 다운로드 진행률과 설치 안내를 추가했습니다. Android 설치 승인 자체는 사용자가 해야 합니다.
- 검증: Android 단위 테스트 190개 통과, lint 및 Debug/Release 빌드 성공. Fold4 계측 14개 중 원격 실계정 인증 1개는 계정 생성을 피하기 위해 건너뛰었습니다. QA 앱에서 Kakao 버튼 활성화를 확인했습니다.
- [v0.1.31 릴리스 노트](docs/releases/v0.1.31.md), [GitHub Release](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.31)
- Weeple 전체 기능·화면·모션과 완전히 동일하다는 뜻은 아닙니다. 계정 삭제와 모든 상세 화면·모션의 실기기 전수 검증은 별도 상태로 관리합니다.

## 과거 공개판 — v0.1.30

- 사진 가져오기에 다중 캡처 후보 수정(가맹점·유형·금액·날짜·카테고리·결제수단·메모), 중복 제거, 미확인 OCR 후보 저장 방지, 사진을 더 선택할 때 수정값 보존을 추가했습니다.
- 연말 자료 정리에 월별 수입·지출, 카테고리·결제수단별 합계와 거래 건수를 추가했습니다. 이체는 합계에서 제외하며 세액/환급 계산은 하지 않습니다.
- 상점 카테고리 추론과 하단 선택 아이콘 반응을 다듬었습니다. 폰트 크기와 Moasseum 테마는 유지했습니다.
- [v0.1.30 릴리스 노트](docs/releases/v0.1.30.md), [GitHub Release](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.30)
- Weeple과 완전히 같지 않습니다. 계정 삭제와 모든 상세 화면·모션의 실기기 전수 검증은 남아 있으며, 공유 목표·공동 재정·장부 나가기용 Supabase migration 002–004의 운영 적용 여부는 별도로 확인해야 합니다.

## 2026-10-09 추가 구현 — 아직 운영 배포 전

> 이 절은 v0.1.23 릴리스 이전의 작업 기록입니다. 최신 배포 상태는 아래 `v0.1.23` 절을 기준으로 확인하세요.

- `+ → 문장으로 여러 건 처리`의 AI 경로를 추가했습니다. AI 서버에는 입력 문장만 보내며, 가계부 행은 보내지 않습니다. AI는 거래 추가 후보나 검색/수정 계획만 반환하고, 수정·삭제 대상은 앱이 기기의 개인 거래에서 찾아 사용자가 행을 선택해야 반영됩니다. 공유 거래·할부·이체는 제외하고 최대 100건으로 제한합니다.
- AI가 만든 추가 후보를 항목별로 저장 전에 수정할 수 있도록 금액·유형·가맹점·분류·날짜·메모 편집을 추가했습니다. 삭제는 선택 후 별도 확인이 필요합니다.
- AI Worker의 `/v1/parse-batch-command` 경로·검증·인증/rate-limit 테스트를 추가했습니다. 삭제 응답 스키마의 필수 편집 객체 불일치도 빈 객체 계약으로 수정하고, 예상치 못한 수정값·무제한 검색 조건을 거부합니다. 이 Worker는 아래 기록처럼 운영 배포했습니다.
- 공동 장부 기념일 카드에 다음 기념일까지 남은 일수를 표시합니다. 2월 29일과 윤년도 처리합니다.
- 공동 거래/목표/재정 항목 조회에 안정 정렬 페이지네이션과 명시적 최대 행 제한을 추가해 서버 결과가 조용히 잘리는 일을 막습니다.
- 검증: Android 단위 테스트 142개 통과, lint 오류 0(기존 의존성·SDK 경고), Debug 및 AndroidTest APK 빌드 성공. Android 16 에뮬레이터 계측 12개 중 11개 통과, 실제 Supabase 로그인 1개는 자격 증명 테스트를 하지 않아 skip. Worker typecheck와 17개 테스트 통과. 연결 Galaxy는 잠금 화면이라 조작하지 않았습니다.
- Worker는 기존 인증·분당 제한을 유지한 채 version `d7b47aef-30f4-49a3-99c1-4efa63543277`로 배포했습니다. `/health` 정상과 새 일괄 경로의 무인증 401을 운영 주소에서 확인했습니다. 실계정 토큰으로 Gemini 요청을 보내지는 않았습니다.
- Android 변경은 아직 공개 릴리스가 아닙니다. 서명 Release APK/GitHub Release와 실제 두 계정 공유 검증은 하지 않았습니다. 전용 Supabase의 `202610080002_shared_goal_collaboration.sql` 및 `202610080003_shared_finance_items.sql`도 여전히 미적용입니다.

## 과거 배포 — v0.1.23

- `+ → 문장으로 여러 건 처리`에 AI 문장 해석을 연결했습니다. 서버에는 입력 문장만 전송하며 거래 행은 보내지 않습니다. 추가 후보는 수정 후 저장하고, 수정·삭제는 기기 내 개인 거래에서 찾은 대상을 사용자가 선택해야 반영됩니다.
- 기념일 D-Day와 공동 장부 조회 페이지네이션을 추가했습니다.
- Android 단위 142개 통과, Galaxy Android 16 계측 11개 통과/원격 로그인 1개 건너뜀, lint 오류 0(경고 24), Debug/AndroidTest/Release 빌드 성공. Worker typecheck 및 17개 테스트 통과.
- Galaxy Z Fold4의 QA 앱에서 첫 실행 권한 안내와 홈을 확인했고, 계측 테스트 11개가 통과했습니다. 이어 서명 Release APK를 설치해 프로덕션 앱을 v0.1.18에서 v0.1.23으로 업데이트했습니다. 패키지의 `firstInstallTime`이 유지되고 앱이 정상 시작되는 것을 확인했습니다. 금융 거래 내용은 열람·출력하지 않았습니다.
- APK: `com.moasseum.app`, versionCode 23, versionName 0.1.23, 59,543,541 bytes, SHA-256 `1615615b748111af10beabf18632ae3d9f28621e5cb2901c8d2bb3f9fae6244b`. 서명 인증서는 기존 공개판과 동일합니다.
- 이번 Android 공개판은 [GitHub Release v0.1.23](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.23) 및 [릴리스 노트](docs/releases/v0.1.23.md)를 따릅니다. 증분 Supabase migration `202610080002`·`202610080003`은 적용하지 않았으므로 공동 목표 진행액/공동 재정 서버 저장은 아직 준비되지 않았습니다.

## Weeple과 비교해 아직 별도 작업이 필요한 항목

- 카카오 로그인은 v0.1.31에서, 공동 자금·반반 참고 정산·각자 관리·수입 비교·매월 저축 선택은 v0.1.32에서 구현했습니다. 저축 진행은 공동 목표의 직접 입력값을 사용합니다.
- 계정 삭제 UI/API는 아직 없습니다. 실제 파트너 두 계정으로 초대/수락·RLS 격리·동시 편집과 모든 상세 화면·애니메이션을 전수 검증하지 않았습니다.
- 따라서 Weeple 전체와 완전히 같다고 보장하지 않습니다. 이번 실기기 비교는 주요 화면 흐름 및 함께 화면을 중심으로 했습니다.

## 과거 배포 — v0.1.22 (2026-10-09)

- 첫 실행 4단계 안내를 추가했습니다. 무료 가계부·목표 지출·빠른 입력·결제 알림·AI 기능과 개인정보 전송 범위를 안내하며, 홈 도움말 또는 `관리 → 기능 안내`에서 다시 열 수 있습니다.
- 시스템 상태 표시줄을 계속 보이게 하고, 홈·알림 안내·입력 시트가 상단/하단 시스템 영역과 겹치지 않는지 Galaxy Z Fold4(Android 16)에서 확인했습니다. 알림 설정 진입과 설정 후 복귀, 안내 종료 뒤 권한 현황 안내도 확인했습니다. OS 알림 접근 권한 자체는 테스트 중 변경하지 않았습니다.
- `:app:testDebugUnitTest` 137개 통과, `:app:lintDebug` 오류 0, Debug/AndroidTest/Release APK 빌드 성공. 실기기에서는 QA 패키지(`com.moasseum.app.qa`)를 사용했습니다.
- 공개 APK: `com.moasseum.app`, versionCode 22, versionName 0.1.22, 59,527,161 bytes, SHA-256 `8cd403c3b8ba37dbe2776a493726171043b4bea5fc9333da3113b9300093d5df`. 서명 지문은 기존 공개 v0.1.21과 같은 `669674a2114fb59d85c56bba774b89fec9ce7f7ee79825769826130362a091c9`입니다.
- 앱 UI/안내만 바뀌어 Worker·Supabase·Room 스키마는 변경하지 않았습니다. 공동 목표 진행액/공동 재정 저장 migration 상태는 이전 버전과 같습니다.

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
- 할부 2~60회 등록과 월별 회차 지출, 원 단위 잔액 분배·월말 날짜 보정, 할부 묶음 삭제. JSON/CSV 회차 정보 보존, Room 5→6 마이그레이션
- 최근 3개월 연속·유사 금액 지출을 고정비 후보로 제안하고, 사용자가 확인한 경우에만 반복 규칙으로 등록
- 매월 예산 50%·초과와 반복 지출 전날 알림, 중복 발송 방지, 알림 설정에서 전체 켜기/끄기
- 생체 인증/기기 잠금 앱 잠금 옵션
- 연도별 수입·지출/카테고리 요약과 상세 거래 CSV 내보내기(세액·환급 계산은 제공하지 않음)
- Android 공유 메뉴에서 텍스트를 AI 입력 화면에, 영수증 이미지를 기기 내 OCR 확인 화면에 전달. 공유만으로 자동 저장하지 않음
- 문장 일괄 처리: 로컬 미리보기 후 다중 거래 추가, 조건 검색 후 선택 수정/삭제(삭제 취소 가능). 임의 문장 AI 명령이 아니라 정해진 한국어 문장 패턴으로 처리하며, 할부·공유·이체 거래는 제외
- 선택 월의 카테고리 합계와 내역을 한국어 다중 페이지 PDF로 내보내기
- CSV 중복 가져오기는 동일 거래 필드를 기준으로 건너뜀
- 19개 기본 카테고리(식비·카페·교통·쇼핑·주거·통신·의료·교육 등)와 사용자 카테고리 추가·수정·삭제(최대 20개); 삭제 시 기존 거래·반복 규칙·알림 후보를 기타로 이동
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
- v0.1.19: Google 공식 버튼·시스템 브라우저 OAuth·PKCE S256·암호화된 요청 복원·취소/만료/재사용 방어 추가. 모아씀 전용 Google OAuth 클라이언트를 Supabase Google 공급자에 연결하고 소유자 계정 하나만 Testing 허용 목록에 등록. 이메일 확인/nonce 검사는 유지하며 유료 전환하지 않음. 실제 Google 계정 로그인·동의·휴대폰 복귀는 미검증
- 기본 무료 메일의 소유자/조직 이메일 제한과 시간당 2통을 화면에 설명. 가입·복구 링크는 PKCE S256/기기 내 암호화 검증값/요청 식별값으로 검증하며, 앱 재실행·재전송·만료·위조·다른 계정·재사용을 검사
- 실제 서버 로그인·세션 갱신·앱 재시작 복구·로그아웃과 인증된 AI 호출을 합성 계정으로 검증. 실제 소유자 이메일의 가입/복구 메일 수신·링크 완료와 두 계정 RLS 격리 테스트는 미실행. 가짜 Auth API 단위 테스트를 메일 실수신 성공으로 간주하지 않음
- v0.1.21 공개판에는 선택 거래 공유·초대·월별 합산·공동 저축 목표·공동 재정 항목·AI 분석/Q&A·PDF 코드가 포함. Supabase `202610080002` 목표 진행액 migration과 `202610080003` 공동 재정 migration은 미적용 상태로 APK를 먼저 공개했으므로, 이 두 서버 저장 기능은 migration 적용 전까지 사용할 수 없거나 오류가 날 수 있음. 두 계정 격리/오프라인 충돌·재시도/실기기 UI 및 기존 Room DB 이전 검증은 미완료

## 외부 연결 상태와 제한

- Cloudflare Worker: `https://moasseum-ai-worker.blossom0948.workers.dev` (2026-10-08 deployed version `7f223d5d-9a24-4bf0-8a68-ecf0bfb0b4c2`)
- Worker 경로: `/health`, `/v1/parse-transaction`, `/v1/analyze-spending`, `/v1/ask-spending`, `/v1/classify-notification`
- Gemini 키는 Worker Secret `GEMINI_API_KEY`로만 관리하며 APK에 포함하지 않음
- v0.1.18 Worker `REQUIRE_AUTH=true`: 같은 전용 Supabase에서 로그인 토큰 확인, 무인증/위조 401, 인증 장애 503, 인증 사용자별 호출 제한. 실제 네 경로의 인증된 200 응답 검증
- Supabase 인증 연결. shared_* 기본 테이블·RLS는 적용됐으나 공동 목표/재정 증분 migration은 미적용. 두 기능의 서버 동작과 두 계정 격리·기기 간 동기화는 검증되지 않음
- 알림 읽기는 Android 특수 접근 권한이라 사용자가 시스템 화면에서 직접 허용해야 함. Android 13 이상에서는 모아씀 알림 표시 런타임 권한도 필요
- AI 알림 분류는 기본 꺼짐. 켜면 해당 알림 제목·내용이 외부 AI 서버로 전송되고 Gemini 사용량이 발생할 수 있음
- OCR은 사진 보관 기능이 아니며 선택/촬영 이미지에서 후보를 만들 뿐. 촬영 임시 파일은 처리/취소 후 제거. 인식 결과를 반드시 확인해야 함
- WorkManager 반복 거래는 Android 백그라운드 정책에 따른 예약 작업이라 정확히 정각 실행을 보장하지 않음

## 이번 작업 검증

- 2026-10-08 공동 장부 첫 구현: `shared_*` 기본 테이블 5개와 RLS는 전용 Supabase에 적용. 증분 migration 002/003은 미적용. DB 사용자 데이터 업로드/초대 생성은 하지 않음
- 2026-10-08 v0.1.21 Release: 단위 테스트·lint·AndroidTest APK 빌드·서명 Release 빌드 성공. lint 오류 0/경고 24개. 에뮬레이터 instrumentation 12개 통과, 실제 Supabase 계정 테스트 1개는 자격 증명 미제공으로 skip. 작은/큰 화면 UI를 확인했으나 실제 Galaxy에서의 설치·로그인·알림 및 Room 5→6 이전은 미검증
- 문장 일괄 추가·수정·삭제는 로컬 제한형 파서로 추가. 임의 자유형 AI 명령, 일괄 추가 후보별 세부 편집, 일괄 기능 실기기 조작은 미검증
- 공동 재정 모드와 5종 항목의 앱/UI/API 코드는 v0.1.21로 공개했지만, 새 Supabase migration 적용 전에는 목표 진행액·공동 재정 서버 저장이 준비되지 않음. 두 계정 동기화/격리는 미검증
- Weeple 화면 기능 대조 작업(2026-10-08): 홈 월 이동, 빠른 카메라/사진 입력, 최근 내역, PDF 리포트 진입을 추가. 관리 화면의 순서를 Weeple 캡처 흐름으로 재배치하고 카테고리 사용 막대·미설정 목록 접기/펼치기·저장되는 카테고리 순서·월별 수입 목표를 추가. 캡처에 보이는 기본 19개 카테고리를 거래 입력·내역 필터·예산에 추가하고, 이전 7개 설정/백업은 기존 이름과 순서를 보존해 확장. 자연어·영수증·결제 알림·Worker 카테고리 키도 동기화. 화면 제목을 줄이고 상단 상태 표시줄만 숨겨 캡처의 화면 밀도에 가깝게 조정. 전체 시스템 바를 숨기는 안은 Android 전체 화면 경고와 하단 탭 잘림 때문에 제외. 홈도 밀도를 낮추고 오늘 소비 인사이트를 반영했으며, 거래가 없을 때 빈 최근 내역 카드를 숨겨 초기 화면 스크롤을 줄임. 내역은 CSV 가져오기 위치·달력 확장·카테고리 칩·한 줄 합계/정렬을 캡처에 맞춤. 함께 연결 전 미리보기와 초대/참여 화면을 보강. 함께 화면에 공유 월 합산·카테고리·공동 저축 목표 진행/수정/삭제·공동 AI 분석/Q&A·공동 PDF를 연결. `202610080002_shared_goal_collaboration.sql`로 current_amount와 공동 멤버 편집 정책 준비. DB 적용·두 계정/실기기 사용성 검증·전체 Weeple 잔여 기능 대조는 미완료. 자세한 범위는 [Weeple 기능 대조 진행 보고서](WEEPLE_PARITY_PROGRESS_2026-10-08.md)
- 검증: unit/lint/서명 Release APK 빌드 성공, Worker typecheck 및 테스트 10개 통과. v0.1.21의 최신 연결 instrumentation은 12개 통과/1개 skip. 인증 없는 Worker 요청은 401, `/health`는 200 반환. 단위 테스트 최종 수와 전체 lint 경고 목록은 Gradle 보고서를 참고. 로그인된 초대/수락·실제 휴대폰 동작·새 migration 적용은 미검증
- Google 가입 범위 확인: `node scripts/check-google-oauth.mjs`에서 실제 서버 Google/email 활성화, 이메일 확인 유지, 앱/QA PKCE redirect와 `openid email profile`만 요청 확인. 실제 두 번째 Google 계정 로그인/앱 복귀는 미검증. Google 기본 신원 scope 예외에 따라 별도 테스트 사용자 등록이 필수가 아닐 수 있음
- v0.1.20 (2026-10-08): 관리·입력·알림·로그인 화면의 반복 설명 정리 및 사용하지 않는 문구 리소스 제거. 오늘/이번 주 집계와 월 경계, 실제 저장 완료 전에 성공 처리하던 폼, 저장 예외 누락, 오래된 월의 AI 응답 경합, 거래 시간대 손실, 잘못된 금액 붙여넣기, 카드/카테고리 설정 정합성, 반복 규칙 수정의 거짓 성공, 알림 진단 저장 실패가 감지를 막는 경로를 수정. 단위 114개·lint 오류 0·Debug/AndroidTest/Release APK 빌드 성공. lint 경고 21개는 기존 의존성 업데이트 안내 등이며 새 오류는 아님. Android instrumentation 10개는 컴파일만 확인했고 휴대폰 미연결로 실행/설치/Google 로그인/알림 실기기 동작은 미검증. 상세: [2026-10-08 앱 QA 보고서](QA_APP_AUDIT_REPORT_2026-10-08.md)
- v0.1.21 APK: `com.moasseum.app`, versionCode 21, versionName 0.1.21, 59,510,773 bytes; SHA-256 `f69638449384a0ae4f6d5d7cf72b44f6fbd345b220a323fa1115ce90b307c59f`. 서명 인증서 SHA-256 `669674a2114fb59d85c56bba774b89fec9ce7f7ee79825769826130362a091c9` (v0.1.20과 동일). 공개 Release digest/다운로드는 게시 후 확인.

- v0.1.14: UI·애니메이션 정리, 대화 알림 제외, 알림 후보 확인 큐/중복 저장 방어. 상세 범위와 한계는 [QA_UI_REPORT_2026-10-04.md](QA_UI_REPORT_2026-10-04.md) 참고
- v0.1.14: 단위 테스트 25개·lint·서명 Release 빌드 성공, Galaxy 기존 데이터 유지 설치와 설치 APK 해시 일치 확인
- v0.1.14 APK: 56,741,406 bytes; SHA-256 `2b0799e42ee06e1ad8b94f5a2d08102f5ea3b238bbbbed177c7ba4502d2e2ca3`
- v0.1.15: 이어서 점검한 달력 월 경계 날짜 필터 수정, 목표 지출 카드 전체 터치 적용. 단위 테스트 25개·lint·서명 Release 빌드 및 두 문제 실기기 재검증 성공
- v0.1.16: 카드 이용기간·고정비 레이더·반복 규칙 수정·내역 필터/정렬 추가, 민트 표면 구분과 라이트 선택 글자 대비 보완. 단위 테스트 46개·lint·서명 Release 빌드 성공. 실기기 등록/수정/일시 중지/재실행/정렬/알림 확인. 상세 범위는 [QA_FEATURES_REPORT_2026-10-04.md](QA_FEATURES_REPORT_2026-10-04.md)
- v0.1.17: 계좌·이체, 예산 이월/사용 속도, 전체 백업 복원, PDF·카메라와 로그인 코드 추가. 단위 76개와 별도 QA 앱의 Android 테스트 7개 통과, lint 오류 0, 서명 빌드 성공. 잠금 해제 후 홈 기존 기록/목표 유지와 신규 화면 확인. 최신 검증/배포 상태와 실제 서버 미연결 한계는 [QA_FEATURES_REPORT_2026-10-05.md](QA_FEATURES_REPORT_2026-10-05.md) 참고
- v0.1.18: 로그인 서버·PKCE 이메일 링크·암호화된 인증 요청·인증된 AI API 연결. 단위 84개, Android 9개(실제 로그인/서버 AI 포함), Worker 10개(실제 Workerd 포함) 통과. Cloudflare 리다이렉트 옵션의 런타임 오류 수정과 회귀 테스트 추가. 상세 결과와 메일 검증 한계는 [QA_AUTH_REPORT_2026-10-05.md](QA_AUTH_REPORT_2026-10-05.md)
- v0.1.19: 구글 로그인 구현/서버 설정, 단위 95개·lint 오류 0(기존 경고 22개)·서명 Release/Android 테스트 APK 빌드 성공. 실제 Supabase 설정과 본앱/QA 인증 시작의 Google 리다이렉트·클라이언트·반환 주소·기본 권한 범위 확인. 휴대폰 미연결로 Android 테스트 실행/설치/실제 Google 로그인은 미실행. [QA_GOOGLE_AUTH_REPORT_2026-10-07.md](QA_GOOGLE_AUTH_REPORT_2026-10-07.md)

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
- 현재 공개 APK — [v0.1.22](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.22), 59,527,161 bytes; SHA-256 `8cd403c3b8ba37dbe2776a493726171043b4bea5fc9333da3113b9300093d5df`. 기존 공개 인증서 지문과 일치. Galaxy Z Fold4(Android 16)에서 QA 빌드의 안내·설정 이동·홈·입력 시트와 시스템 바 여백을 수동 확인.
- 이전 공개 APK — [v0.1.21](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.21), 59,510,773 bytes; SHA-256 `f69638449384a0ae4f6d5d7cf72b44f6fbd345b220a323fa1115ce90b307c59f`. GitHub asset digest/공개 재다운로드 기록은 해당 릴리스 검증 시점의 상태를 참고.
- 이전 공개 APK — [v0.1.19](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.19), 59,131,229 bytes; SHA-256 `d9dee61e2c9a954f916d9afa94b40267032d485262e380d419008909530d416c`. 당시 무인증 latest·asset digest·공개 재다운로드와 로컬 서명 빌드 일치. 휴대폰 설치/실제 Google 로그인은 미실행.
- 마지막 휴대폰 설치 검증 APK — [v0.1.18](https://github.com/blossom0948/gagebu2/releases/tag/v0.1.18), 56,905,458 bytes; SHA-256 `172d481f7915d9916a27c4d809d6618ce62ced3830c1c7722a3fed91e3a23521`. 2026-10-05 무인증 latest·asset digest·공개 재다운로드·설치 APK 일치. v0.1.19의 설치 성공으로 간주하지 않음

## 다음 작업/배포 안내

- Android 코드·UI·Room 스키마 변경: 테스트 후 `versionCode`를 올리고 서명 APK를 GitHub Release에 올림
- Worker 코드·프롬프트·API 응답 변경: `cd cloudflare/ai-worker`, `npm run typecheck`, `npm run deploy`; 이어 앱과 계약이 바뀌면 APK도 새로 릴리스
- Room Entity 추가/변경: 반드시 `FinanceDatabase` 버전을 올리고 이전 설치 데이터용 Migration 작성
- 공동 장부 후속: migrations `202610080002`–`202610100005`는 운영 Supabase에 적용되어 있다. 다시 실행하지 않는다. 실제 두 계정의 초대/수락·RLS 격리, 동시 수정, 오프라인 재시도와 기존 Room 데이터 보존 검증은 남아 있다.
- 개인 배포는 GitHub Release 기반. 앱이 새 APK를 직접 내려받고 크기·SHA-256(제공 시)·패키지·버전·서명을 확인한 뒤 설치 화면을 열며, 출처 허용과 설치 승인은 Android 보안상 사용자가 해야 함
