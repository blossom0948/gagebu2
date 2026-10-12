package com.moasseum.app.ui.screens

/** Searchable Manage destinations. Kept separate from Compose callbacks so the real catalog can be tested. */
data class ManageSearchDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val aliases: List<String>,
    val actionLabel: String = "설정 열기",
)

data class ManageSearchShortcut(
    val id: String,
    val label: String,
    val query: String,
)

object ManageSearchCatalog {
    val shortcuts = listOf(
        ManageSearchShortcut("budget", "예산", "이번 달 예산"),
        ManageSearchShortcut("card-usage", "카드값", "이번 달 카드값"),
        ManageSearchShortcut("notification-access", "알림 감지", "금융 알림 감지"),
        ManageSearchShortcut("backup", "백업", "백업 파일 저장"),
    )

    val entries = listOf(
        ManageSearchDescriptor("budget", "월 예산 설정", "목표와 이번 예산 기간의 사용액", listOf("예산", "생활비", "남은 생활비", "생활비 잔액", "지출 한도", "이번 달 얼마", "얼마 남았어", "소비 목표", "예산 얼마나 썼어", "이번 달 한도", "예산에서 돈 얼마나 남았어", "이번달 예산 얼마나 남았어", "생활비 얼마 남았어"), "예산 수정"),
        ManageSearchDescriptor("income", "월 수입 목표", "월급·수입 목표 금액", listOf("수입", "월급", "월급 목표", "급여", "소득", "매달 버는 돈", "수입 목표"), "목표 수정"),
        ManageSearchDescriptor("payday", "예산 시작일", "월 예산을 시작하는 날짜", listOf("예산 시작", "예산 계산 시작일", "급여일", "월급날", "월급날 기준", "월급 들어오는 날", "월급 들어오는 날 기준으로 예산 다시 맞추기", "급여일에 맞춰 예산 기간 바꾸기", "월초", "매달 며칠부터", "예산 기간"), "시작일 변경"),
        ManageSearchDescriptor("rollover", "남은 예산 다음 달 이월", "사용하지 않은 예산을 다음 달에 반영", listOf("이월", "미사용 예산", "남은 돈", "예산 남으면", "다음 달로", "잔액 넘기기", "남은 돈 다음달로 이월", "안 쓰고 남은 금액을 다음 달 예산에 포함", "안 쓴 돈 다음 달 예산에 반영", "이번 달 남은 예산 다음 달로", "잔여 예산 다음달 반영"), "이월 설정 변경"),
        ManageSearchDescriptor("no-spend", "무지출 챌린지", "정한 기간 동안 지출 없는 날을 기록", listOf("절약", "돈 안 쓰기", "지출 줄이기", "소비 줄이기", "무지출", "챌린지"), "챌린지 설정"),
        ManageSearchDescriptor("categories", "카테고리 관리", "항목 이름과 사용 여부", listOf("분류", "식비", "카테고리 이름", "항목 이름 바꾸기", "지출 항목", "소비 종류"), "카테고리 관리"),
        ManageSearchDescriptor("category-budgets", "카테고리별 예산", "식비·교통비 등 항목마다 한도 설정", listOf("항목별 한도", "식비 한도", "카테고리 한도", "분류별 예산", "카테고리 예산"), "한도 설정"),
        ManageSearchDescriptor("category-order", "카테고리 순서", "자주 쓰는 항목을 앞으로", listOf("분류 순서", "항목 정렬", "카테고리 정렬", "목록 순서"), "순서 변경"),
        ManageSearchDescriptor("accounts", "계좌·지갑과 이체", "잔액·계좌 간 이동 관리", listOf("계좌", "계좌 잔액", "지갑", "통장", "송금", "이체", "잔액", "은행 계좌 추가"), "계좌 열기"),
        ManageSearchDescriptor("payment-methods", "결제수단", "현금·카드·이체 이름 관리", listOf("결제 방식", "지불 수단", "현금", "카드 종류", "결제 방법", "결제수단 이름"), "결제수단 관리"),
        ManageSearchDescriptor("installments", "할부 관리", "할부 내역과 남은 회차", listOf("할부", "할부 잔액", "무이자", "몇 개월 남았어", "나눠 내기", "분할 결제"), "할부 내역 보기"),
        ManageSearchDescriptor("card-usage", "결제일 기준 보기", "카드 결제일에 맞춘 사용 금액", listOf("카드값", "카드 대금", "이번 달 카드값", "청구 금액", "결제 예정", "이번 카드값", "결제일별 사용", "카드 이용 기간", "카드값 이번달", "카드값 이번달 얼마나 써", "이번달 카드값 얼마나 썼어", "이번 달 카드값 얼마", "이번달 카드 결제 얼마"), "카드 사용액 보기"),
        ManageSearchDescriptor("cards", "카드·결제일", "카드와 결제일 등록", listOf("신용카드", "체크카드", "카드 등록", "결제 날짜", "청구일", "카드 결제일"), "카드 관리"),
        ManageSearchDescriptor("recurring", "반복 거래 관리", "매달 자동으로 기록할 거래", listOf("정기 거래", "정기 지출", "반복 지출", "매달 나가는 돈", "정기적으로 나가는 비용", "자동으로 빠져나가는 비용", "월세", "자동 기록"), "반복 거래 관리"),
        ManageSearchDescriptor("fixed-costs", "구독·고정비 레이더", "정기 결제와 월 예정액 확인", listOf("구독", "구독료", "고정비", "월 고정비", "정기 결제", "자동 결제", "매달 빠져나가는 돈", "월 지출", "한 달마다 나가는 구독 어디서 봐", "자동결제 찾기"), "예정 내역 보기"),
        ManageSearchDescriptor("reminders", "예산·고정비·기념일 알림", "예산·고정 결제·기념일 알림", listOf("알림", "예산 알림", "결제 예정 알림", "기념일", "리마인더", "고정비 알림", "예산 절반 쓰면 알려줘", "정기결제 미리 알려줘", "결제 전날 알림"), "알림 설정 변경"),
        ManageSearchDescriptor("notification-access", "결제 알림 감지", "금융 앱 알림을 거래 후보로 인식", listOf("알림 읽기", "알림 읽는 권한", "알림 접근", "알림 허용", "카드 알림", "금융 알림", "거래 알림", "결제 감지", "입금 감지", "출금 감지", "자동 기록", "문자 알림", "거래 인식", "금융앱 입출금 알림 감지", "은행 입금 알림 감지", "금융 알림 자동 기록"), "권한 설정 열기"),
        ManageSearchDescriptor("app-notifications", "인식 알림 표시", "감지된 거래를 앱 알림으로 안내", listOf("푸시 알림", "앱 알림", "추가할까요", "알림 표시", "알림 권한", "모아씀 알림"), "알림 권한 설정"),
        ManageSearchDescriptor("ai-notifications", "AI 알림 오탐 줄이기", "숫자만 있는 알림을 걸러 금융 거래인지 판별", listOf("AI 알림", "오탐", "잘못 감지", "카톡 숫자", "쓸모없는 알림", "금융인지 판단", "알림 ai 분류", "카톡 숫자 알림 거르기", "오픈채팅 숫자 감지", "카톡 숫자 감지 끄기", "사람들 숫자 적은 건 무시"), "AI 판별 설정"),
        ManageSearchDescriptor("candidate-inbox", "알림 후보함", "감지된 거래 검토·추가·삭제", listOf("감지 내역", "인식된 거래", "추가할까요", "알림 기록", "결제 후보", "입금 후보", "감지된 알림 한 번에 지우기", "알림 후보 모두 삭제", "후보 일괄 삭제", "인식 내역 지우기", "알림 후보 한번에 전부 삭제"), "후보함 열기"),
        ManageSearchDescriptor("backup", "전체 백업 내보내기", "기기 거래와 설정을 파일로 보관", listOf("백업", "기록 저장", "자료 내보내기", "파일로 저장", "데이터 보관"), "백업 내보내기"),
        ManageSearchDescriptor("restore", "백업 복원", "저장한 백업 파일 불러오기", listOf("복원", "백업 가져오기", "파일 불러오기", "기록 가져오기", "데이터 복구"), "백업 파일 선택"),
        ManageSearchDescriptor("safety-backup", "복원 전 백업 내보내기", "복원하기 전에 현재 기록 보관", listOf("복원 전 백업", "안전 백업", "기존 기록 저장"), "안전 백업 내보내기"),
        ManageSearchDescriptor("year-end", "연말정산 도우미", "연간 카드 사용 기록과 CSV", listOf("연말 정산", "소득공제", "카드 공제", "25퍼센트", "연간 소비", "연말정산 csv"), "연말정산 보기"),
        ManageSearchDescriptor("pdf", "월별 PDF 리포트", "선택한 달의 지출 보고서", listOf("보고서", "가계부 pdf", "월간 통계 pdf", "리포트 저장"), "PDF 저장"),
        ManageSearchDescriptor("year-csv", "연간 거래 CSV", "한 해의 거래를 스프레드시트로 내보내기", listOf("연간 csv", "엑셀", "거래 내보내기", "전체 기록 csv", "엑셀로 내보내기"), "CSV 내보내기"),
        ManageSearchDescriptor("account", "로그인·계정", "로그인 상태와 계정 관리", listOf("로그인", "가입", "구글", "카카오", "로그아웃", "계정 연결", "연동 해제", "계정 삭제", "회원 탈퇴"), "계정 설정 열기"),
        ManageSearchDescriptor("guide", "기능 안내", "앱 기능과 권한 설정 도움말", listOf("도움말", "사용법", "앱 사용법", "앱을 어떻게 쓰는지 알려줘", "사용 방법 알려줘", "어떻게 써", "알림 설정 방법", "처음 설정", "가이드"), "도움말 열기"),
        ManageSearchDescriptor("home-cards", "홈 카드 구성", "대시보드에 보일 카드 선택", listOf("홈 화면", "대시보드", "첫 화면", "요약 카드", "홈 구성"), "홈 카드 선택"),
        ManageSearchDescriptor("ai-cards", "AI 조언 카드", "홈에 표시할 AI 분석 카드 선택", listOf("AI 분석", "소비 분석", "AI 요약", "인사이트 카드"), "AI 카드 선택"),
        ManageSearchDescriptor("currency", "표시 통화", "참고 환율로 화면만 환산 · 거래와 예산은 원화로 저장", listOf("환율", "달러", "엔화", "외화", "원화 말고", "원화로 보기", "통화 변경", "유로"), "통화 선택"),
        ManageSearchDescriptor("theme", "다크 모드", "앱의 밝고 어두운 화면 전환", listOf("화면 어둡게", "야간 화면", "밝은 테마", "테마", "색상 모드"), "테마 변경"),
        ManageSearchDescriptor("motion", "모션 줄이기", "화면 전환과 움직임 효과 설정", listOf("애니메이션 끄기", "움직임", "전환 효과", "모션 감소"), "모션 설정 변경"),
        ManageSearchDescriptor("app-lock", "앱 잠금", "생체 인증 또는 기기 잠금으로 앱 보호", listOf("잠금", "지문", "생체 인증", "비밀번호", "앱 보안"), "앱 잠금 변경"),
        ManageSearchDescriptor("privacy", "개인정보와 데이터", "내 정보·거래 기록 관리", listOf("개인 정보", "데이터 삭제", "기록 삭제", "탈퇴", "내 정보 지우기"), "개인정보 설정 열기"),
        ManageSearchDescriptor("update", "앱 업데이트 확인", "새 버전을 확인하고 앱에서 설치", listOf("업데이트", "새 버전", "최신 버전", "앱 갱신", "버전 확인", "앱 업데이트"), "업데이트 확인"),
    )
}
