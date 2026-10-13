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
import androidx.compose.foundation.layout.Box
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
    val imageResId: Int? = null,
    val imageAspectRatio: Float = 1.55f,
    val title: String,
    val description: String,
    val imageDescription: String = "",
    val releaseVersion: String? = null,
)

object ReleaseNotesCatalog {
    private val supportedVersions = listOf("0.1.1", "0.1.2") + (4..51).map { "0.1.$it" }

    /** Returns only releases newer than [lastSeenVersion], in chronological order. */
    fun slidesSince(lastSeenVersion: String?, currentVersion: String): List<ReleaseNoteSlide> {
        require(currentVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        if (lastSeenVersion != null) require(lastSeenVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        return supportedVersions
            .asSequence()
            .filter { com.moasseum.app.update.ReleaseNotesPolicy.compareVersions(it, currentVersion) <= 0 }
            .filter { version ->
                lastSeenVersion == null ||
                    com.moasseum.app.update.ReleaseNotesPolicy.compareVersions(version, lastSeenVersion) > 0
            }
            .flatMap { version -> slides(version).asSequence().map { it.copy(releaseVersion = version) } }
            .toList()
    }

    fun slides(versionName: String): List<ReleaseNoteSlide> = when (versionName) {
        "0.1.1" -> listOf(
            ReleaseNoteSlide(
                title = "모아씀 첫 개인용 앱",
                description = "Galaxy에 APK로 설치해 기존 기록을 유지하며 쓸 수 있게 배포했어요. 관리 화면에서 이후 앱 업데이트도 확인할 수 있어요.",
            ),
        )
        "0.1.2" -> listOf(
            ReleaseNoteSlide(
                title = "설치 보호와 업데이트 안내",
                description = "개인용 APK의 설치 호환성을 다듬었어요. 업데이트 확인 후에는 GitHub에서 APK를 받고 Android 설치 화면에서 직접 확인하는 방식이었어요.",
            ),
        )
        "0.1.4" -> listOf(
            ReleaseNoteSlide(
                title = "업데이트 버전 확인 안정화",
                description = "설치 보호 호환성을 보완하고 새 버전 비교를 안정화했어요. APK 설치는 계속 Android 시스템이 처리해요.",
            ),
        )
        "0.1.5" -> listOf(
            ReleaseNoteSlide(
                title = "기존 앱과 데이터 호환 유지",
                description = "Galaxy에 설치된 개인용 앱과 같은 서명을 유지해 기록을 지우지 않고 업데이트할 수 있도록 정리했어요.",
            ),
        )
        "0.1.6" -> listOf(
            ReleaseNoteSlide(
                title = "작은 화면에 맞춰 화면을 간결하게",
                description = "홈 요약·목표 지출 막대·기록 입력·내역·함께·관리 화면의 간격과 배열을 더 컴팩트하게 다듬었어요.",
            ),
        )
        "0.1.7" -> listOf(
            ReleaseNoteSlide(
                title = "거래 편집·CSV·음성 기록",
                description = "거래 수정과 삭제 취소, CSV 가져오기/내보내기, 월 반복 거래와 이름 관리, 음성 입력, 기기 안 한국어 영수증 인식, 월간 AI 소비 분석을 추가했어요.",
            ),
        )
        "0.1.8" -> listOf(
            ReleaseNoteSlide(
                title = "알림 후보와 카테고리 직접 관리",
                description = "숫자만 있는 알림과 광고·취소 등을 걸러내고, 금융 거래로 보이는 알림만 후보로 안내해요. AI 오탐 분류는 선택 기능이며, 카테고리 추가·수정·삭제와 반복 거래 보충도 지원해요.",
            ),
        )
        "0.1.9" -> listOf(
            ReleaseNoteSlide(
                title = "처음 실행할 때 알림 권한 안내",
                description = "알림 접근·모아씀 알림 권한과 AI 오탐 줄이기 설정을 단계별로 확인해요. 광고·인증번호·예정·취소 알림은 제외하고 거래는 확인 뒤 추가해요.",
            ),
        )
        "0.1.10" -> listOf(
            ReleaseNoteSlide(
                title = "알림 권한 상태를 더 정확히 확인",
                description = "Android 알림 권한까지 실제 상태를 확인하고 필요한 설정을 안내해요. AI 오탐 줄이기가 꺼져 있을 때 켜는 방법도 알려줘요.",
            ),
        )
        "0.1.11" -> listOf(
            ReleaseNoteSlide(
                title = "카드·카테고리 예산과 소비 Q&A",
                description = "알림 본문 판별과 AI 장애 복구를 다듬고 카드·결제일 관리, 카테고리별 예산, 월간 소비 질문, 전체 JSON 백업을 추가했어요.",
            ),
        )
        "0.1.12" -> listOf(
            ReleaseNoteSlide(
                title = "Galaxy 알림 감지 재연결 보강",
                description = "알림 접근 권한을 켠 뒤 서비스 연결을 다시 시도하고, 시스템이 연결을 끊으면 복구를 재시도해요. 설정 화면을 열 수 없는 기기용 대체 경로도 추가했어요.",
            ),
        )
        "0.1.13" -> listOf(
            ReleaseNoteSlide(
                title = "AI 서버가 응답하지 않아도 알림 감지 유지",
                description = "AI 서버 오류 복구와 알림 서비스 생명주기를 보완했어요. 숫자만 있는 메신저·운세 알림이 후보로 오인되지 않도록 방어를 추가했어요.",
            ),
        )
        "0.1.14" -> listOf(
            ReleaseNoteSlide(
                title = "대화 알림은 제외하고 거래만 확인",
                description = "카카오톡 대화와 송금 메시지는 금융 후보에서 제외하고 별도 금융앱 알림은 유지해요. 감지 직후 추가 확인창과 알림 후보함을 제공하고, 입력창·팝업·애니메이션도 정리했어요.",
            ),
        )
        "0.1.15" -> listOf(
            ReleaseNoteSlide(
                title = "예산과 거래 입력 흐름 다듬기",
                description = "월 경계에서 날짜 필터가 초기화되던 문제를 고치고 목표 지출 카드를 눌러 바로 수정할 수 있게 했어요. 거래 날짜 수정·키보드·상세 화면도 보완했어요.",
            ),
        )
        "0.1.16" -> listOf(
            ReleaseNoteSlide(
                title = "카드 결제주기와 고정비 관리",
                description = "카드별 결제월·이용기간·수단 집계, 구독·고정비 예정액과 30일 일정, 반복 거래 편집, 내역 필터·정렬을 추가했어요. 테마의 창 구분은 선명하게, 글꼴 크기는 그대로 유지했어요.",
            ),
        )
        "0.1.49" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_49_home_dashboard,
                imageAspectRatio = 904f / 1458f,
                title = "예산 기간과 달력 월을 명확하게",
                description = "홈·인사이트·리포트에서 급여일 기준 예산 주기와 달력 월 기록을 구분해 보여줘요. 지난 예산 기간은 실제 지출로, 현재 기간만 예상 지출로 표시하고 초과 금액도 바로 확인할 수 있어요. 업데이트 안내도 마지막으로 확인한 버전 이후의 변경을 버전별로 모아 보여줘요.",
                imageDescription = "Fold4 QA 앱에서 합성 데이터 없이 촬영한 홈 화면. 급여일 예산 기간과 달력 월 카테고리 기준이 따로 표시됩니다.",
            ),
        )
        "0.1.17" -> listOf(
            ReleaseNoteSlide(
                title = "계좌·예산·백업을 한곳에",
                description = "계좌 잔액과 계좌 간 이체, 예산 이월·하루 사용액·월말 예상, 전체 JSON 백업/복원을 추가했어요. 월간 PDF와 카메라 영수증 기록도 지원해요. 이체는 은행 송금이 아닌 장부 기록입니다.",
            ),
        )
        "0.1.18" -> listOf(
            ReleaseNoteSlide(
                title = "이메일 로그인과 계정 복구",
                description = "모아씀 전용 서버로 이메일 가입·로그인과 비밀번호 재설정을 연결했어요. 인증 메일 링크로 앱에 안전하게 돌아오는 흐름도 추가했어요.",
            ),
        )
        "0.1.19" -> listOf(
            ReleaseNoteSlide(
                title = "Google 계정으로 로그인",
                description = "Google 계정을 선택해 로그인하고 앱으로 돌아올 수 있어요. Gmail·Drive 접근은 요청하지 않아요.",
            ),
        )
        "0.1.20" -> listOf(
            ReleaseNoteSlide(
                title = "기록 날짜와 저장 흐름을 더 안전하게",
                description = "기기 시간대가 바뀌어도 거래 날짜를 보존하고, 저장이 끝난 뒤에만 완료로 처리해요. 오늘·이번 주 집계와 금액 입력 검증도 다듬었어요.",
            ),
        )
        "0.1.21" -> listOf(
            ReleaseNoteSlide(
                title = "카테고리·할부·함께 장부 확장",
                description = "기본 카테고리와 월 예산, 할부·반복 지출, 앱 잠금, 연간 자료를 보강했어요. 함께 장부에는 선택 거래 공유와 공동 목표·재정 및 AI 요약을 추가했어요.",
            ),
        )
        "0.1.22" -> listOf(
            ReleaseNoteSlide(
                title = "처음 쓰는 분을 위한 무료 사용 안내",
                description = "첫 실행에서 목표 지출·빠른 기록·결제 알림·AI 기능을 안내해요. 알림 권한 상태와 시스템 화면 여백도 확인하기 쉽게 정리했어요.",
            ),
        )
        "0.1.23" -> listOf(
            ReleaseNoteSlide(
                title = "한 문장으로 여러 거래 관리",
                description = "여러 거래 추가나 내 거래 수정·삭제 대상을 문장으로 찾을 수 있어요. 추가·수정·삭제는 내용을 확인하고 직접 승인해야 반영돼요.",
            ),
        )
        "0.1.48" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_48_photo_picker,
                imageAspectRatio = 840f / 858f,
                title = "커플 대표 사진을 직접 골라요",
                description = "함께 앨범에 이미 공유한 사진 중 한 장을 두 사람의 상단 카드에 지정할 수 있어요. 사진을 새로 올리거나 공유하지 않으며, 최근 공유 사진으로 다시 바꿀 수도 있어요.",
                imageDescription = "Galaxy Z Fold4 격리 QA 앱에서 합성 사진 두 장으로 촬영한 대표 사진 선택 창. 표시된 제목과 날짜는 테스트용이며 개인 사진은 없습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_48_holiday_calendar,
                imageAspectRatio = 830f / 527f,
                title = "달력에서 주말·공휴일을 구분",
                description = "토요일은 파란색, 일요일과 공휴일은 코랄색으로 표시해요. 선택한 공휴일 이름도 달력 아래에서 바로 확인할 수 있어요.",
                imageDescription = "Galaxy Z Fold4 QA에서 합성 거래 기록 없이 촬영한 2026년 10월 달력. 10월 9일 한글날 이름과 주말 색 구분이 보입니다.",
            ),
        )
        "0.1.47" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_47_relationship,
                imageAspectRatio = 830f / 467f,
                title = "함께한 날과 최근 사진을 한눈에",
                description = "관계 시작일을 함께 설정하면 두 사람 화면에 D+가 표시돼요. 시작일은 언제든 바꾸거나 지울 수 있고, 커플 앨범에 직접 공유한 최신 사진은 상단 카드에 보여요. 사진을 누르면 크게 확인할 수 있어요.",
                imageDescription = "Galaxy Z Fold4 격리 QA 앱에서 만든 시작일·D+128 화면 캡처. 배경 그림과 날짜는 모두 합성 예시이며 실제 계정이나 사진은 사용하지 않았습니다.",
            ),
        )
        "0.1.46" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_46_budget_report,
                imageAspectRatio = 830f / 684f,
                title = "리포트에서 예산 페이스 바로 확인",
                description = "리포트에서 달을 바꾸고, 이번 달 목표 대비 사용률과 남은 금액을 한눈에 봐요. 예산을 넘기면 초과 금액을 표시하고, 아직 예산이 없으면 같은 화면에서 설정할 수 있어요.",
                imageDescription = "Galaxy Z Fold4 격리 QA 앱에서 합성 10월 지출 12만 원·9월 28일 지출 3만 원, 월급날 25일과 예산 30만 원으로 촬영한 리포트. 예산 주기 50% 사용과 15만 원 잔액이 보이며 실제 거래 기록은 없습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_46_manage_search,
                imageAspectRatio = 904f / 555f,
                title = "비슷한 메뉴도 말뜻에 맞게 찾아요",
                description = "‘월급 들어오는 날 기준으로 예산 맞추기’, ‘남은 돈 다음 달로 넘기기’처럼 말해도 급여일·예산 이월을 구분해요. 대표 자연어 질의 35개를 회귀 검사했고, 결과는 설명을 먼저 본 뒤 직접 실행하도록 유지했어요.",
                imageDescription = "모아씀 격리 QA 앱의 관리 검색 추천 화면. 예산·카드값·알림 감지·백업 예시만 있으며 개인 기록은 포함하지 않습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_46_together_preview,
                imageAspectRatio = 840f / 1039f,
                title = "함께 장부 미리보기를 더 알차게",
                description = "공동 목표 진행률과 공동 수입·생활비·고정비 예시를 한 번에 볼 수 있어요. 화면의 금액은 모두 합성 예시이고, 실제 거래는 자동 공유되지 않아요. 무료로 쓰고 공유할 항목은 직접 고릅니다.",
                imageDescription = "모아씀 격리 QA 앱의 함께 기능 미리보기에서 촬영한 합성 목표와 공동 재정 요약. 실제 계정이나 거래는 사용하지 않았습니다.",
            ),
        )
        "0.1.45" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_45_budget_presets,
                imageAspectRatio = 840f / 918f,
                title = "예산 금액을 빠르게 선택",
                description = "월 목표 지출을 수정할 때 10만·30만·50만·100만·300만 원을 바로 고를 수 있어요. 직접 입력도 계속 사용할 수 있어요.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 합성 장부로 촬영한 목표 지출 수정 창. 30만 원 프리셋이 선택되어 있고 실제 거래 데이터는 없습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_45_manage_group,
                imageAspectRatio = 904f / 250f,
                title = "관리 메뉴를 간결하게 정돈",
                description = "예산·결제·알림은 그대로 두고, 백업·계정·화면 설정은 한곳에 접어 뒀어요. 검색으로 찾거나 필요할 때 열 수 있고, 업데이트 확인은 계속 바로 할 수 있어요.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 촬영한 접힌 앱과 데이터 메뉴. 백업·계정·화면 설정을 한곳에서 펼칠 수 있습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_45_photo_evidence,
                imageAspectRatio = 840f / 734f,
                title = "취소 거래 사진을 같은 창에서 확인",
                description = "취소로 제외된 내역에서 사진을 누르면 거래 부분을 보고 목록으로 돌아오거나, 그 자리에서 후보로 복원할 수 있어요.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 합성 거래 캡처를 같은 취소 내역 창 안에서 확인하는 화면. 가상 카페·금액만 사용했습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_45_insights_tools,
                imageAspectRatio = 904f / 1458f,
                title = "인사이트에서 절약 도구 바로 열기",
                description = "무지출 챌린지, 구독·고정비 레이더, 연말정산 도우미를 각각 눌러 바로 확인할 수 있어요. 각 도구는 필요한 설정과 기록만 열고, 거래는 바꾸지 않아요.",
                imageDescription = "Galaxy Z Fold4 격리 QA 앱에서 합성 데이터로 촬영한 인사이트 화면. 무지출 챌린지, 고정비 레이더, 연말정산 바로가기가 보입니다.",
            ),
        )
        "0.1.44" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_44_compact_home,
                imageAspectRatio = 904f / 1458f,
                title = "홈 요약을 더 간결하게",
                description = "오늘·이번 주 카드와 겹치던 오늘 소비 요약은 기본 홈에서 줄였어요. 홈 카드 구성에서 다시 켤 수 있고, 무지출 챌린지는 시작일과 오늘 지출에 맞는 문구를 보여줘요.",
                imageDescription = "Galaxy Z Fold4 QA 앱에서 촬영한 홈 화면. 월 목표 막대와 오늘·이번 주 지출은 유지하고 중복 오늘 요약은 숨긴 합성 데이터 화면입니다.",
            ),
        )
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
                title = "다른 앱에서 여러 장 바로 공유",
                description = "금융앱 캡처를 여러 장 선택해 모아씀으로 공유하면 한 번에 분석해요. 기존 내역과 겹치는 거래는 걸러내고, 새 거래만 확인한 뒤 기록할 수 있어요.",
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
        "0.1.31" -> listOf(
            ReleaseNoteSlide(
                title = "카카오 로그인과 사진 근거 확인",
                description = "카카오 계정 로그인을 추가했어요. 금융 사진에서 거래 행마다 해당 부분만 확인하고, 애매한 후보도 수정한 뒤 선택할 수 있어요. 앱 내 업데이트는 다운로드 진행률과 설치 안내를 보여줘요.",
            ),
        )
        "0.1.30" -> listOf(
            ReleaseNoteSlide(
                title = "사진 거래를 더 꼼꼼히 확인",
                description = "달력 합계·요일·취소 내역은 걸러내고, 겹친 거래는 한 건만 남겨요. 송금 상대나 결제처 분류가 애매한 항목은 확인 전 저장할 수 없어요. 사진 분석은 무료 ML Kit로 기기 안에서 처리해요.",
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
        "0.1.50" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_50_quick_add,
                imageAspectRatio = 904f / 473f,
                title = "기록 추가 창을 더 빠르게",
                description = "개인·함께 저장과 AI·직접 입력을 한 줄에서 고를 수 있어요. 창을 열 때 키보드를 먼저 띄우지 않아 음성·카메라·사진 입력을 바로 볼 수 있고, 입력 방식 전환도 짧고 부드럽게 이어져요.",
                imageDescription = "Galaxy Z Fold4 격리 QA에서 촬영한 합성 빠른 기록 창. 개인·함께 저장과 AI·직접 입력 선택이 보이며 입력창은 아직 포커스되지 않았습니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_50_manage_search,
                imageAspectRatio = 904f / 760f,
                title = "관리 검색은 결과 화면에서",
                description = "설정 검색을 열면 원래 관리 메뉴 대신 검색 전용 화면만 보여요. 자연어 검색 결과를 바로 확인하고, 돌아와도 입력한 검색어가 유지돼요.",
                imageDescription = "Galaxy Z Fold4 격리 QA에서 캡처한 어두운 테마의 모아씀 설정 검색 결과 화면. 테스트 검색어와 가짜 설정 결과만 담겼습니다.",
            ),
        )
        "0.1.51" -> listOf(
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_51_home_ai,
                imageAspectRatio = 904f / 1458f,
                title = "AI 분석을 홈에서 바로 열어요",
                description = "요약·인사이트·리포트는 세 탭으로 정돈하고, AI는 전용 버튼으로 분리했어요. 홈 탭 내용도 짧고 부드럽게 전환됩니다.",
                imageDescription = "Galaxy Z Fold4 격리 QA에서 촬영한 홈 화면. 거래가 없는 테스트 장부와 모아씀 AI 전용 버튼이 보입니다.",
            ),
            ReleaseNoteSlide(
                imageResId = R.drawable.whats_new_0_1_51_ai_sheet,
                imageAspectRatio = 904f / 1795f,
                title = "AI 소비 코치를 팝업으로",
                description = "AI 분석과 소비 질문을 전용 창에서 이어서 사용할 수 있어요. 화면 전환에는 가벼운 등장 모션을 적용하고, 추천 질문은 잘리지 않게 배치했어요. 분석은 버튼을 눌렀을 때만 기존의 최근 4개월 합계·카테고리 데이터를 사용합니다.",
                imageDescription = "Galaxy Z Fold4 격리 QA에서 촬영한 AI 팝업. 12,000원 QA 합성 지출 한 건만 사용했고, 실제 장부나 개인 사진은 포함하지 않았습니다.",
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
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
                Text("업데이트 소식", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                        Text(
                            "v${pageSlide.releaseVersion ?: versionName} 신기능",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val imageResId = pageSlide.imageResId
                        if (imageResId != null) {
                            Image(
                                painter = painterResource(imageResId),
                                contentDescription = pageSlide.imageDescription,
                                modifier = Modifier.fillMaxWidth().aspectRatio(pageSlide.imageAspectRatio)
                                    .clip(RoundedCornerShape(18.dp)).background(colors.surfaceBase),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth().aspectRatio(pageSlide.imageAspectRatio),
                                shape = RoundedCornerShape(18.dp),
                                color = colors.surfaceBase,
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text(
                                        "지난 업데이트 요약",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                        }
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
