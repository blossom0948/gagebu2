package com.moasseum.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moasseum.app.R
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.LocalFinanceMotion

data class ReleaseNoteSlide(
    val imageResId: Int,
    val imageAspectRatio: Float,
    val title: String,
    val description: String,
    val imageDescription: String,
)

object ReleaseNotesCatalog {
    fun slides(versionName: String): List<ReleaseNoteSlide> = when (versionName) {
        "0.1.43" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_43_photo_review,
                imageAspectRatio = 904f / 1343f,
                title = "사진 거래, 취소 내역은 빼고 확인해요",
                description = "다른 사진에서도 날짜·금액·가맹점이 같은 취소 내역을 찾아 해당 거래만 제외해요. 완료된 취소 영수증도 지출 후보로 넣지 않아요. 잘못 제외된 항목은 원본 사진을 보고 후보로 되돌릴 수 있어요.",
                imageDescription = "Galaxy Z Fold4 격리 QA 앱에서 촬영한 사진 거래 검토 화면. 중복 2건과 취소 1건을 제외하고 가상 카페 거래 1건을 보여줍니다.",
            ),
        )
        "0.1.42" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_42_search_suggestions,
                imageAspectRatio = 904f / 555f,
                title = "관리 메뉴, 검색을 바로 시작해요",
                description = "처음 관리 화면을 열면 예산·카드값·알림 감지·백업 검색 칩이 보여요. 눌러도 곧바로 이동하지 않고 검색 결과와 설명을 먼저 확인한 뒤 실행해요. 카드값처럼 여러 메뉴에 걸칠 수 있는 표현도 실제 문장 테스트로 순위를 보정했어요.",
                imageDescription = "Galaxy Z Fold4 격리 QA 화면에서 캡처한 모아씀 관리 검색의 예산·카드값·알림 감지·백업 추천 칩.",
            ),
        )
        "0.1.41" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_41_recent_search,
                imageAspectRatio = 904f / 509f,
                title = "최근 찾은 메뉴, 한 번에 다시 열어요",
                description = "관리에서 실행한 메뉴 최대 5개를 검색 칩으로 다시 찾을 수 있어요. 칩을 눌러도 바로 이동하지 않고 기능 설명을 먼저 보여줘요. 최근 메뉴 기록에는 검색어 원문을 남기지 않고 기능 ID만 기기에 저장해요. ‘지우기’로 언제든 비울 수 있어요.",
                imageDescription = "Galaxy Z Fold4의 격리된 QA 앱에서 캡처한 실제 관리 검색창과 최근 메뉴 칩. 예산 메뉴 한 개만 예시로 표시했습니다.",
            ),
        )
        "0.1.40" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_40_search,
                imageAspectRatio = 904f / 1111f,
                title = "관리 메뉴, 말하듯 찾아요",
                description = "‘생활비 얼마나 남았어?’처럼 검색하면 기기 안의 키워드 결과와 기능 설명을 보여줘요. 결과를 눌러도 바로 이동하지 않고, 실제 동작을 확인한 뒤 버튼을 눌러야 해요. 무료 Gemini 의미 검색은 로그인 후 직접 눌렀을 때만 검색어와 메뉴 키워드를 보내요. 무료 입력은 Google 제품 개선에 쓰일 수 있으니 거래·계좌 정보는 검색어에 넣지 마세요.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 캡처한 실제 검색 UI. 검색어와 메뉴·AI 설명은 모두 테스트용 예시이며 실제 거래 내역은 없습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_40_profile,
                imageAspectRatio = 840f / 1034f,
                title = "홈 프로필을 내 이름과 사진으로",
                description = "홈의 프로필 사진을 누르면 표시 이름과 사진을 바꿀 수 있어요. 이름과 사진은 이 기기에만 저장되며 계정이나 파트너와 동기화되지 않습니다.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 캡처한 실제 프로필 편집 창. 민지라는 가상 이름만 사용했습니다.",
            ),
        )
        "0.1.39" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_39_currency,
                imageAspectRatio = 840f / 1139f,
                title = "화면 금액을 원하는 통화로",
                description = "미국·일본·유럽·대만·싱가포르 통화로 화면 금액을 환산해 볼 수 있어요. 거래와 예산은 원화로 그대로 저장하고, 환율이 없거나 7일이 지나면 원화로 표시해요. 앱 언어와 내보내기 파일은 기존처럼 한국어·원화 기준이에요.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 캡처한 실제 표시 통화 선택 창. 미국 달러가 선택되어 있고 환율 확인 전 원화 대체 안내가 보여요.",
            ),
        )
        "0.1.38" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_38_album,
                imageAspectRatio = 840f / 740f,
                title = "함께한 사진과 기념일 알림",
                description = "직접 고른 사진만 커플 앨범에 공유해요. 위치 정보(EXIF)는 제거하며 최대 30장·15 MB까지 저장할 수 있어요. 기념일 알림과 함께 보는 응원도 추가했어요.",
                imageDescription = "모아씀 함께 화면의 선택 사진 앨범과 공동 응원 카드. 화면 QA용 가상 항목이 표시된 실제 앱 캡처",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_38_tax,
                imageAspectRatio = 840f / 1647f,
                title = "연말정산 기준과 기록을 한눈에",
                description = "총급여 25% 기준과 연간 기록을 확인하고 CSV로 내보내요. 공제액이나 환급액을 계산하지는 않으며, 지출을 늘리도록 권하지 않아요.",
                imageDescription = "Fold4 QA 앱에서 캡처한 실제 앱 캡처: 연말정산 도우미 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_38_payday,
                imageAspectRatio = 840f / 900f,
                title = "급여일에 맞춘 예산 기간",
                description = "매월 예산 시작일을 정하면 목표·사용액·남은 금액·하루 사용 가능액이 그 주기로 계산돼요. 해당 날짜가 없는 달은 말일로 처리합니다.",
                imageDescription = "Fold4 QA 앱에서 캡처한 실제 앱 캡처: 예산 시작일 설정 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_38_ai_cards,
                imageAspectRatio = 840f / 1100f,
                title = "AI 조언은 필요한 화면만",
                description = "홈 요약·홈 리포트·함께 요약·함께 리포트를 각각 켜고 끌 수 있어요. 문장에 직접 적은 결제수단도 AI 거래 후보에 반영합니다.",
                imageDescription = "Fold4 QA 앱에서 캡처한 실제 앱 캡처: AI 조언 카드 설정 화면",
            ),
        )
        "0.1.37" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_37_calendar,
                imageAspectRatio = 904f / 1260f,
                title = "선택한 날짜를 한눈에",
                description = "내역 달력에서 선택한 날짜가 원의 가운데 보이도록 다듬었어요. 거래가 있는 날은 작은 점으로 확인할 수 있어요.",
                imageDescription = "선택한 12일 숫자가 민트색 원의 중앙에 표시된 소비내역 달력",
            ),
        )
        "0.1.36" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_28_photo_import,
                imageAspectRatio = 904f / 950f,
                title = "다른 앱에서 여러 장 바로 공유",
                description = "금융앱 캡처를 여러 장 선택해 모아씀으로 공유하면 한 번에 분석해요. 기존 내역과 겹치는 거래는 걸러내고, 새 거래만 확인한 뒤 기록할 수 있어요.",
                imageDescription = "여러 금융앱 캡처에서 중복 거래를 제외하고 새 거래를 검토하는 모아씀 화면",
            ),
        )
        "0.1.35" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_35_manage,
                imageAspectRatio = 904f / 1350f,
                title = "한 화면에서 찾는 관리 메뉴",
                description = "불필요한 메뉴 탭을 없애고 예산·장부, 결제·알림, 앱·데이터를 한 화면에 정리했어요. 목표 지출에는 이번 달 사용량과 진행 막대를 표시해요.",
                imageDescription = "목표 지출 진행 막대와 예산·장부 설정이 이어지는 모아씀 관리 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_35_suggestions,
                imageAspectRatio = 904f / 1150f,
                title = "가맹점을 입력하면 이전 내역 추천",
                description = "두 글자 이상 입력하면 같은 유형의 개인 거래를 최대 3개까지 찾아요. 하나를 고르면 금액·카테고리·결제수단을 불러와 빠르게 기록할 수 있어요.",
                imageDescription = "QA 테스트 장부에서 가맹점 입력에 따라 이전 개인 거래가 추천되는 상세 기록 화면",
            ),
        )
        "0.1.34" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_34_fx,
                imageAspectRatio = 904f / 1450f,
                title = "외화 거래에 거래일 환율 기록",
                description = "통화와 날짜를 고르면 무료 일일 참고 환율로 원화 금액을 계산하고, 원래 금액·환율·기준일도 함께 저장해요. 실제 카드 청구액과는 다를 수 있어요.",
                imageDescription = "거래일 참고 환율로 12.50달러를 원화로 환산해 기록하는 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_34_deposit,
                imageAspectRatio = 904f / 1450f,
                title = "외부 입금은 계좌 잔액에만",
                description = "수입·예산 합계에 넣지 않고 선택한 계좌 잔액에만 반영해요. 계좌를 보관하면 과거 거래 연결은 유지됩니다.",
                imageDescription = "계좌 잔액에만 반영되는 외부 입금 입력 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_34_installments,
                imageAspectRatio = 904f / 1450f,
                title = "할부 총액 수정과 일시불 전환",
                description = "총액을 바꾸면 회차별 금액을 다시 나누고 합계와 결제일을 보존해요. 할부 전환·삭제도 한곳에서 관리할 수 있어요.",
                imageDescription = "총액 수정, 일시불 전환, 할부 삭제를 제공하는 할부 관리 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_34_suggestions,
                imageAspectRatio = 904f / 1300f,
                title = "빠른 기록 추천 내역 정리",
                description = "추천 내역 편집에서 필요 없는 항목을 숨기면 다음 기록부터 추천에서 제외돼요.",
                imageDescription = "빠른 기록 추천 내역을 편집하고 개별 삭제하는 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_34_together,
                imageAspectRatio = 904f / 1450f,
                title = "함께 장부는 고른 거래만 공유",
                description = "두 계정이 공동 장부에 참여해도 개인 기록은 자동으로 올라가지 않아요. 공유할 거래를 직접 선택합니다.",
                imageDescription = "무료 공동 장부와 선택 공유를 안내하는 함께 화면",
            ),
        )
        "0.1.33" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_33_widget,
                imageAspectRatio = 2.3f,
                title = "홈 화면에서 바로 기록",
                description = "모아씀 위젯에서 지출·수입을 누르면 해당 입력 화면이 바로 열려요. 거래는 확인한 뒤 기기에 저장됩니다.",
                imageDescription = "모아씀 홈 화면 빠른 기록 위젯의 지출·수입 버튼 미리보기",
            ),
        )
        "0.1.32" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_32_together,
                imageAspectRatio = 904f / 1230f,
                title = "함께 쓰기도 무료로",
                description = "새 ‘매월 저축’ 방식으로 공동 목표 진행률을 함께 관리해요. 무료로 시작하고, 거래는 직접 고른 것만 공유해요.",
                imageDescription = "무료로 시작할 수 있고 공유할 거래를 직접 선택하는 모아씀 함께 화면",
            ),
        )
        "0.1.30" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_28_photo_import,
                imageAspectRatio = 904f / 950f,
                title = "사진 거래를 더 꼼꼼히 확인",
                description = "달력 합계·요일·취소 내역은 걸러내고, 겹친 거래는 한 건만 남겨요. 송금 상대나 결제처 분류가 애매한 항목은 확인 전 저장할 수 없어요. 사진 분석은 무료 ML Kit로 기기 안에서 처리해요.",
                imageDescription = "가맹점과 금액을 검토하고 확인한 거래만 추가하는 사진 가져오기 화면",
            ),
        )
        "0.1.29" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_29_fold_home,
                imageAspectRatio = 1812f / 2176f,
                title = "폴드 펼친 화면을 더 넓게",
                description = "펼친 화면에서는 대시보드 정보를 두 칸으로 배치해 월 목표 지출과 오늘·이번 주 지출을 한눈에 볼 수 있어요. 접은 화면은 기존 한 칸 구성을 유지해요.",
                imageDescription = "Galaxy Z Fold4를 펼친 상태의 모아씀 대시보드와 월 목표 지출 진행 막대",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_29_fold_history,
                imageAspectRatio = 1812f / 2176f,
                title = "달력과 내역을 함께 확인",
                description = "펼친 화면에서는 달력과 검색·필터·거래 목록을 나란히 보여줘요. 금융 사진 인식은 계속 기기 안에서 처리하고, 거래 문맥이 부족한 숫자는 후보에서 걸러요.",
                imageDescription = "Galaxy Z Fold4를 펼친 상태의 소비 달력과 거래 목록 분할 화면",
            ),
        )
        "0.1.28" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_28_photo_import,
                imageAspectRatio = 904f / 950f,
                title = "사진 여러 장에서 새 거래만 골라 기록",
                description = "캡처 속 거래를 모두 확인하고 가맹점·유형·금액·날짜·카테고리·결제 수단·메모를 고칠 수 있어요. 장부와 선택한 사진 사이의 중복은 제외하고, 확인이 필요한 결과는 검토 전 저장하지 않아요.",
                imageDescription = "여러 금융앱 캡처에서 장부·사진 간 중복 거래를 제외하고 신규 거래를 검토하는 화면",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_28_year_end,
                imageAspectRatio = 904f / 1100f,
                title = "한 해 소비를 흐름별로 정리",
                description = "연간 수입·지출과 월별 흐름, 카테고리·결제 수단별 합계를 확인해요. 이체는 합계에서 제외하며 세액·공제·환급액은 계산하지 않아요.",
                imageDescription = "연간 수입·지출, 월별 흐름, 카테고리 및 결제 수단 합계를 보여주는 자료 정리 화면",
            ),
        )
        "0.1.27" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_27_photo,
                imageAspectRatio = 904f / 1040f,
                title = "금융앱 캡처 여러 장에서 거래 추가",
                description = "토스 등 거래내역 화면을 한 번에 고르고, 장부·사진 간 중복은 제외해 새 내역만 검토 후 추가해요.",
                imageDescription = "모아씀 새 기록 메뉴의 사진에서 거래 가져오기 항목",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_27_help,
                imageAspectRatio = 904f / 1170f,
                title = "필요한 기능으로 바로 이동",
                description = "도움말에서 기록, 내역 수정, 예산·카드 관리, 파트너 공유 화면을 바로 열 수 있어요.",
                imageDescription = "거래 기록·내역 수정·관리·파트너 공유로 연결되는 빠른 도움말",
            ),
        )
        "0.1.26" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_installments,
                imageAspectRatio = 904f / 1100f,
                title = "날짜 지정하고 할부로 기록",
                description = "직접 입력에서 거래일을 고르고, 지출을 2~60개월로 나눠 등록할 수 있어요.",
                imageDescription = "거래 기록 화면의 할부 개월과 거래일 선택",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_report,
                imageAspectRatio = 904f / 1000f,
                title = "6개월·요일별 소비 흐름",
                description = "월별 지출 변화와 저축률, 카테고리·요일별 지출을 한 화면에서 확인해요.",
                imageDescription = "월별 지출 추이와 요일별 지출 리포트",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_notifications,
                imageAspectRatio = 904f / 780f,
                title = "알림을 종류별로 모아보기",
                description = "예산·챌린지 소식과 결제 후보를 나눠 보고, 소식은 읽음 처리할 수 있어요.",
                imageDescription = "전체, 예산, 결제, 챌린지 알림 필터",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_26_profile,
                imageAspectRatio = 904f / 700f,
                title = "내 이름으로 홈 화면 꾸미기",
                description = "홈 프로필 이름을 바꿔 보세요. 표시 이름은 이 기기에만 저장돼요.",
                imageDescription = "기기 안에 저장하는 프로필 이름 편집 창",
            ),
        )
        "0.1.25" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_25_navigation,
                imageAspectRatio = 904f / 704f,
                title = "하단 메뉴를 다시 누르면 맨 위로",
                description = "대시보드·소비내역·함께·관리에서 선택된 메뉴를 한 번 더 누르면 화면 맨 위로 이동해요.",
                imageDescription = "대시보드 최근 내역과 선택된 하단 메뉴",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_25_sync,
                imageAspectRatio = 904f / 704f,
                title = "연결되면 함께 장부를 새로고침",
                description = "공유 장부를 보는 중 인터넷 연결이 복구되면 최신 내용을 자동으로 다시 불러와요.",
                imageDescription = "함께 장부 기능 화면",
            ),
        )
        "0.1.24" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_24_ai,
                imageAspectRatio = 904f / 688f,
                title = "최근 4개월 소비 흐름",
                description = "월별 지출과 카테고리 합계를 AI가 비교해 변화와 반복 패턴을 짚어줘요. 거래처와 메모는 전송하지 않아요.",
                imageDescription = "모아씀 AI 분석 화면의 최근 4개월 지출 흐름",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_24_alerts,
                imageAspectRatio = 904f / 790f,
                title = "알림 후보를 더 정확하고 간편하게",
                description = "금액이 언급된 일반 알림은 걸러내고, 승인·입금·출금이 확인되는 거래 위주로 후보를 만들어요. 마트 상호는 식비로 분류하고 후보를 한 번에 정리할 수도 있어요.",
                imageDescription = "모아씀 알림 후보함의 카테고리와 전체 정리 동작",
            ),
        )
        else -> emptyList()
    }
}

@Composable
fun WhatsNewDialog(
    versionName: String,
    slides: List<ReleaseNoteSlide> = remember(versionName) { ReleaseNotesCatalog.slides(versionName) },
    onDismiss: () -> Unit,
) {
    if (slides.isEmpty()) return
    val colors = LocalFinanceColors.current
    val motion = LocalFinanceMotion.current
    var selectedPage by rememberSaveable(versionName) { mutableIntStateOf(0) }
    val safePage = selectedPage.coerceIn(slides.indices)
    val slide = slides[safePage]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).padding(horizontal = 22.dp).heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceRaised,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("v$versionName 신기능", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                AnimatedContent(
                    targetState = safePage,
                    transitionSpec = {
                        if (motion.reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { it / 4 * direction } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 4 * direction } + fadeOut())
                        }
                    },
                    label = "release-note-slide",
                ) { page ->
                    val pageSlide = slides[page]
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Image(
                            painter = painterResource(pageSlide.imageResId),
                            contentDescription = pageSlide.imageDescription,
                            modifier = Modifier.fillMaxWidth().aspectRatio(pageSlide.imageAspectRatio)
                                .clip(RoundedCornerShape(18.dp)).background(colors.surfaceBase),
                            contentScale = ContentScale.Fit,
                        )
                        Text(pageSlide.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(pageSlide.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("${safePage + 1} / ${slides.size}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    if (safePage > 0) {
                        TextButton(onClick = { selectedPage = safePage - 1 }) { Text("이전") }
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = if (safePage == slides.lastIndex) onDismiss else ({ selectedPage = safePage + 1 }),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (safePage == slides.lastIndex) "확인" else "다음", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
