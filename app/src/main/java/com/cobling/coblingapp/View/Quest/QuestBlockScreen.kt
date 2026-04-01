// 블록코딩 게임 메인 화면
//
//  QuestBlockView.kt
//  Cobling
//

package com.cobling.app.view.quest

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
// import androidx.compose.foundation.layout.HStack
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
//import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cobling.app.model.Block
//import com.cobling.app.model.BlockIntroType
//import com.cobling.app.model.SuccessReward
import com.cobling.app.viewmodel.AppState
import com.cobling.app.viewmodel.AuthViewModel
import com.cobling.app.viewmodel.DragManager
//import com.cobling.app.viewmodel.QuestTutorialViewModel
import com.cobling.app.viewmodel.QuestViewModel
//import com.cobling.app.viewmodel.ReviewManager
import com.cobling.app.viewmodel.TabBarViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun QuestBlockView(
    // MARK: - 전달받는 값
    chapterId: String,
    subQuestId: String,

    // 부모(QuestDetailView)에게 상태 변경을 요청하는 콜백
    onGoNextSubQuest: (String) -> Unit,
    onExitToList: () -> Unit,

    // MARK: - Environment
    tabBarViewModel: TabBarViewModel,
    appState: AppState,
    authViewModel: AuthViewModel,

    // MARK: - State / ViewModel
    dragManager: DragManager = remember { DragManager() },
    viewModel: QuestViewModel = remember { QuestViewModel() },

    // 튜토리얼 전용 ViewModel
    // tutorialVM: QuestTutorialViewModel = remember { QuestTutorialViewModel() },

    // reviewManager: ReviewManager = ReviewManager.shared
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 팔레트 영역 프레임 (삭제 판별용)
    var paletteFrame by remember { mutableStateOf(Rect.Zero) }

    // 블록 소개 팝업
    // var currentBlockIntroType by remember { mutableStateOf<BlockIntroType?>(null) }
    // var hasPresentedInitialBlockIntro by remember { mutableStateOf(false) }

    // 튜토리얼 하이라이트 대상 frame들
    // var storyButtonFrame by remember { mutableStateOf(Rect.Zero) }
    // var blockPaletteFrame by remember { mutableStateOf(Rect.Zero) }
    // var blockCanvasFrame by remember { mutableStateOf(Rect.Zero) }
    // var playButtonFrame by remember { mutableStateOf(Rect.Zero) }
    // var stopButtonFrame by remember { mutableStateOf(Rect.Zero) }
    // var flagFrame by remember { mutableStateOf(Rect.Zero) }

    // 첫 튜토리얼 중복 표시 방지
    // var hasPresentedInitialTutorial by remember { mutableStateOf(false) }

    // waiting / locked 상태
    var isWaitingOverlay by remember { mutableStateOf(false) }
    var waitingRetryCount by remember { mutableIntStateOf(0) }
    var showWaitingAlert by remember { mutableStateOf(false) }
    var showLockedAlert by remember { mutableStateOf(false) }

    // "아웃트로 컷신 닫힌 뒤" 다음 퀘스트로 이어가기 플래그
    // var shouldGoNextAfterCutscene by remember { mutableStateOf(false) }

    // - SuccessDialogView -> (진화 조건이면) EvolutionView -> (챕터클리어면) Outro Cutscene -> Next
    // var showEvolution by remember { mutableStateOf(false) }
    // var evolutionReachedLevel by remember { mutableIntStateOf(0) }
    // var shouldGoNextAfterFlow by remember { mutableStateOf(false) }
    // var shouldShowOutroAfterEvolution by remember { mutableStateOf(false) }

    // rewardSettled 대기 리스너(중복 방지)
    var rewardSettledListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var isWaitingRewardSettled by remember { mutableStateOf(false) }

    // 드래그 종료 위치 추적
    var lastDragEndPosition by remember { mutableStateOf(Offset.Zero) }

    // MARK: - 진화 필요 여부/레벨 추출
    // 서버 필드명에 맞게 evolutionLevel 사용
//    val pendingEvolutionLevel: Int? = run {
//        val pending = authViewModel.userProfile?.character?.evolutionPending ?: false
//        if (!pending) {
//            null
//        } else {
//            val lv = authViewModel.userProfile?.character?.evolutionLevel ?: 0
//            if (lv in listOf(5, 10, 15)) lv else null
//        }
//    }

    // 1-1(= ch1 / sq1) 에서만 튜토리얼 표시
    // val isTutorialTargetQuest = chapterId.lowercase() == "ch1" && subQuestId.lowercase() == "sq1"

    // 1-3 / 1-4 / 2-1 / 3-1 / 4-1 에서만 블록 소개 표시
//    val blockIntroTypeForCurrentQuest: BlockIntroType? = run {
//        val ch = chapterId.lowercase()
//        val sq = subQuestId.lowercase()
//
//        when (Pair(ch, sq)) {
//            Pair("ch1", "sq3") -> BlockIntroType.TURN_LEFT
//            Pair("ch1", "sq4") -> BlockIntroType.TURN_RIGHT
//            Pair("ch2", "sq1") -> BlockIntroType.ATTACK
//            Pair("ch3", "sq1") -> BlockIntroType.REPEAT_LOOP
//            Pair("ch4", "sq1") -> BlockIntroType.CONDITION
//            else -> null
//        }
//    }

    // 현재 퀘스트가 블록 인트로 대상인지 확인
    // val isBlockIntroTargetQuest = blockIntroTypeForCurrentQuest != null

    // UserDefaults 저장 키
    // val tutorialStorageKey = "tutorial.quest.${chapterId.lowercase()}.${subQuestId.lowercase()}"

    // MARK: - 삭제 영역 판별
    fun isOverPalette(): Boolean {
        return dragManager.isDragging &&
                dragManager.dragSource == DragManager.DragSource.CANVAS &&
                paletteFrame.contains(dragManager.dragPosition)
    }

    // 리뷰 팝업을 지금 띄워도 되는 상태인지 확인 후 표시 시도
//    fun tryShowPendingReviewPopup() {
//        if (viewModel.isShowingCutscene) return
//        if (tutorialVM.isActive) return
//        if (currentBlockIntroType != null) return
//        if (viewModel.showSuccessDialog) return
//        if (viewModel.isRewardLoading) return
//
//        reviewManager.consumePendingReviewIfNeeded()
//    }

    // rewardSettled 대기 (subQuest progress 문서 리스너)
    fun waitForRewardSettled(
        chapterId: String,
        subQuestId: String,
        timeout: Double,
        completion: (Boolean) -> Unit
    ) {
        rewardSettledListener?.remove()
        rewardSettledListener = null

        val db = FirebaseFirestore.getInstance()
        val uid = authViewModel.currentUserId

        if (uid.isEmpty()) {
            completion(false)
            return
        }

        val ref = db
            .collection("users")
            .document(uid)
            .collection("progress")
            .document(chapterId)
            .collection("subQuests")
            .document(subQuestId)

        var didFinish = false

        scope.launch {
            delay((timeout * 1000).toLong())
            if (didFinish) return@launch

            didFinish = true
            rewardSettledListener?.remove()
            rewardSettledListener = null
            completion(false)
        }

        rewardSettledListener = ref.addSnapshotListener { snap, err ->
            if (didFinish) return@addSnapshotListener

            if (err != null) {
                didFinish = true
                rewardSettledListener?.remove()
                rewardSettledListener = null
                completion(false)
                return@addSnapshotListener
            }

            val data = snap?.data ?: return@addSnapshotListener
            val settled = data["rewardSettled"] as? Boolean ?: false

            if (settled) {
                didFinish = true
                rewardSettledListener?.remove()
                rewardSettledListener = null
                completion(true)
            }
        }
    }

    // Next 플로우: rewardSettled 게이트 추가
    fun handleNextFlowAfterSuccessWithSettlementGate(reward: SuccessReward) {
        if (isWaitingRewardSettled) return

        isWaitingRewardSettled = true
        isWaitingOverlay = true

        waitForRewardSettled(
            chapterId = chapterId,
            subQuestId = subQuestId,
            timeout = 6.0
        ) { settled ->
            isWaitingRewardSettled = false
            isWaitingOverlay = false

            if (!settled) {
                viewModel.showRewardDelayAlert = true
                return@waitForRewardSettled
            }

            scope.launch {
                authViewModel.refreshUserProfileIfNeeded()
                handleNextFlowAfterSuccess(reward)
            }
        }
    }

    // =================================================
    // SuccessDialog Next 이후 “진화 -> 컷신 -> 다음” 플로우 제어
    // =================================================
    fun handleNextFlowAfterSuccess(reward: SuccessReward) {

        // 1) 진화 조건이면: 진화를 먼저 띄우고, 진화 완료 후 다음 액션을 수행
//        if (pendingEvolutionLevel != null) {
//            evolutionReachedLevel = pendingEvolutionLevel
//            showEvolution = true
//
//            // 진화 끝나면 기본적으로 다음으로 진행하도록 예약
//            shouldGoNextAfterFlow = true
//
//            // 챕터 클리어 + 아웃트로 미시청이면 “진화 끝난 뒤 컷신” 예약
//            if (reward.isChapterCleared &&
//                !viewModel.wasOutroShown(viewModel.currentChapterId)
//            ) {
//                shouldShowOutroAfterEvolution = true
//            }
//            return
//        }

        // 2) 진화가 없으면: 기존 로직대로 “챕터 클리어면 컷신 -> 닫히면 다음”
        if (reward.isChapterCleared) {

            // 아웃트로를 이미 봤으면 → 바로 다음으로
            if (viewModel.wasOutroShown(viewModel.currentChapterId)) {
                waitingRetryCount = 0
                isWaitingOverlay = true
                tryGoNextHandlingWaiting()
                return
            }

            // 아직 안 봤으면 → 컷신 띄우고 닫히면 다음으로
//            shouldGoNextAfterCutscene = true
//            viewModel.presentOutroAfterChapterReward(viewModel.currentChapterId)
//
//            // 안전장치: 혹시 VM이 컷신을 안 띄우는 경우(=isShowingCutscene 변화 없음) 바로 진행
//            scope.launch {
//                delay(50)
//                if (shouldGoNextAfterCutscene && !viewModel.isShowingCutscene) {
//                    shouldGoNextAfterCutscene = false
//                    waitingRetryCount = 0
//                    isWaitingOverlay = true
//                    tryGoNextHandlingWaiting()
//                }
//            }
//            return

            waitingRetryCount = 0
            isWaitingOverlay = true
            tryGoNextHandlingWaiting()
            return
        }

        // 3) 일반 케이스
        waitingRetryCount = 0
        isWaitingOverlay = true
        tryGoNextHandlingWaiting()
    }

    // =================================================
    // MARK: - 다음 서브퀘스트 처리 (핵심)
    // =================================================
    fun tryGoNextHandlingWaiting() {
        viewModel.goToNextSubQuest { action ->
            when (action) {

                // 🔁 다음 서브퀘스트
                is QuestViewModel.NextAction.GoToQuest -> {
                    isWaitingOverlay = false
                    onGoNextSubQuest(action.nextId)
                }

                // 📋 리스트로 이동
                is QuestViewModel.NextAction.GoToList -> {
                    isWaitingOverlay = false
                    appState.isInGame = false
                    tabBarViewModel.isTabBarVisible = true
                    onExitToList()
                }

                // ⏳ 서버 대기
                is QuestViewModel.NextAction.Waiting -> {
                    waitingRetryCount += 1
                    val maxRetry = 6

                    if (waitingRetryCount <= maxRetry) {
                        scope.launch {
                            delay(600)
                            tryGoNextHandlingWaiting()
                        }
                    } else {
                        isWaitingOverlay = false
                        showWaitingAlert = true
                    }
                }

                // 🔒 잠김
                is QuestViewModel.NextAction.Locked -> {
                    isWaitingOverlay = false
                    showLockedAlert = true
                }
            }
        }
    }

    // 초기 로딩
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)

//        Log.d("QuestBlockView", "📌 chapterId: $chapterId")
//        Log.d("QuestBlockView", "📌 subQuestId: $subQuestId")
//        Log.d("QuestBlockView", "📌 tutorialStorageKey: $tutorialStorageKey")
//        Log.d("QuestBlockView", "📌 isTutorialTargetQuest: $isTutorialTargetQuest")
//        Log.d("QuestBlockView", "📌 isBlockIntroTargetQuest: $isBlockIntroTargetQuest")
//        Log.d("QuestBlockView", "📌 saved tutorial: ${prefs.getBoolean(tutorialStorageKey, false)}")

        appState.isInGame = true
        tabBarViewModel.isTabBarVisible = false

        // 새 진입 시 frame / 표시 상태 초기화
//        storyButtonFrame = Rect.Zero
//        blockPaletteFrame = Rect.Zero
//        blockCanvasFrame = Rect.Zero
//        playButtonFrame = Rect.Zero
//        stopButtonFrame = Rect.Zero
//        flagFrame = Rect.Zero
//        hasPresentedInitialTutorial = false
//        currentBlockIntroType = null
//        hasPresentedInitialBlockIntro = false

        viewModel.fetchSubQuest(
            chapterId = chapterId,
            subQuestId = subQuestId
        )
    }

    LaunchedEffect(subQuestId) {
        Log.d("QuestBlockView", "🧹 새 서브퀘스트 진입, 블록 초기화: $subQuestId")

        // 1️⃣ 블록 상태 완전 초기화
        viewModel.resetForNewSubQuest()

        // 다음 서브퀘스트 진입 시 튜토리얼 상태/프레임 초기화
//        tutorialVM.resetTutorial()
//        storyButtonFrame = Rect.Zero
//        blockPaletteFrame = Rect.Zero
//        blockCanvasFrame = Rect.Zero
//        playButtonFrame = Rect.Zero
//        stopButtonFrame = Rect.Zero
//        flagFrame = Rect.Zero
//        hasPresentedInitialTutorial = false
//        currentBlockIntroType = null
//        hasPresentedInitialBlockIntro = false

        // 2️⃣ 새 퀘스트 데이터 로드
        viewModel.fetchSubQuest(
            chapterId = chapterId,
            subQuestId = subQuestId
        )
    }

    // 컷신 닫히는 순간 감지
    // 1) 아웃트로 컷신이면 다음 퀘스트 진행
    // 2) 인트로 컷신이 끝난 1-1이면 튜토리얼 시작
//    LaunchedEffect(viewModel.isShowingCutscene) {
//        val isShowing = viewModel.isShowingCutscene
//
//        Log.d("QuestBlockView", "🎬 isShowingCutscene: $isShowing")
//        Log.d("QuestBlockView", "🎬 shouldGoNextAfterCutscene: $shouldGoNextAfterCutscene")
//        Log.d("QuestBlockView", "🎬 hasPresentedInitialTutorial: $hasPresentedInitialTutorial")
//        Log.d("QuestBlockView", "🎬 isTutorialTargetQuest: $isTutorialTargetQuest")
//        Log.d("QuestBlockView", "🎬 isBlockIntroTargetQuest: $isBlockIntroTargetQuest")
//
//        // 1) 아웃트로 컷신 종료 후 다음 퀘스트 이동
//        if (!isShowing && shouldGoNextAfterCutscene) {
//            shouldGoNextAfterCutscene = false
//            waitingRetryCount = 0
//            isWaitingOverlay = true
//            tryGoNextHandlingWaiting()
//            return@LaunchedEffect
//        }
//
//        // 2) 1-1 튜토리얼
//        if (!isShowing &&
//            isTutorialTargetQuest &&
//            !hasPresentedInitialTutorial &&
//            !shouldGoNextAfterCutscene
//        ) {
//            Log.d("QuestBlockView", "✅ 컷신 종료 후 튜토리얼 시작 조건 충족")
//            hasPresentedInitialTutorial = true
//
//            delay(150)
//            tutorialVM.startTutorial(
//                tutorialKey = tutorialStorageKey
//            )
//            return@LaunchedEffect
//        }
//
//        // 3) 1-3 / 1-4 / 2-1 / 3-1 / 4-1 블록 인트로
//        if (!isShowing &&
//            blockIntroTypeForCurrentQuest != null &&
//            !hasPresentedInitialBlockIntro &&
//            !shouldGoNextAfterCutscene
//        ) {
//            Log.d("QuestBlockView", "✅ 컷신 종료 후 블록 인트로 시작")
//            hasPresentedInitialBlockIntro = true
//
//            delay(150)
//            currentBlockIntroType = blockIntroTypeForCurrentQuest
//            return@LaunchedEffect
//        }
//
//        // 컷신이 끝난 시점에 대기 중인 리뷰 팝업 다시 시도
//        if (!isShowing) {
//            delay(100)
//            tryShowPendingReviewPopup()
//        }
//    }

    // 튜토리얼 종료 시 대기 중인 리뷰 팝업 다시 시도
//    LaunchedEffect(tutorialVM.isActive) {
//        if (!tutorialVM.isActive) {
//            delay(100)
//            tryShowPendingReviewPopup()
//        }
//    }

    // subQuest 로드 후 보조 튜토리얼 시작 트리거 추가
//    LaunchedEffect(viewModel.subQuest?.id) {
//        Log.d("QuestBlockView", "📦 subQuest loaded: ${viewModel.subQuest?.id ?: "nil"}")
//        Log.d("QuestBlockView", "📦 isShowingCutscene: ${viewModel.isShowingCutscene}")
//        Log.d("QuestBlockView", "📦 isTutorialTargetQuest: $isTutorialTargetQuest")
//        Log.d("QuestBlockView", "📦 isBlockIntroTargetQuest: $isBlockIntroTargetQuest")
//        Log.d("QuestBlockView", "📦 hasPresentedInitialTutorial: $hasPresentedInitialTutorial")
//        Log.d("QuestBlockView", "📦 hasPresentedInitialBlockIntro: $hasPresentedInitialBlockIntro")
//
//        // 다음 스테이지가 실제로 로드된 후 대기 중인 리뷰 팝업 표시 시도
//        delay(200)
//        tryShowPendingReviewPopup()
//
//        // 1) 1-1 튜토리얼
//        if (isTutorialTargetQuest &&
//            !viewModel.isShowingCutscene &&
//            !hasPresentedInitialTutorial
//        ) {
//            Log.d("QuestBlockView", "✅ subQuest 로드 후 튜토리얼 시작")
//            hasPresentedInitialTutorial = true
//
//            delay(200)
//            tutorialVM.startTutorial(
//                tutorialKey = tutorialStorageKey
//            )
//            return@LaunchedEffect
//        }
//
//        // 2) 1-3 / 1-4 / 2-1 / 3-1 / 4-1 블록 인트로
//        if (blockIntroTypeForCurrentQuest != null &&
//            !viewModel.isShowingCutscene &&
//            !hasPresentedInitialBlockIntro
//        ) {
//            Log.d("QuestBlockView", "✅ subQuest 로드 후 블록 인트로 시작")
//            hasPresentedInitialBlockIntro = true
//
//            delay(200)
//            currentBlockIntroType = blockIntroTypeForCurrentQuest
//        }
//    }

    DisposableEffect(Unit) {
        onDispose {
            rewardSettledListener?.remove()
            rewardSettledListener = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(
                // tutorialVM.isActive,
                // currentBlockIntroType,
                viewModel.isExecuting
            ) {
                detectDragGestures(
                    onDragEnd = {
                        // 튜토리얼 / 블록 소개 중에는 드래그 금지
                        // if (tutorialVM.isActive || currentBlockIntroType != null) return@detectDragGestures
                        if (viewModel.isExecuting) return@detectDragGestures

                        dragManager.finishDrag(lastDragEndPosition) { endPos, source, type, block ->

                            if (viewModel.isExecuting) return@finishDrag

                            // 팔레트 > 반복문 내부
                            if (source == DragManager.DragSource.PALETTE &&
                                type != null &&
                                dragManager.isOverContainer &&
                                dragManager.containerTargetBlock != null
                            ) {
                                dragManager.containerTargetBlock!!.children.add(Block(type))
                                return@finishDrag
                            }

                            // 1️⃣ 캔버스 → 팔레트 (삭제)
                            if (source == DragManager.DragSource.CANVAS &&
                                block != null &&
                                paletteFrame.contains(endPos)
                            ) {
                                Log.d("QuestBlockView", "🧨 DELETE target: ${block.type}, ${block.id}")
                                Log.d(
                                    "QuestBlockView",
                                    "🧨 parent: ${dragManager.draggingParentContainer?.id}"
                                )

                                // 🔥 반복문 내부 블록이면
                                val parent = viewModel.findParentContainer(block)
                                if (parent != null) {
                                    parent.children.removeAll { it.id == block.id }
                                } else {
                                    viewModel.startBlock.children.removeAll { it.id == block.id }
                                }
                                return@finishDrag
                            }

                            // 3️⃣ 팔레트 → 캔버스 (추가)
                            if (source == DragManager.DragSource.PALETTE &&
                                type != null &&
                                dragManager.isOverCanvas
                            ) {
                                val rawIndex = dragManager.canvasInsertIndex
                                    ?: viewModel.startBlock.children.size

                                val safeIndex = rawIndex.coerceIn(
                                    0,
                                    viewModel.startBlock.children.size
                                )

                                viewModel.startBlock.children.add(safeIndex, Block(type))
                                return@finishDrag
                            }

                            // 캔버스에 있던 블록을 반복문 안으로 드롭했을 때
                            if (source == DragManager.DragSource.CANVAS &&
                                block != null &&
                                dragManager.isOverContainer &&
                                dragManager.containerTargetBlock != null
                            ) {
                                val target = dragManager.containerTargetBlock!!

                                // 컨테이너를 자기 자신/자기 자손 컨테이너 안으로 넣는 것 금지 (사이클 방지)
                                if (block.type.isContainer) {
                                    if (target.id == block.id) return@finishDrag
                                    if (viewModel.isDescendant(target, block)) return@finishDrag
                                }

                                // 1️⃣ 기존 위치에서 제거
                                val parent = viewModel.findParentContainer(block)
                                if (parent != null) {
                                    parent.children.removeAll { it.id == block.id }
                                } else {
                                    viewModel.startBlock.children.removeAll { it.id == block.id }
                                }

                                // 2️⃣ 반복문 내부 삽입 위치
                                val rawIndex = dragManager.containerInsertIndex
                                    ?: target.children.size

                                // index 범위 보정 (0 ~ count)
                                val safeIndex = rawIndex.coerceIn(0, target.children.size)

                                target.children.add(safeIndex, block)
                                return@finishDrag
                            }

                            // 4️⃣ 반복문 → 캔버스 (꺼내기)
                            if (source == DragManager.DragSource.CANVAS &&
                                block != null &&
                                viewModel.findParentContainer(block) != null &&
                                dragManager.isOverCanvas
                            ) {
                                val parent = viewModel.findParentContainer(block) ?: return@finishDrag

                                dragManager.isOverContainer = false
                                dragManager.containerTargetBlock = null

                                parent.children.removeAll { it.id == block.id }

                                val rawIndex = dragManager.canvasInsertIndex
                                    ?: viewModel.startBlock.children.size

                                val safeIndex = rawIndex.coerceIn(
                                    0,
                                    viewModel.startBlock.children.size
                                )

                                viewModel.startBlock.children.add(safeIndex, block)
                                return@finishDrag
                            }

                            // 5️⃣ 캔버스 → 캔버스 (재정렬)
                            if (source == DragManager.DragSource.CANVAS &&
                                block != null &&
                                dragManager.isOverCanvas
                            ) {
                                val fromIndex = viewModel.startBlock.children.indexOfFirst { it.id == block.id }

                                if (fromIndex != -1) {
                                    val rawIndex = dragManager.canvasInsertIndex
                                        ?: viewModel.startBlock.children.size

                                    if (fromIndex == rawIndex || fromIndex + 1 == rawIndex) {
                                        return@finishDrag
                                    }

                                    viewModel.startBlock.children.removeAt(fromIndex)

                                    val adjustedRaw = if (fromIndex < rawIndex) rawIndex - 1 else rawIndex
                                    val safeIndex = adjustedRaw.coerceIn(
                                        0,
                                        viewModel.startBlock.children.size
                                    )

                                    viewModel.startBlock.children.add(safeIndex, block)
                                }
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        lastDragEndPosition = change.position
                    }
                )
            }
    ) {
        // =================================================
        // 메인 콘텐츠
        // =================================================
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 게임 맵
            if (viewModel.subQuest != null) {
                GameMapView(
                    viewModel = viewModel,
                    questTitle = viewModel.subQuest!!.title,
                    subQuestId = subQuestId,
//                    storyButtonFrame = storyButtonFrame,
//                    onStoryButtonFrameChanged = { storyButtonFrame = it },
//                    playButtonFrame = playButtonFrame,
//                    onPlayButtonFrameChanged = { playButtonFrame = it },
//                    stopButtonFrame = stopButtonFrame,
//                    onStopButtonFrameChanged = { stopButtonFrame = it },
//                    flagFrame = flagFrame,
//                    onFlagFrameChanged = { flagFrame = it },
                    modifier = Modifier.height(450.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("불러오는 중...")
                    }
                }
            }

            // =================================================
            // 블록 영역
            // =================================================
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ---------- 팔레트 ----------
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .background(Color.White)
                        .onGloballyPositioned { coordinates ->
                            val frame = coordinates.boundsInWindow()
                            paletteFrame = frame
                            // blockPaletteFrame = frame
                        }
                ) {
                    // =================================================
                    // 삭제 오버레이 (팔레트 영역 전체, 여백 없음)
                    // =================================================
                    if (isOverPalette()) {
                        Row(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .fillMaxHeight()
                                    .background(Color.Red.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "삭제",
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 40.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    BlockPaletteView(
                        dragManager = dragManager,
                        viewModel = viewModel,
                        modifier = Modifier.matchParentSize()
                    )
                }

                // ---------- 캔버스 ----------
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .onGloballyPositioned { coordinates ->
                            // blockCanvasFrame = coordinates.boundsInWindow()
                        }
                ) {
                    BlockCanvasView(
                        paletteFrame = paletteFrame,
                        onPaletteFrameChanged = { paletteFrame = it },
                        dragManager = dragManager,
                        viewModel = viewModel,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }

        // =================================================
        // Waiting Overlay
        // =================================================
        if (isWaitingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "다음 퀘스트 여는 중입니다…",
                        color = Color.White
                    )
                }
            }
        }

        // =================================================
        // Reward Loading Overlay (성공 후 보상 정산)
        // =================================================
        if (viewModel.isRewardLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "보상 정산 중입니다…",
                        color = Color.White
                    )
                }
            }
        }

        // =================================================
        // 고스트 블록 (일반 / 반복문 분기)
        // =================================================
        if (dragManager.isDragging && dragManager.draggingType != null) {
            val type = dragManager.draggingType!!

            if (type.isContainer) {
                GhostContainerBlockView(
                    block = dragManager.draggingBlock ?: Block(type),
                    position = dragManager.dragPosition
                )
            } else {
                GhostBlockView(
                    type = type,
                    position = dragManager.dragPosition,
                    offset = dragManager.dragStartOffset
                )
            }
        }

        // =================================================
        // 실패 다이얼로그
        // =================================================
        if (viewModel.showFailureDialog) {
            FailureDialogView(
                onRetry = {
                    viewModel.showFailureDialog = false

                    scope.launch {
                        delay(220)
                        viewModel.resetExecution()
                    }
                }
            )
        }

        // =================================================
        // 성공 다이얼로그
        // =================================================
        if (viewModel.showSuccessDialog && viewModel.successReward != null) {
            SuccessDialogView(
                reward = viewModel.successReward!!,
                characterStage = authViewModel.userProfile?.character?.stage ?: "egg",
                onRetry = {
                    viewModel.showSuccessDialog = false

                    scope.launch {
                        delay(220)
                        viewModel.resetExecution()
                    }
                },
                onNext = {
                    val reward = viewModel.successReward ?: return@SuccessDialogView
                    viewModel.showSuccessDialog = false

                    scope.launch {
                        delay(220)
                        // Next 플로우를 함수로 분리
                        // - SuccessDialog -> Evolution(조건) -> Outro(챕터클리어) -> Next
                        handleNextFlowAfterSuccessWithSettlementGate(reward)
                    }
                }
            )
        }

        // Evolution Overlay
        // - SuccessDialogView 이후, 진화 조건이면 여기 먼저 띄움
        // - "진화 완료" 시 onCompleted에서 다음 단계(컷신/다음퀘)로 이어짐
//        if (showEvolution) {
//            EvolutionView(
//                reachedLevel = evolutionReachedLevel,
//                onCompleted = {
//                    showEvolution = false
//
//                    scope.launch {
//                        authViewModel.completeEvolutionIfNeeded()
//
//                        // 진화 후 아웃트로 예약이면 컷신부터
//                        if (shouldShowOutroAfterEvolution) {
//                            shouldShowOutroAfterEvolution = false
//                            shouldGoNextAfterCutscene = true
//                            viewModel.presentOutroAfterChapterReward(viewModel.currentChapterId)
//                            return@launch
//                        }
//
//                        // 컷신 없으면 다음 퀘스트/리스트로
//                        if (shouldGoNextAfterFlow) {
//                            shouldGoNextAfterFlow = false
//                            waitingRetryCount = 0
//                            isWaitingOverlay = true
//                            tryGoNextHandlingWaiting()
//                        }
//                    }
//                }
//            )
//        }

        // =================================================
        // Chapter Cutscene Overlay (Intro / Outro)
        // =================================================
//        if (viewModel.isShowingCutscene && viewModel.currentCutscene != null) {
//            ChapterCutsceneView(
//                cutscene = viewModel.currentCutscene!!,
//                onClose = {
//                    viewModel.dismissCutsceneAndMarkShown()
//                }
//            )
//        }

        // 컷신 종료 후 게임 화면 위에 튜토리얼 오버레이 표시
//        if (tutorialVM.isActive) {
//            QuestTutorialOverlayView(
//                viewModel = tutorialVM,
//                storyButtonFrame = if (storyButtonFrame == Rect.Zero) null else storyButtonFrame,
//                blockPaletteFrame = if (blockPaletteFrame == Rect.Zero) null else blockPaletteFrame,
//                blockCanvasFrame = if (blockCanvasFrame == Rect.Zero) null else blockCanvasFrame,
//                playButtonFrame = if (playButtonFrame == Rect.Zero) null else playButtonFrame,
//                stopButtonFrame = if (stopButtonFrame == Rect.Zero) null else stopButtonFrame,
//                flagFrame = if (flagFrame == Rect.Zero) null else flagFrame
//            )
//        }

        // 컷신 종료 후 게임 화면 위에 블록 소개 팝업 표시
//        currentBlockIntroType?.let { introType ->
//            BlockIntroView(
//                type = introType,
//                onStart = {
//                    currentBlockIntroType = null
//
//                    // 블록 인트로 종료 후 대기 중인 리뷰 팝업 다시 시도
//                    scope.launch {
//                        delay(100)
//                        tryShowPendingReviewPopup()
//                    }
//                }
//            )
//        }

        // =================================================
        // 리뷰 팝업
        // =================================================
//        if (reviewManager.shouldShowReviewPopup && reviewManager.currentMilestone != null) {
//            ReviewPromptView(
//                milestone = reviewManager.currentMilestone!!,
//                onNegative = {
//                    reviewManager.handleNegativeFeedback()
//                },
//                onPositive = {
//                    reviewManager.handlePositiveFeedback()
//                }
//            )
//        }
    }

    // 알럿
    if (showWaitingAlert) {
        AlertDialog(
            onDismissRequest = { showWaitingAlert = false },
            confirmButton = {
                TextButton(onClick = { showWaitingAlert = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("⏳ 챕터를 여는 중이에요")
            },
            text = {
                Text("서버 반영이 지연되고 있어요.\n잠시 후 다시 시도해 주세요.")
            }
        )
    }

    if (showLockedAlert) {
        AlertDialog(
            onDismissRequest = { showLockedAlert = false },
            confirmButton = {
                TextButton(onClick = { showLockedAlert = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("🔒 잠긴 퀘스트입니다")
            },
            text = {
                Text("선행 퀘스트를 먼저 완료해 주세요.")
            }
        )
    }

//    // (선택) 보상 정산 지연 알럿
//    if (viewModel.showRewardDelayAlert) {
//        AlertDialog(
//            onDismissRequest = { viewModel.showRewardDelayAlert = false },
//            confirmButton = {
//                TextButton(onClick = { viewModel.showRewardDelayAlert = false }) {
//                    Text("확인")
//                }
//            },
//            title = {
//                Text("⏳ 보상 정산이 지연되고 있어요")
//            },
//            text = {
//                Text("서버 반영이 지연되고 있어요.\n잠시 후 다시 시도해 주세요.")
//            }
//        )
//    }
}