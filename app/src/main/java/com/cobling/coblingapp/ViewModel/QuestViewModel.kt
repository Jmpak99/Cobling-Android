// 퀘스트 실행 / 상태 / 블록 트리 관리

// 캐릭터 방향 회전
//앞으로 이동
//벽/맵 범위 체크
//시작 블록 트리 실행
//반복문 실행
//목표 지점 도달 시 성공 다이얼로그 표시
//실패 시 시작 위치로 리셋

package com.cobling.app.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cobling.app.model.Block
import com.cobling.app.model.BlockType
//import com.cobling.app.model.ChapterCutscene
//import com.cobling.app.model.ChapterCutsceneType
//import com.cobling.app.model.ChapterDialogueStore
//import com.cobling.app.model.Enemy
//import com.cobling.app.model.IfCondition
//import com.cobling.app.model.SubQuestDocument
//import com.cobling.app.model.SuccessReward
//import com.cobling.app.util.LocalStorageManager
//import com.cobling.app.util.ReviewManager
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.DocumentReference
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ListenerRegistration
//import com.google.firebase.firestore.Source
import java.util.UUID
//import kotlin.math.max

// MARK: - 캐릭터 방향 열거형 정의
enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    fun turnedLeft(): Direction {
        return when (this) {
            UP -> LEFT
            LEFT -> DOWN
            DOWN -> RIGHT
            RIGHT -> UP
        }
    }

    fun turnedRight(): Direction {
        return when (this) {
            UP -> RIGHT
            RIGHT -> DOWN
            DOWN -> LEFT
            LEFT -> UP
        }
    }

    companion object {
        fun fromFirestore(value: String?): Direction {
            return when (value?.lowercase()) {
                "up" -> UP
                "down" -> DOWN
                "left" -> LEFT
                "right" -> RIGHT
                else -> RIGHT
            }
        }
    }
}

// MARK: - 다음 퀘스트 이동 액션 정의
/*
sealed class NextQuestAction {
    data class GoToQuest(val questId: String) : NextQuestAction()
    data object Locked : NextQuestAction()
    data object Waiting : NextQuestAction()
    data object GoToList : NextQuestAction()
}
*/

data class GridPosition(
    val row: Int = 0,
    val col: Int = 0
)

// MARK: - 퀘스트 실행 뷰모델
class QuestViewModel : ViewModel() {

    private val mainHandler = Handler(Looper.getMainLooper())

    // MARK: - 게임 상태
    var characterPosition by mutableStateOf(GridPosition(0, 0))
        private set

    var characterDirection by mutableStateOf(Direction.RIGHT)
        private set

    // DB startDirection 값을 저장해두는 용도 (reset 시 이 값으로 복구)
    private var startDirection: Direction = Direction.RIGHT

    var mapData by mutableStateOf(listOf<List<Int>>())         // Firestore에서 변환된 맵
        private set

    var showFailureDialog by mutableStateOf(false)
    var showSuccessDialog by mutableStateOf(false)
    var startBlock by mutableStateOf(Block(type = BlockType.START))
    var currentExecutingBlockID by mutableStateOf<UUID?>(null)
    var isExecuting by mutableStateOf(false)
    var didFailExecution by mutableStateOf(false)

    // "멈춤" 요청 플래그 (즉시 중단용)
    var didStopExecution by mutableStateOf(false)

    // 실행 세션 토큰 (postDelayed가 남아있어도 무효화)
    private var executionToken: UUID = UUID.randomUUID()

    // MARK: - Success Reward
//    var successReward by mutableStateOf<SuccessReward?>(null)

    // - QuestDetailView 최초 진입 시 intro 1회
    // - 챕터 클리어 보상(2단 게이지) 끝난 뒤 outro 표시
//    var isShowingCutscene by mutableStateOf(false)
//    var currentCutscene by mutableStateOf<ChapterCutscene?>(null)

    // 보상 정산 중 오버레이 표시 여부
//    var isRewardLoading by mutableStateOf(false)
//    var showRewardDelayAlert by mutableStateOf(false)

    // MARK: - 적
//    var initialEnemies by mutableStateOf(listOf<Enemy>())
//        private set
//
//    var enemies by mutableStateOf(listOf<Enemy>())

    // MARK: - Firestore
//    var subQuest by mutableStateOf<SubQuestDocument?>(null)
    var startPosition by mutableStateOf(GridPosition(0, 0))
        private set

    var goalPosition by mutableStateOf(GridPosition(0, 0))
        private set

    var allowedBlocks by mutableStateOf(listOf<BlockType>())

//    // if 조건 옵션(스테이지별)
//    var currentAllowedIfConditions by mutableStateOf(IfCondition.entries.toList())
//    var currentDefaultIfCondition by mutableStateOf(IfCondition.FRONT_IS_CLEAR)
//
//    private val db = FirebaseFirestore.getInstance()

    // fetch로 받은 식별자 저장 (클리어 시 progress 문서 지정에 사용)
    var currentChapterId: String = ""
    private var currentSubQuestId: String = ""

    // unlock 대기 리스너(중복 등록 방지)
//    private var unlockListener: ListenerRegistration? = null

    // users 업데이트 감지 리스너 (보관 / 중복 제거용)
//    private var userUpdateListener: ListenerRegistration? = null

    // 챕터 보너스 필드 반영 대기 리스너(레이스 해결용)
//    private var chapterBonusListener: ListenerRegistration? = null

    // 보상 로딩 시작 시간(최소 표시 시간 보장용)
//    private var rewardLoadingStartedAt: Long? = null

    // 오버레이 최소 표시 시간 (0.3~0.6 사이로 조절)
//    private val minRewardOverlayDuration: Double = 0.45

    override fun onCleared() {
        super.onCleared()
//        unlockListener?.remove()
//        userUpdateListener?.remove()
//        chapterBonusListener?.remove()
    }

    fun resetForNewSubQuest() {
        println("🧹 resetForNewSubQuest() 호출")

        // ▶️ 실행 세션 무효화
        executionToken = UUID.randomUUID()
        didStopExecution = false

        // ▶️ 블록 트리 초기화
        startBlock = Block(type = BlockType.START)

        // ▶️ 실행 상태 초기화
        isExecuting = false
        didFailExecution = false
        currentExecutingBlockID = null

        // ▶️ 캐릭터 상태 초기화
        characterPosition = startPosition
        characterDirection = startDirection

        // ▶️ 적 상태 초기화
//        enemies = initialEnemies

        // ▶️ 다이얼로그 초기화
        showFailureDialog = false
        showSuccessDialog = false
//        successReward = null

        // ▶️ 로딩 오버레이도 초기화
//        isRewardLoading = false
//        rewardLoadingStartedAt = null
    }

    // 멈춤(Stop): 실행 즉시 중단 + 무조건 시작으로 리셋
    fun stopExecution() {
        mainHandler.post {
            didStopExecution = true

            // 기존에 예약된 postDelayed 콜백 전부 무효화
            executionToken = UUID.randomUUID()

            // 실행 상태 정리
            isExecuting = false
            didFailExecution = false
            currentExecutingBlockID = null

            // 시작 상태로 강제 복귀(다이얼로그는 띄우지 않음)
            characterPosition = startPosition
            characterDirection = startDirection
//            enemies = initialEnemies

            // 실패/성공 다이얼로그는 STOP에서는 띄우지 않게
            showFailureDialog = false
            showSuccessDialog = false

            println("⏹️ stopExecution(): 실행 중단 + 시작 위치로 리셋 완료")
        }
    }

    // 현재 토큰이 유효한지 체크하는 헬퍼
    private fun isTokenValid(token: UUID): Boolean {
        return token == executionToken && !didStopExecution
    }

    // =================================================
    // 컷신(인트로/아웃트로) "봤는지" 조회용 헬퍼
    // =================================================
    /*
        fun wasCutsceneShown(chapterId: String, type: ChapterCutsceneType): Boolean {
            return LocalStorageManager.isCutsceneShown(chapterId, type)
        }

        fun wasOutroShown(chapterId: String): Boolean {
            return LocalStorageManager.isCutsceneShown(chapterId, ChapterCutsceneType.OUTRO)
        }

        // Chapter Cutscene Control
        // - intro: QuestDetailView 최초 진입에서 호출
        // - outro: 챕터 보상(2단 게이지) 끝난 뒤 호출
        fun presentIntroIfNeeded(chapterId: String) {
            if (LocalStorageManager.isCutsceneShown(chapterId, ChapterCutsceneType.INTRO)) {
                return
            }

            // ChapterDialogueStore에서 라인 가져오기
            val lines = ChapterDialogueStore.lines(chapterId, ChapterCutsceneType.INTRO)

            // ChapterCutscene로 감싸기 (lines 비어있을 때 방어)
            if (lines.isEmpty()) return

            val cutscene = ChapterCutscene(
                chapterId = chapterId,
                type = ChapterCutsceneType.INTRO,
                lines = lines
            )

            mainHandler.post {
                currentCutscene = cutscene
                isShowingCutscene = true
            }
        }

        /// 정책 : 챕터 클리어 보상(2단 게이지) 끝난 뒤 호출
        fun presentOutroAfterChapterReward(chapterId: String) {
            if (LocalStorageManager.isCutsceneShown(chapterId, ChapterCutsceneType.OUTRO)) {
                return
            }

            // ChapterDialogueStore에서 라인 가져오기
            val lines = ChapterDialogueStore.lines(chapterId, ChapterCutsceneType.OUTRO)

            if (lines.isEmpty()) return

            val cutscene = ChapterCutscene(
                chapterId = chapterId,
                type = ChapterCutsceneType.OUTRO,
                lines = lines
            )

            mainHandler.post {
                currentCutscene = cutscene
                isShowingCutscene = true
            }
        }

        fun dismissCutsceneAndMarkShown() {
            val cutscene = currentCutscene
            if (cutscene == null) {
                isShowingCutscene = false
                return
            }

            LocalStorageManager.setCutsceneShown(cutscene.chapterId, cutscene.type)

            mainHandler.post {
                isShowingCutscene = false
                currentCutscene = null
            }
        }

        // 보상 정산 로딩 시작 (오버레이 ON)
        private fun beginRewardLoading() {
            mainHandler.post {
                rewardLoadingStartedAt = System.currentTimeMillis()
                isRewardLoading = true
            }
        }

        // 보상 정산 로딩 종료 + (성공 다이얼로그 표시를) 최소표시시간 이후 실행
        private fun endRewardLoadingAndShowSuccess(showSuccess: () -> Unit) {
            val started = rewardLoadingStartedAt ?: System.currentTimeMillis()
            val elapsed = (System.currentTimeMillis() - started).toDouble() / 1000.0
            val remaining = max(0.0, minRewardOverlayDuration - elapsed)

            mainHandler.postDelayed({
                isRewardLoading = false
                rewardLoadingStartedAt = null
                showSuccess()
            }, (remaining * 1000).toLong())
        }
    */

    // SubQuest rules에서 if 조건 옵션/기본값을 ViewModel에 반영
    /*
        private fun applyIfRules(subQuest: SubQuestDocument) {
            // 1) 허용 조건 리스트 (없으면 전체 허용)
            val allowedRaw = subQuest.rules.allowedIfConditions ?: emptyList()
            val allowed = allowedRaw.mapNotNull { raw ->
                IfCondition.fromRawValue(raw)
            }

            currentAllowedIfConditions =
                if (allowed.isEmpty()) IfCondition.entries.toList() else allowed

            // 2) 기본 조건 (없거나 잘못된 값이면 frontIsClear)
            currentDefaultIfCondition =
                IfCondition.fromRawValue(subQuest.rules.defaultIfCondition)
                    ?: IfCondition.FRONT_IS_CLEAR

            println(
                "🟩 IF 룰 반영 완료 allowed: ${
                    currentAllowedIfConditions.map { it.rawValue }
                } default: ${currentDefaultIfCondition.rawValue}"
            )
        }
    */

    // MARK: - Firestore에서 SubQuest 불러오기
    /*
        fun fetchSubQuest(chapterId: String, subQuestId: String) {
            // 현재 컨텍스트 보관
            currentChapterId = chapterId
            currentSubQuestId = subQuestId

            db.collection("quests")
                .document(chapterId)
                .collection("subQuests")
                .document(subQuestId)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val loaded = snapshot.toObject(SubQuestDocument::class.java)
                        if (loaded != null) {
                            mainHandler.post {
                                subQuest = loaded

                                // 맵 데이터
                                mapData = loaded.map.parsedGrid

                                // 시작/목표 위치
                                startPosition = GridPosition(
                                    loaded.map.start.row,
                                    loaded.map.start.col
                                )
                                goalPosition = GridPosition(
                                    loaded.map.goal.row,
                                    loaded.map.goal.col
                                )

                                // 적 목록 로드 (원본저장 + 현재 값 세팅)
                                val loadedEnemies = (loaded.map.enemies ?: emptyList()).filter {
                                    it.id.trim().isNotEmpty()
                                }

                                initialEnemies = loadedEnemies
                                enemies = loadedEnemies

                                // 캐릭터 위치 초기화
                                characterPosition = startPosition

                                // 방향 초기화 + 시작 방향 저장
                                val dir = Direction.fromFirestore(loaded.map.startDirection)
                                startDirection = dir
                                characterDirection = dir

                                // 허용 블록 반영
                                allowedBlocks = loaded.rules.allowBlocks.mapNotNull { raw ->
                                    BlockType.fromRawValue(raw)
                                }

                                // if 조건 룰(allowed/default) 반영
                                applyIfRules(loaded)

                                println("✅ 불러온 서브퀘스트: ${loaded.title}")
                                println("📦 허용 블록: $allowedBlocks")
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ 디코딩 실패: $e")
                    }
                }
                .addOnFailureListener { error ->
                    println("Firestore 불러오기 실패: $error")
                }
        }
    */

    // MARK: - (공통) locked → inProgress/completed 될 때까지 대기
    /*
        private fun waitUntilUnlocked(
            progressRef: DocumentReference,
            timeoutSeconds: Double = 4.0,
            onUnlocked: () -> Unit,
            onTimeout: () -> Unit
        ) {
            unlockListener?.remove()
            var done = false

            // 타임아웃 (무한 대기 방지)
            mainHandler.postDelayed({
                if (done) return@postDelayed
                done = true
                unlockListener?.remove()
                unlockListener = null
                onTimeout()
            }, (timeoutSeconds * 1000).toLong())

            unlockListener = progressRef.addSnapshotListener { snap, err ->
                if (done) return@addSnapshotListener

                if (err != null) {
                    println("❌ unlock listener error: $err")
                    return@addSnapshotListener
                }

                val state = snap?.getString("state") ?: "locked"

                if (state == "inProgress" || state == "completed") {
                    done = true
                    unlockListener?.remove()
                    unlockListener = null
                    onUnlocked()
                }
            }
        }
    */

    // MARK: - 퀘스트 "진입" 게이트
    /*
        fun ensureSubQuestAccessible(
            chapterId: String,
            subQuestId: String,
            timeoutSeconds: Double = 4.0,
            completion: (NextQuestAction) -> Unit
        ) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                completion(NextQuestAction.Locked)
                return
            }

            val progressRef = db.collection("users")
                .document(userId)
                .collection("progress")
                .document(chapterId)
                .collection("subQuests")
                .document(subQuestId)

            // 서버 우선으로 읽어서 "캐시 locked" 오판 줄이기
            progressRef.get(Source.SERVER)
                .addOnSuccessListener { snap ->
                    val state = snap.getString("state") ?: "locked"
                    handleAccessState(
                        state = state,
                        progressRef = progressRef,
                        subQuestId = subQuestId,
                        timeoutSeconds = timeoutSeconds,
                        completion = completion
                    )
                }
                .addOnFailureListener {
                    // 서버 read 실패(오프라인 등)면 캐시로 fallback
                    progressRef.get()
                        .addOnSuccessListener { snap2 ->
                            val state2 = snap2.getString("state") ?: "locked"
                            handleAccessState(
                                state = state2,
                                progressRef = progressRef,
                                subQuestId = subQuestId,
                                timeoutSeconds = timeoutSeconds,
                                completion = completion
                            )
                        }
                        .addOnFailureListener {
                            completion(NextQuestAction.Locked)
                        }
                }
        }

        private fun handleAccessState(
            state: String,
            progressRef: DocumentReference,
            subQuestId: String,
            timeoutSeconds: Double,
            completion: (NextQuestAction) -> Unit
        ) {
            when (state) {
                "inProgress", "completed" -> completion(NextQuestAction.GoToQuest(subQuestId))

                "locked" -> {
                    // 잠깐 locked일 수 있으니 기다렸다가 열리면 진입
                    waitUntilUnlocked(
                        progressRef = progressRef,
                        timeoutSeconds = timeoutSeconds,
                        onUnlocked = { completion(NextQuestAction.GoToQuest(subQuestId)) },
                        onTimeout = { completion(NextQuestAction.Waiting) }
                    )
                }

                else -> completion(NextQuestAction.Locked)
            }
        }
    */

    // MARK: - 다음 퀘스트 찾기 로직 (locked면 waiting 대기)
    /*
        fun goToNextSubQuest(completion: (NextQuestAction) -> Unit) {
            val currentSubQuest = subQuest
            if (currentSubQuest == null) {
                completion(NextQuestAction.GoToList)
                return
            }

            val nextOrder = currentSubQuest.order + 1
            val chapterRef = db.collection("quests")
                .document(currentChapterId)
                .collection("subQuests")

            chapterRef.whereEqualTo("order", nextOrder)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    if (doc == null) {
                        println("📋 다음 퀘스트 없음 → 리스트로")
                        completion(NextQuestAction.GoToList)
                        return@addOnSuccessListener
                    }

                    val nextId = doc.id

                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId == null) {
                        println("❌ 로그인 유저 없음")
                        completion(NextQuestAction.Locked)
                        return@addOnSuccessListener
                    }

                    val progressRef = db.collection("users")
                        .document(userId)
                        .collection("progress")
                        .document(currentChapterId)
                        .collection("subQuests")
                        .document(nextId)

                    // 다음 퀘스트도 서버 우선으로 읽기(캐시 locked 완화)
                    progressRef.get(Source.SERVER)
                        .addOnSuccessListener { snap ->
                            val state = snap.getString("state") ?: "locked"
                            handleNextState(
                                state = state,
                                progressRef = progressRef,
                                nextId = nextId,
                                completion = completion
                            )
                        }
                        .addOnFailureListener {
                            // 서버 read 실패면 캐시 fallback
                            progressRef.get()
                                .addOnSuccessListener { snap2 ->
                                    val state2 = snap2.getString("state") ?: "locked"
                                    handleNextState(
                                        state = state2,
                                        progressRef = progressRef,
                                        nextId = nextId,
                                        completion = completion
                                    )
                                }
                                .addOnFailureListener { error ->
                                    println("❌ Error fetching next subQuest: $error")
                                    completion(NextQuestAction.GoToList)
                                }
                        }
                }
                .addOnFailureListener { error ->
                    println("❌ Error fetching next subQuest: $error")
                    completion(NextQuestAction.GoToList)
                }
        }

        private fun handleNextState(
            state: String,
            progressRef: DocumentReference,
            nextId: String,
            completion: (NextQuestAction) -> Unit
        ) {
            when (state) {
                "inProgress", "completed" -> completion(NextQuestAction.GoToQuest(nextId))

                "locked" -> {
                    waitUntilUnlocked(
                        progressRef = progressRef,
                        timeoutSeconds = 4.0,
                        onUnlocked = { completion(NextQuestAction.GoToQuest(nextId)) },
                        onTimeout = { completion(NextQuestAction.Waiting) }
                    )
                }

                else -> completion(NextQuestAction.Locked)
            }
        }
    */

    // MARK: - IF 조건 판정
    /*
        private fun evaluateIfCondition(cond: IfCondition): Boolean {
            return when (cond) {
                IfCondition.FRONT_IS_CLEAR -> isFrontClear()
                IfCondition.FRONT_IS_BLOCKED -> !isFrontClear()
                IfCondition.ENEMY_IN_FRONT -> isEnemyInFront()
                else -> false
            }
        }
    */

    // MARK: - 앞칸이 이동 가능한지(벽/맵 범위 체크)
    private fun isFrontClear(): Boolean {
        if (mapData.isEmpty() || mapData.firstOrNull().isNullOrEmpty()) return false

        val r = characterPosition.row
        val c = characterPosition.col
        var nr = r
        var nc = c

        when (characterDirection) {
            Direction.UP -> nr -= 1
            Direction.DOWN -> nr += 1
            Direction.LEFT -> nc -= 1
            Direction.RIGHT -> nc += 1
        }

        // 범위
        if (nr !in mapData.indices || nc !in mapData[0].indices) return false

        // 벽(0)인지
        return mapData[nr][nc] != 0
    }

    // MARK: - 한 칸 앞에 적이 있는지
    /*
        private fun isEnemyInFront(): Boolean {
            val r = characterPosition.row
            val c = characterPosition.col
            var nr = r
            var nc = c

            when (characterDirection) {
                Direction.UP -> nr -= 1
                Direction.DOWN -> nr += 1
                Direction.LEFT -> nc -= 1
                Direction.RIGHT -> nc += 1
            }

            return enemies.any { it.row == nr && it.col == nc }
        }
    */

    // MARK: - 블록 실행 시작
    fun startExecution() {
        if (isExecuting) return

        // 새 실행 시작 시 stop 플래그 해제 + 토큰 갱신
        didStopExecution = false
        executionToken = UUID.randomUUID()
        val token = executionToken

        didFailExecution = false
        isExecuting = true

        executeBlocks(
            blocks = startBlock.children,
            isTopLevel = true,
            token = token
        ) {
            // 최상위 실행 종료
        }
    }

    // MARK: - 블록 리스트 순차 실행
    fun executeBlocks(
        blocks: List<Block>,
        index: Int = 0,
        isTopLevel: Boolean = false,
        token: UUID,
        completion: () -> Unit
    ) {
        // STOP 누르면 즉시 종료
        if (!isTokenValid(token)) {
            println("⏹️ 실행 중단: 토큰 무효(Stop 또는 새 실행)")
            return
        }

        // 실패 시 즉시 중단
        if (didFailExecution) {
            println("실행 중단 : 실패 상태")
            return
        }

        if (index >= blocks.size) {
            // 종료 직전에도 토큰 체크
            if (!isTokenValid(token)) return

            if (!isTopLevel) {
                completion()
                return
            }

            // 실패 상태면 그냥 종료 (위로 전파 안 함)
            if (didFailExecution) {
                return
            }

            println("✅ 모든 블록 실행 완료")

            // 도착 지점 검사
            if (characterPosition != goalPosition) {
                println("실패 : 깃발에 도달하지 못함")
                resetToStart()
                return
            }

            // 적이 하나라도 남아있으면 실패
//            if (enemies.isNotEmpty()) {
//                println("실패 : 적을 모두 처치하지 않음")
//                resetToStart()
//                return
//            }

            // 성공 (깃발 + 적 전부 처치)
            println("성공 : 깃발 도착")
            isExecuting = false
            showSuccessDialog = true

            // showSuccessDialog 여기서 켜지 않음 (reward 생성 후 켜야 함)
//            subQuest?.let {
//                handleQuestClear(it, countUsedBlocks())
//            }

            completion()
            return
        }

        val current = blocks[index]
        currentExecutingBlockID = current.id
        println("▶️ 현재 실행 중인 블록: ${current.type}")

        when (current.type) {
            BlockType.MOVE_FORWARD -> {
                moveForward {
                    mainHandler.postDelayed({
                        if (!isTokenValid(token)) return@postDelayed
                        executeBlocks(
                            blocks = blocks,
                            index = index + 1,
                            isTopLevel = isTopLevel,
                            token = token,
                            completion = completion
                        )
                    }, 500)
                }
            }

            BlockType.TURN_LEFT -> {
                characterDirection = characterDirection.turnedLeft()
                mainHandler.postDelayed({
                    executeBlocks(
                        blocks = blocks,
                        index = index + 1,
                        isTopLevel = isTopLevel,
                        token = token,
                        completion = completion
                    )
                }, 300)
            }

            BlockType.TURN_RIGHT -> {
                characterDirection = characterDirection.turnedRight()
                mainHandler.postDelayed({
                    executeBlocks(
                        blocks = blocks,
                        index = index + 1,
                        isTopLevel = isTopLevel,
                        token = token,
                        completion = completion
                    )
                }, 300)
            }

            BlockType.ATTACK -> {
                /*
                                attack {
                                    mainHandler.postDelayed({
                                        executeBlocks(
                                            blocks = blocks,
                                            index = index + 1,
                                            isTopLevel = isTopLevel,
                                            token = token,
                                            completion = completion
                                        )
                                    }, 350)
                                }
                */
                mainHandler.postDelayed({
                    executeBlocks(
                        blocks = blocks,
                        index = index + 1,
                        isTopLevel = isTopLevel,
                        token = token,
                        completion = completion
                    )
                }, 350)
            }

            BlockType.REPEAT_COUNT -> {
                val repeatCount = current.value?.toIntOrNull() ?: 1

                fun runRepeat(remaining: Int) {
                    if (!isTokenValid(token)) return

                    // 반복문 종료 시점
                    if (remaining <= 0) {
                        // 다음 블럭으로 진행
                        executeBlocks(
                            blocks = blocks,
                            index = index + 1,
                            isTopLevel = isTopLevel,
                            token = token,
                            completion = completion
                        )
                        return
                    }

                    // 1. 반복문 블록 강조
                    mainHandler.post {
                        currentExecutingBlockID = current.id
                    }

                    // 2. 잠깐 깜빡이게 딜레이
                    mainHandler.postDelayed({
                        if (!isTokenValid(token)) return@postDelayed

                        executeBlocks(current.children, token = token) {
                            runRepeat(remaining - 1)
                        }
                    }, 200)
                }

                runRepeat(repeatCount)
            }

            BlockType.IF -> {
                /*
                                val condition = current.condition
                                val shouldRun = evaluateIfCondition(condition)

                                mainHandler.post {
                                    currentExecutingBlockID = current.id
                                }

                                mainHandler.postDelayed({
                                    if (!isTokenValid(token)) return@postDelayed

                                    if (shouldRun) {
                                        executeBlocks(current.children, token = token) {
                                            executeBlocks(
                                                blocks = blocks,
                                                index = index + 1,
                                                isTopLevel = isTopLevel,
                                                token = token,
                                                completion = completion
                                            )
                                        }
                                    } else {
                                        executeBlocks(
                                            blocks = blocks,
                                            index = index + 1,
                                            isTopLevel = isTopLevel,
                                            token = token,
                                            completion = completion
                                        )
                                    }
                                }, 200)
                */
                executeBlocks(
                    blocks = blocks,
                    index = index + 1,
                    isTopLevel = isTopLevel,
                    token = token,
                    completion = completion
                )
            }

            else -> {
                mainHandler.postDelayed({
                    if (!isTokenValid(token)) return@postDelayed
                    executeBlocks(
                        blocks = blocks,
                        index = index + 1,
                        isTopLevel = isTopLevel,
                        token = token,
                        completion = completion
                    )
                }, 300)
            }
        }
    }

    fun findParentContainer(target: Block): Block? {
        fun search(container: Block): Block? {
            if (container.children.any { it.id == target.id }) {
                return container
            }

            for (child in container.children) {
                if (child.type.isContainer) {
                    val found = search(child)
                    if (found != null) {
                        return found
                    }
                }
            }
            return null
        }

        return search(startBlock)
    }

    // MARK: - target이 ancestor의 "자손(하위 컨테이너)"인지 판별
    fun isDescendant(target: Block, ancestor: Block): Boolean {
        // ancestor 아래를 DFS로 탐색해서 target이 나오면 true
        fun dfs(node: Block): Boolean {
            for (child in node.children) {
                if (child.id == target.id) return true
                if (child.type.isContainer) {
                    if (dfs(child)) return true
                }
            }
            return false
        }

        return dfs(ancestor)
    }

    // MARK: - EXP 테이블 (서버와 동일)
    /*
        fun maxExpForLevel(level: Int): Double {
            val table = mapOf(
                1 to 100.0, 2 to 120.0, 3 to 160.0, 4 to 200.0, 5 to 240.0,
                6 to 310.0, 7 to 380.0, 8 to 480.0, 9 to 600.0, 10 to 750.0,
                11 to 930.0, 12 to 1160.0, 13 to 1460.0, 14 to 1820.0, 15 to 2270.0,
                16 to 2840.0, 17 to 3550.0, 18 to 4440.0, 19 to 5550.0
            )
            return table[level] ?: 100.0
        }

        // 보너스 exp를 로컬로 적용해서 (level, exp)를 계산하는 헬퍼
        private fun applyGainLocally(
            level: Int,
            exp: Double,
            gain: Int
        ): Pair<Int, Double> {
            var lv = level
            var e = exp + max(0, gain).toDouble()

            while (e >= maxExpForLevel(lv)) {
                e -= maxExpForLevel(lv)
                lv += 1
            }
            return lv to e
        }
    */

    // MARK: - USERS 업데이트를 기다리는 헬퍼
    /*
        private fun waitForUserUpdate(
            userRef: DocumentReference,
            previousLevel: Int,
            previousExp: Double,
            timeout: Double = 6.0,
            completion: (Int, Double) -> Unit,
            onTimeout: () -> Unit
        ) {
            // 기존 리스너 제거 (중복 등록 방지)
            userUpdateListener?.remove()
            userUpdateListener = null

            var done = false

            // 타임아웃
            mainHandler.postDelayed({
                if (done) return@postDelayed
                done = true
                userUpdateListener?.remove()
                userUpdateListener = null
                onTimeout()
            }, (timeout * 1000).toLong())

            // listener를 userUpdateListener에 저장해서 관리
            userUpdateListener = userRef.addSnapshotListener { snap, err ->
                if (done) return@addSnapshotListener
                if (err != null) {
                    println("❌ waitForUserUpdate listener error: $err")
                    return@addSnapshotListener
                }

                val data = snap?.data ?: return@addSnapshotListener
                val level = (data["level"] as? Number)?.toInt() ?: 1
                val exp = (data["exp"] as? Number)?.toDouble() ?: 0.0

                // (level, exp)가 이전과 달라졌으면 "정산 완료"로 간주
                if (level != previousLevel || exp != previousExp) {
                    done = true
                    userUpdateListener?.remove()
                    userUpdateListener = null
                    completion(level, exp)
                }
            }
        }

        // 챕터 보너스 정보를 읽어오는 헬퍼
        private fun fetchChapterBonusInfo(
            userId: String,
            chapterId: String,
            subQuestId: String,
            completion: (Boolean, Int) -> Unit
        ) {
            val subQuestProgressRef = db.collection("users")
                .document(userId)
                .collection("progress")
                .document(chapterId)
                .collection("subQuests")
                .document(subQuestId)

            subQuestProgressRef.get(Source.SERVER)
                .addOnSuccessListener { snap ->
                    val data = snap.data ?: emptyMap<String, Any>()

                    val cleared = data["chapterClearGranted"] as? Boolean ?: false

                    val bonusExp = when (val raw = data["chapterBonusExpGranted"]) {
                        is Int -> raw
                        is Long -> raw.toInt()
                        is Double -> raw.toInt()
                        else -> 0
                    }

                    completion(cleared, bonusExp)
                }
                .addOnFailureListener {
                    completion(false, 0)
                }
        }

        // 미션 결과 + 미션 보상 EXP를 함께 읽어오는 헬퍼로 확장
        private fun fetchMissionResultInfo(
            userId: String,
            chapterId: String,
            subQuestId: String,
            completion: (
                didJustCompleteDailyMission: Boolean,
                didJustCompleteMonthlyMission: Boolean,
                isDailyMissionCompleted: Boolean,
                isMonthlyMissionCompleted: Boolean,
                dailyMissionRewardExp: Int,
                monthlyMissionRewardExp: Int
            ) -> Unit
        ) {
            val ref = db.collection("users")
                .document(userId)
                .collection("progress")
                .document(chapterId)
                .collection("subQuests")
                .document(subQuestId)

            ref.get(Source.SERVER)
                .addOnSuccessListener { snap ->
                    val data = snap.data ?: emptyMap<String, Any>()

                    val didJustCompleteDailyMission =
                        data["didJustCompleteDailyMission"] as? Boolean ?: false

                    val didJustCompleteMonthlyMission =
                        data["didJustCompleteMonthlyMission"] as? Boolean ?: false

                    val isDailyMissionCompleted =
                        data["isDailyMissionCompleted"] as? Boolean ?: false

                    val isMonthlyMissionCompleted =
                        data["isMonthlyMissionCompleted"] as? Boolean ?: false

                    val dailyMissionRewardExp = when (val raw = data["dailyMissionRewardExpGranted"]) {
                        is Int -> raw
                        is Long -> raw.toInt()
                        is Double -> raw.toInt()
                        else -> 0
                    }

                    val monthlyMissionRewardExp =
                        when (val raw = data["monthlyMissionRewardExpGranted"]) {
                            is Int -> raw
                            is Long -> raw.toInt()
                            is Double -> raw.toInt()
                            else -> 0
                        }

                    completion(
                        didJustCompleteDailyMission,
                        didJustCompleteMonthlyMission,
                        isDailyMissionCompleted,
                        isMonthlyMissionCompleted,
                        dailyMissionRewardExp,
                        monthlyMissionRewardExp
                    )
                }
                .addOnFailureListener {
                    completion(false, false, false, false, 0, 0)
                }
        }

        // 챕터 보너스 필드가 "늦게 들어오는" 레이스 해결
        private fun waitForChapterBonusWrite(
            userId: String,
            chapterId: String,
            subQuestId: String,
            timeout: Double = 2.0,
            completion: (Boolean, Int) -> Unit
        ) {
            val ref = db.collection("users")
                .document(userId)
                .collection("progress")
                .document(chapterId)
                .collection("subQuests")
                .document(subQuestId)

            var done = false

            // 기존 챕터 보너스 리스너 제거(중복 등록/누수 방지)
            chapterBonusListener?.remove()
            chapterBonusListener = null

            // 타임아웃: 끝까지 안 오면 현재 값으로 진행
            mainHandler.postDelayed({
                if (done) return@postDelayed
                done = true
                chapterBonusListener?.remove()
                chapterBonusListener = null

                // 마지막으로 한번 읽고 종료
                ref.get(Source.SERVER)
                    .addOnSuccessListener { snap ->
                        val data = snap.data ?: emptyMap<String, Any>()
                        val cleared = data["chapterClearGranted"] as? Boolean ?: false
                        val bonusExp = when (val raw = data["chapterBonusExpGranted"]) {
                            is Int -> raw
                            is Long -> raw.toInt()
                            is Double -> raw.toInt()
                            else -> 0
                        }
                        completion(cleared, bonusExp)
                    }
                    .addOnFailureListener {
                        completion(false, 0)
                    }
            }, (timeout * 1000).toLong())

            // 리스너로 “필드가 생기는 순간”을 기다림
            chapterBonusListener = ref.addSnapshotListener { snap, _ ->
                if (done) return@addSnapshotListener
                val data = snap?.data ?: emptyMap<String, Any>()

                val cleared = data["chapterClearGranted"] as? Boolean ?: false
                val bonusExp = when (val raw = data["chapterBonusExpGranted"]) {
                    is Int -> raw
                    is Long -> raw.toInt()
                    is Double -> raw.toInt()
                    else -> 0
                }

                // cleared가 true이거나 bonusExp가 0보다 커지면 “보너스 준비 완료”
                if (cleared || bonusExp > 0) {
                    done = true
                    chapterBonusListener?.remove()
                    chapterBonusListener = null
                    completion(cleared, bonusExp)
                }
            }
        }
    */

    // MARK: - 퀘스트 클리어 처리
    /*
        private fun handleQuestClear(subQuest: SubQuestDocument, usedBlocks: Int) {
            // 보상 정산 시작 오버레이 ON
            beginRewardLoading()

            val baseExp = subQuest.rewards.baseExp
            val bonusExp = subQuest.rewards.perfectBonusExp
            val maxSteps = subQuest.rules.maxSteps

            val isPerfect = usedBlocks <= maxSteps
            val earned = if (isPerfect) (baseExp + bonusExp) else baseExp

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val subId = currentSubQuestId
            if (subId.isEmpty()) return

            val progressRef = db.collection("users")
                .document(userId)
                .collection("progress")
                .document(currentChapterId)
                .collection("subQuests")
                .document(subId)

            val userRef = db.collection("users").document(userId)

            // 재도전(이미 completed)면 level/exp 변화가 없으니 기다리면 타임아웃이 정상
            progressRef.get(Source.SERVER)
                .addOnSuccessListener { progressSnap ->
                    val prevState = progressSnap.getString("state") ?: "locked"

                    // =================================================
                    // 이미 완료된 퀘스트 재도전 케이스
                    // =================================================
                    if (prevState == "completed") {
                        progressRef.update(
                            mapOf(
                                "attempts" to com.google.firebase.firestore.FieldValue.increment(1),
                                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                        )

                        // users는 "현재 값"만 읽어서 reward 구성
                        userRef.get(Source.SERVER)
                            .addOnSuccessListener { userSnap ->
                                val data = userSnap.data ?: return@addOnSuccessListener

                                val level = (data["level"] as? Number)?.toInt() ?: 1
                                val exp = (data["exp"] as? Number)?.toDouble() ?: 0.0
                                val maxExp = maxExpForLevel(level)

                                // 챕터 보너스 정보도 함께 읽어오기
                                fetchChapterBonusInfo(
                                    userId = userId,
                                    chapterId = currentChapterId,
                                    subQuestId = subId
                                ) { isCleared, chapterBonus ->

                                    println(
                                        "🟢 fetchChapterBonusInfo 결과 isCleared: $isCleared bonus: $chapterBonus chapter: $currentChapterId subId: $subId"
                                    )

                                    // 미션 결과도 함께 읽기
                                    fetchMissionResultInfo(
                                        userId = userId,
                                        chapterId = currentChapterId,
                                        subQuestId = subId
                                    ) { didJustDaily, didJustMonthly, isDailyCompleted, isMonthlyCompleted, dailyMissionRewardExp, monthlyMissionRewardExp ->
                                        mainHandler.post {
                                            successReward = SuccessReward(
                                                level = level,
                                                currentExp = exp.toFloat(),
                                                maxExp = maxExp.toFloat(),
                                                gainedExp = 0,
                                                isPerfectClear = false,
                                                chapterBonusExp = chapterBonus,
                                                isChapterCleared = isCleared,
                                                didJustCompleteDailyMission = didJustDaily,
                                                didJustCompleteMonthlyMission = didJustMonthly,
                                                isDailyMissionCompleted = isDailyCompleted,
                                                isMonthlyMissionCompleted = isMonthlyCompleted,
                                                dailyMissionRewardExp = dailyMissionRewardExp,
                                                monthlyMissionRewardExp = monthlyMissionRewardExp
                                            )
                                        }

                                        // 최소 표시시간 보장 후 오버레이 OFF → 성공 다이얼로그 ON
                                        endRewardLoadingAndShowSuccess {
                                            showSuccessDialog = true
                                        }
                                    }
                                }
                            }

                        return@addOnSuccessListener
                    }

                    // =================================================
                    // 처음 완료(보상 지급) 케이스
                    // =================================================

                    // 0) 현재 level/exp를 먼저 읽어둠 (변경 감지 기준)
                    userRef.get()
                        .addOnSuccessListener { userSnap ->
                            val prevLevel = (userSnap.data?.get("level") as? Number)?.toInt() ?: 1
                            val prevExp = (userSnap.data?.get("exp") as? Number)?.toDouble() ?: 0.0

                            // 1) progress 업데이트 (Cloud Function 트리거)
                            progressRef.update(
                                mapOf(
                                    "earnedExp" to earned,
                                    "perfectClear" to isPerfect,
                                    "state" to "completed",
                                    "attempts" to com.google.firebase.firestore.FieldValue.increment(1),
                                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )
                            )

                            ReviewManager.recordSubQuestCompletion()

                            // 2) users 문서가 실제로 갱신될 때까지 기다렸다가 reward 생성
                            waitForUserUpdate(
                                userRef = userRef,
                                previousLevel = prevLevel,
                                previousExp = prevExp,
                                timeout = 6.0,
                                completion = { level, exp ->
                                    val afterSubquestLevel = level
                                    val afterSubquestExp = exp

                                    // 보너스 필드가 써질 때까지 잠깐 기다림
                                    waitForChapterBonusWrite(
                                        userId = userId,
                                        chapterId = currentChapterId,
                                        subQuestId = subId,
                                        timeout = 2.0
                                    ) { isCleared, chapterBonus ->

                                        println(
                                            "🟣 waitForChapterBonusWrite 결과 isCleared: $isCleared bonus: $chapterBonus"
                                        )

                                        // 보너스가 없으면(또는 cleared 아님) 그냥 1단계 값으로 표시
                                        if (!isCleared || chapterBonus <= 0) {
                                            val maxExp = maxExpForLevel(afterSubquestLevel)

                                            fetchMissionResultInfo(
                                                userId = userId,
                                                chapterId = currentChapterId,
                                                subQuestId = subId
                                            ) { didJustDaily, didJustMonthly, isDailyCompleted, isMonthlyCompleted, dailyMissionRewardExp, monthlyMissionRewardExp ->
                                                mainHandler.post {
                                                    successReward = SuccessReward(
                                                        level = afterSubquestLevel,
                                                        currentExp = afterSubquestExp.toFloat(),
                                                        maxExp = maxExp.toFloat(),
                                                        gainedExp = earned,
                                                        isPerfectClear = isPerfect,
                                                        chapterBonusExp = 0,
                                                        isChapterCleared = false,
                                                        didJustCompleteDailyMission = didJustDaily,
                                                        didJustCompleteMonthlyMission = didJustMonthly,
                                                        isDailyMissionCompleted = isDailyCompleted,
                                                        isMonthlyMissionCompleted = isMonthlyCompleted,
                                                        dailyMissionRewardExp = dailyMissionRewardExp,
                                                        monthlyMissionRewardExp = monthlyMissionRewardExp
                                                    )
                                                }
                                                endRewardLoadingAndShowSuccess {
                                                    showSuccessDialog = true
                                                }
                                            }
                                            return@waitForChapterBonusWrite
                                        }

                                        // 보너스가 users에 반영될 때까지 "한 번 더" users 업데이트를 기다림
                                        waitForUserUpdate(
                                            userRef = userRef,
                                            previousLevel = afterSubquestLevel,
                                            previousExp = afterSubquestExp,
                                            timeout = 2.5,
                                            completion = { finalLevel, finalExp ->
                                                val maxExp = maxExpForLevel(finalLevel)

                                                fetchMissionResultInfo(
                                                    userId = userId,
                                                    chapterId = currentChapterId,
                                                    subQuestId = subId
                                                ) { didJustDaily, didJustMonthly, isDailyCompleted, isMonthlyCompleted, dailyMissionRewardExp, monthlyMissionRewardExp ->
                                                    mainHandler.post {
                                                        successReward = SuccessReward(
                                                            level = finalLevel,
                                                            currentExp = finalExp.toFloat(),
                                                            maxExp = maxExp.toFloat(),
                                                            gainedExp = earned,
                                                            isPerfectClear = isPerfect,
                                                            chapterBonusExp = chapterBonus,
                                                            isChapterCleared = true,
                                                            didJustCompleteDailyMission = didJustDaily,
                                                            didJustCompleteMonthlyMission = didJustMonthly,
                                                            isDailyMissionCompleted = isDailyCompleted,
                                                            isMonthlyMissionCompleted = isMonthlyCompleted,
                                                            dailyMissionRewardExp = dailyMissionRewardExp,
                                                            monthlyMissionRewardExp = monthlyMissionRewardExp
                                                        )
                                                    }

                                                    endRewardLoadingAndShowSuccess {
                                                        showSuccessDialog = true
                                                    }
                                                }
                                            },
                                            onTimeout = {
                                                val applied = applyGainLocally(
                                                    level = afterSubquestLevel,
                                                    exp = afterSubquestExp,
                                                    gain = chapterBonus
                                                )
                                                val maxExp = maxExpForLevel(applied.first)

                                                println(
                                                    "🟠 users 보너스 반영 대기 timeout → 로컬 보정 적용 level: ${applied.first} exp: ${applied.second} bonus: $chapterBonus"
                                                )

                                                fetchMissionResultInfo(
                                                    userId = userId,
                                                    chapterId = currentChapterId,
                                                    subQuestId = subId
                                                ) { didJustDaily, didJustMonthly, isDailyCompleted, isMonthlyCompleted, dailyMissionRewardExp, monthlyMissionRewardExp ->
                                                    mainHandler.post {
                                                        successReward = SuccessReward(
                                                            level = applied.first,
                                                            currentExp = applied.second.toFloat(),
                                                            maxExp = maxExp.toFloat(),
                                                            gainedExp = earned,
                                                            isPerfectClear = isPerfect,
                                                            chapterBonusExp = chapterBonus,
                                                            isChapterCleared = true,
                                                            didJustCompleteDailyMission = didJustDaily,
                                                            didJustCompleteMonthlyMission = didJustMonthly,
                                                            isDailyMissionCompleted = isDailyCompleted,
                                                            isMonthlyMissionCompleted = isMonthlyCompleted,
                                                            dailyMissionRewardExp = dailyMissionRewardExp,
                                                            monthlyMissionRewardExp = monthlyMissionRewardExp
                                                        )
                                                    }

                                                    endRewardLoadingAndShowSuccess {
                                                        showSuccessDialog = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                },
                                onTimeout = {
                                    println("⚠️ users update wait timeout → fallback getDocument")

                                    userRef.get(Source.SERVER)
                                        .addOnSuccessListener { snap ->
                                            val data = snap.data ?: return@addOnSuccessListener
                                            val level = (data["level"] as? Number)?.toInt() ?: 1
                                            val exp = (data["exp"] as? Number)?.toDouble() ?: 0.0
                                            val maxExp = maxExpForLevel(level)

                                            // timeout fallback에서도 챕터 보너스 정보 읽기
                                            fetchChapterBonusInfo(
                                                userId = userId,
                                                chapterId = currentChapterId,
                                                subQuestId = subId
                                            ) { isCleared, chapterBonus ->

                                                // 미션 결과도 함께 읽기
                                                fetchMissionResultInfo(
                                                    userId = userId,
                                                    chapterId = currentChapterId,
                                                    subQuestId = subId
                                                ) { didJustDaily, didJustMonthly, isDailyCompleted, isMonthlyCompleted, dailyMissionRewardExp, monthlyMissionRewardExp ->
                                                    mainHandler.post {
                                                        successReward = SuccessReward(
                                                            level = level,
                                                            currentExp = exp.toFloat(),
                                                            maxExp = maxExp.toFloat(),
                                                            gainedExp = earned,
                                                            isPerfectClear = isPerfect,
                                                            chapterBonusExp = chapterBonus,
                                                            isChapterCleared = isCleared,
                                                            didJustCompleteDailyMission = didJustDaily,
                                                            didJustCompleteMonthlyMission = didJustMonthly,
                                                            isDailyMissionCompleted = isDailyCompleted,
                                                            isMonthlyMissionCompleted = isMonthlyCompleted,
                                                            dailyMissionRewardExp = dailyMissionRewardExp,
                                                            monthlyMissionRewardExp = monthlyMissionRewardExp
                                                        )
                                                    }

                                                    // 최소 표시시간 보장 후 오버레이 OFF → 성공 다이얼로그 ON
                                                    endRewardLoadingAndShowSuccess {
                                                        showSuccessDialog = true
                                                    }
                                                }
                                            }
                                        }
                                }
                            )
                        }
                }
                .addOnFailureListener {
                    isRewardLoading = false
                }
        }
    */

    private fun countUsedBlocks(): Int {
        fun dfs(blocks: List<Block>): Int {
            var total = 0
            for (b in blocks) {
                total += 1
                if (b.type.isContainer) {
                    total += dfs(b.children)
                }
            }
            return total
        }
        return dfs(startBlock.children)
    }

    // MARK: - 앞으로 이동
    fun moveForward(completion: () -> Unit) {
        var newRow = characterPosition.row
        var newCol = characterPosition.col

        when (characterDirection) {
            Direction.UP -> newRow -= 1
            Direction.DOWN -> newRow += 1
            Direction.LEFT -> newCol -= 1
            Direction.RIGHT -> newCol += 1
        }

        // 1) 범위 체크
        if (newRow !in mapData.indices || mapData.isEmpty() || newCol !in mapData[0].indices) {
            println("이동 실패: 범위 밖입니다.")
            resetToStart()
            return
        }

        // 2) 벽(0) 체크
        if (mapData[newRow][newCol] == 0) {
            println("이동 실패: 벽입니다.")
            resetToStart()
            return
        }

        // 3) 적 충돌 체크 (부딪히면 실패)
        /*
                val hitEnemy = enemies.any { it.row == newRow && it.col == newCol }
                if (hitEnemy) {
                    println("💥 실패: 적과 충돌했습니다. ($newRow, $newCol)")
                    resetToStart()
                    return
                }
        */

        // 4) 이동 성공
        characterPosition = GridPosition(newRow, newCol)
        println("캐릭터 이동 → 위치: ($newRow, $newCol)")
        completion()
    }

    // MARK: - 공격 처리 (가장 가까운 1명 처치)
    /*
        fun attack(completion: () -> Unit) {
            val target = enemyInAttackRange()
            if (target == null) {
                println("공격: 범위 내 적 없음")
                completion()
                return
            }

            // 현재는 '처치' = enemies에서 제거
            enemies = enemies.filterNot { it.id == target.id }
            println("적 처치 성공: ${target.id} at (${target.row}, ${target.col})")

            completion()
        }

        // MARK: - 공격 범위 내 적 찾기 (가장 가까운 1명)
        fun enemyInAttackRange(): Enemy? {
            val currentSubQuest = subQuest ?: return null
            val range = max(0, currentSubQuest.rules.attackRange)
            if (range == 0) return null

            val row = characterPosition.row
            val col = characterPosition.col

            for (step in 1..range) {
                var targetRow = row
                var targetCol = col

                when (characterDirection) {
                    Direction.UP -> targetRow -= step
                    Direction.DOWN -> targetRow += step
                    Direction.LEFT -> targetCol -= step
                    Direction.RIGHT -> targetCol += step
                }

                val enemy = enemies.firstOrNull { it.row == targetRow && it.col == targetCol }
                if (enemy != null) {
                    return enemy
                }
            }
            return null
        }
    */

    // MARK: - 실패 시 초기화
    fun resetToStart() {
        mainHandler.post {
            // 실패도 "현재 실행 세션" 무효화(겹침 방지)
            executionToken = UUID.randomUUID()
            didStopExecution = false

            didFailExecution = true
            isExecuting = false
            currentExecutingBlockID = null
            characterPosition = startPosition
            characterDirection = startDirection
//            enemies = initialEnemies
            showFailureDialog = true
            println("🔁 캐릭터를 시작 위치로 되돌림")
        }
    }

    fun resetExecution() {
        // reset도 세션 무효화(겹침 방지)
        executionToken = UUID.randomUUID()
        didStopExecution = false

        didFailExecution = false
        isExecuting = false
        currentExecutingBlockID = null
        characterPosition = startPosition
        characterDirection = startDirection
//        enemies = initialEnemies

        println("🔄 다시하기: 캐릭터 초기화 및 다이얼로그 종료")
    }

    fun previewConfigure(
        map: List<List<Int>>,
        start: GridPosition,
        goal: GridPosition,
        direction: Direction = Direction.RIGHT
    ) {
        mapData = map
        startPosition = start
        goalPosition = goal
        characterPosition = start
        characterDirection = direction
    }

    // MARK: - Story / Hint (UI 전용 접근자)
    /*
        val storyMessage: String?
            get() {
                val story = subQuest?.story
                return if (story != null && story.isActive) story.message else null
            }

        val hintMessage: String?
            get() {
                val hint = subQuest?.hint
                return if (hint != null && hint.isActive) hint.message else null
            }
    */
}