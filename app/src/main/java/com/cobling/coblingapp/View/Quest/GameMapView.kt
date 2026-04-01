// 맵 / 캐릭터 / 깃발 표시 영역

//
//  GameMapView.kt
//  Cobling
//
//  Created by 박종민 on 2025/07/02.
//

package com.cobling.app.view.quest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cobling.coblingapp.R
import com.cobling.app.viewmodel.QuestViewModel
import kotlin.math.roundToInt

@Composable
fun GameMapView(
    viewModel: QuestViewModel,
    questTitle: String,
    subQuestId: String, // 서브퀘스트 변경 감지용

    // MARK: - Tutorial Highlight Frames
    storyButtonFrame: Rect,
    onStoryButtonFrameChange: (Rect) -> Unit,
    playButtonFrame: Rect,
    onPlayButtonFrameChange: (Rect) -> Unit,
    stopButtonFrame: Rect,
    onStopButtonFrameChange: (Rect) -> Unit,
    flagFrame: Rect,
    onFlagFrameChange: (Rect) -> Unit,

    tabBarViewModel: TabBarViewModel,
    authVM: AuthViewModel,
    appState: AppState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHintOn by remember { mutableStateOf(false) }
    var isStoryOn by remember { mutableStateOf(false) }

    // 타입 체크 부담을 줄이기 위해 타일 크기를 프로퍼티로 분리
    val tileSize = 36.dp
    val density = LocalDensity.current

    // mapData 접근을 분리해서 body 내부 복잡도 감소
    val map = viewModel.mapData

    // DB stage → 에셋 prefix (게임 캐릭터용)
    val gameCharacterAssetPrefix = run {
        val stage = (authVM.userProfile?.character?.stage ?: "egg")
            .trim()
            .lowercase()

        val allowed = setOf("egg", "kid", "cobling", "legend")
        val safeStage = if (allowed.contains(stage)) stage else "egg"

        "cobling_stage_$safeStage"
    }

    // 방향 → suffix 매핑
    // up    -> back
    // down  -> front
    // left  -> left
    // right -> right
    val directionSuffix = when (viewModel.characterDirection) {
        CharacterDirection.UP -> "back"
        CharacterDirection.DOWN -> "front"
        CharacterDirection.LEFT -> "left"
        CharacterDirection.RIGHT -> "right"
    }

    // 최종 캐릭터 에셋 이름
    // ex) "cobling_stage_kid_front"
    val gameCharacterDirectionalAssetName = "${gameCharacterAssetPrefix}_$directionSuffix"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        backgroundView()

        mainContentView(
            questTitle = questTitle,
            tileSize = tileSize,
            map = map,
            viewModel = viewModel,
            gameCharacterDirectionalAssetName = gameCharacterDirectionalAssetName,
            onPlayButtonFrameChange = onPlayButtonFrameChange,
            onStopButtonFrameChange = onStopButtonFrameChange,
            onFlagFrameChange = onFlagFrameChange,
            appState = appState,
            onDismiss = onDismiss
        )

//        storyOverlayView(
//            isStoryOn = isStoryOn,
//            onStoryToggle = { isStoryOn = !isStoryOn },
//            message = viewModel.storyMessage,
//            onStoryButtonFrameChange = onStoryButtonFrameChange
//        )
//
//        hintOverlayView(
//            isHintOn = isHintOn,
//            message = viewModel.hintMessage
//        )
    }

    LaunchedEffect(Unit) {
        refreshTutorialFrames()
    }

    LaunchedEffect(viewModel.characterPosition.row) {
        refreshTutorialFrames()
    }

    LaunchedEffect(viewModel.characterPosition.col) {
        refreshTutorialFrames()
    }

    LaunchedEffect(viewModel.characterDirection) {
        refreshTutorialFrames()
    }

    LaunchedEffect(subQuestId) {
        isStoryOn = false
        isHintOn = false
    }
}

// MARK: - Background

// 배경 뷰 분리
@Composable
private fun backgroundView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color+Hex("#FFF2DC"))
    )
}

// MARK: - Main Content

// 메인 전체 레이아웃 분리
@Composable
private fun mainContentView(
    questTitle: String,
    tileSize: androidx.compose.ui.unit.Dp,
    map: List<List<Int>>,
    viewModel: QuestViewModel,
    gameCharacterDirectionalAssetName: String,
    onPlayButtonFrameChange: (Rect) -> Unit,
    onStopButtonFrameChange: (Rect) -> Unit,
    onFlagFrameChange: (Rect) -> Unit,
    appState: AppState,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(modifier = Modifier.height(42.dp))

        titleView(questTitle = questTitle)

        topButtonBarView(
            viewModel = viewModel,
            onPlayButtonFrameChange = onPlayButtonFrameChange,
            onStopButtonFrameChange = onStopButtonFrameChange,
            appState = appState,
            onDismiss = onDismiss
        )

        mapContainerView(
            tileSize = tileSize,
            map = map,
            viewModel = viewModel,
            gameCharacterDirectionalAssetName = gameCharacterDirectionalAssetName,
            onFlagFrameChange = onFlagFrameChange
        )
    }
}

// 타이틀 뷰 분리
@Composable
private fun titleView(
    questTitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        androidx.compose.material3.Text(
            text = questTitle,
            color = HexColor("#3A3A3A"),
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

// 상단 버튼 바 분리
@Composable
private fun topButtonBarView(
    viewModel: QuestViewModel,
    onPlayButtonFrameChange: (Rect) -> Unit,
    onStopButtonFrameChange: (Rect) -> Unit,
    appState: AppState,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 40.dp)
        ) {
            playButtonView(
                viewModel = viewModel,
                onPlayButtonFrameChange = onPlayButtonFrameChange
            )

            stopButtonView(
                viewModel = viewModel,
                onStopButtonFrameChange = onStopButtonFrameChange
            )

//                IconButton(
//                    onClick = {
//                        withAnimation {
//                            isHintOn.toggle()
//                        }
//                    }
//                ) {
//                    Image(
//                        painter = painterResource(
//                            id = if (isHintOn) R.drawable.gp_hint_on else R.drawable.gp_hint_off
//                        ),
//                        contentDescription = null,
//                        modifier = Modifier.size(28.dp)
//                    )
//                }
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                appState.isInGame = false
                onDismiss()
            },
            modifier = Modifier
                .padding(end = 40.dp, top = 10.dp)
                .size(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.gp_out),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// 플레이 버튼 분리
@Composable
private fun playButtonView(
    viewModel: QuestViewModel,
    onPlayButtonFrameChange: (Rect) -> Unit
) {
    IconButton(
        onClick = {
            viewModel.startExecution()
        },
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                onPlayButtonFrameChange(coordinates.boundsInWindow())
            }
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_media_play),
            contentDescription = null,
            tint = HexColor("#58ED98"),
            modifier = Modifier.size(32.dp)
        )
    }
}

// 정지 버튼 분리
@Composable
private fun stopButtonView(
    viewModel: QuestViewModel,
    onStopButtonFrameChange: (Rect) -> Unit
) {
    IconButton(
        onClick = {
            viewModel.stopExecution()
        },
        modifier = Modifier
            .size(28.dp)
            .onGloballyPositioned { coordinates ->
                onStopButtonFrameChange(coordinates.boundsInWindow())
            }
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_media_pause),
            contentDescription = null,
            tint = HexColor("#E85A5A"),
            modifier = Modifier.size(32.dp)
        )
    }
}

// MARK: - Map

// 맵 전체 컨테이너 분리
@Composable
private fun mapContainerView(
    tileSize: androidx.compose.ui.unit.Dp,
    map: List<List<Int>>,
    viewModel: QuestViewModel,
    gameCharacterDirectionalAssetName: String,
    onFlagFrameChange: (Rect) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        mapTilesView(
            tileSize = tileSize,
            map = map,
            viewModel = viewModel,
            onFlagFrameChange = onFlagFrameChange
        )

        characterLayerView(
            tileSize = tileSize,
            map = map,
            viewModel = viewModel,
            gameCharacterDirectionalAssetName = gameCharacterDirectionalAssetName
        )
    }
}

// 맵 타일 전체 뷰 분리
@Composable
private fun mapTilesView(
    tileSize: androidx.compose.ui.unit.Dp,
    map: List<List<Int>>,
    viewModel: QuestViewModel,
    onFlagFrameChange: (Rect) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        map.indices.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                map[row].indices.forEach { col ->
                    mapCellView(
                        row = row,
                        col = col,
                        tileSize = tileSize,
                        map = map,
                        viewModel = viewModel,
                        onFlagFrameChange = onFlagFrameChange
                    )
                }
            }
        }
    }
}

// 셀 단위 렌더링 분리
@Composable
private fun mapCellView(
    row: Int,
    col: Int,
    tileSize: androidx.compose.ui.unit.Dp,
    map: List<List<Int>>,
    viewModel: QuestViewModel,
    onFlagFrameChange: (Rect) -> Unit
) {
    Box(
        modifier = Modifier.size(tileSize),
        contentAlignment = Alignment.Center
    ) {
        tileImageView(
            row = row,
            col = col,
            tileSize = tileSize,
            map = map
        )

        enemyImageView(
            row = row,
            col = col,
            tileSize = tileSize,
            viewModel = viewModel
        )

        flagImageView(
            row = row,
            col = col,
            tileSize = tileSize,
            viewModel = viewModel,
            onFlagFrameChange = onFlagFrameChange
        )
    }
}

// 타일 이미지 분리
@Composable
private fun tileImageView(
    row: Int,
    col: Int,
    tileSize: androidx.compose.ui.unit.Dp,
    map: List<List<Int>>
) {
    if (isWalkableTile(row = row, col = col, map = map)) {
        Image(
            painter = painterResource(id = R.drawable.iv_game_way_1),
            contentDescription = null,
            modifier = Modifier.size(tileSize)
        )
    }
}

// 적 이미지 분리
@Composable
private fun enemyImageView(
    row: Int,
    col: Int,
    tileSize: androidx.compose.ui.unit.Dp,
    viewModel: QuestViewModel
) {
    if (hasEnemy(row = row, col = col, viewModel = viewModel)) {
        Image(
            painter = painterResource(id = R.drawable.cobling_character_enemies),
            contentDescription = null,
            modifier = Modifier
                .size(tileSize * 1.4f)
                .offset(y = (-8).dp)
        )
    }
}

// 깃발 이미지 분리
@Composable
private fun flagImageView(
    row: Int,
    col: Int,
    tileSize: androidx.compose.ui.unit.Dp,
    viewModel: QuestViewModel,
    onFlagFrameChange: (Rect) -> Unit
) {
    if (isGoalPosition(row = row, col = col, viewModel = viewModel)) {
        Image(
            painter = painterResource(id = R.drawable.gp_flag),
            contentDescription = null,
            modifier = Modifier
                .size(width = 36.dp, height = 36.dp)
                .offset(y = (-15).dp)
                .onGloballyPositioned { coordinates ->
                    onFlagFrameChange(coordinates.boundsInWindow())
                }
        )
    }
}

// 캐릭터 레이어 분리
@Composable
private fun characterLayerView(
    tileSize: androidx.compose.ui.unit.Dp,
    map: List<List<Int>>,
    viewModel: QuestViewModel,
    gameCharacterDirectionalAssetName: String
) {
    val density = LocalDensity.current
    val tileSizePx = with(density) { tileSize.toPx() }

    val x = viewModel.characterPosition.col * tileSizePx + tileSizePx / 2f
    val y = viewModel.characterPosition.row * tileSizePx + tileSizePx / 2f

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 120),
        label = "characterDirectionAnimation"
    )

    Box(
        modifier = Modifier
            .width((map.firstOrNull()?.size ?: 0).let { count -> tileSize * count })
            .height(map.size.let { count -> tileSize * count })
    ) {
        Image(
            painter = painterResource(id = getDrawableResId(gameCharacterDirectionalAssetName)),
            contentDescription = null,
            modifier = Modifier
                .size(tileSize * 1.4f)
                .offset {
                    IntOffset(
                        x = (x - with(density) { (tileSize * 0.7f).toPx() }).roundToInt(),
                        y = (y - 15.dp.toPx() - with(density) { (tileSize * 0.7f).toPx() }).roundToInt()
                    )
                }
        )
    }
}

// MARK: - Story Overlay

// 스토리 오버레이 분리
@Composable
private fun storyOverlayView(
    isStoryOn: Boolean,
    onStoryToggle: () -> Unit,
    message: String?,
    onStoryButtonFrameChange: (Rect) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.padding(end = 30.dp)
            ) {
                AnimatedVisibility(
                    visible = isStoryOn && message != null
                ) {
                    SpeechBubbleView(
                        message = message ?: "",
                        modifier = Modifier.padding(end = 50.dp)
                    )
                }

                IconButton(
                    onClick = onStoryToggle,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        onStoryButtonFrameChange(coordinates.boundsInWindow())
                    }
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isStoryOn) R.drawable.gp_story_btn_on else R.drawable.gp_story_btn_off
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

// MARK: - Hint Overlay

// 힌트 오버레이 분리
@Composable
private fun hintOverlayView(
    isHintOn: Boolean,
    message: String?
) {
    if (isHintOn && message != null) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(160.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(80.dp))

                SpeechBubbleView(
                    message = message,
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// MARK: - Helper

// 길 타일 판별 함수 분리
private fun isWalkableTile(
    row: Int,
    col: Int,
    map: List<List<Int>>
): Boolean {
    if (row >= map.size || col >= map[row].size) return false
    val value = map[row][col]
    return value == 1 || value == 2
}

// 적 존재 여부 판별 함수 분리
private fun hasEnemy(
    row: Int,
    col: Int,
    viewModel: QuestViewModel
): Boolean {
    return viewModel.enemies.any { enemy ->
        enemy.row == row && enemy.col == col
    }
}

// 목표 위치 판별 함수 분리
private fun isGoalPosition(
    row: Int,
    col: Int,
    viewModel: QuestViewModel
): Boolean {
    return viewModel.goalPosition.row == row && viewModel.goalPosition.col == col
}

// MARK: - Tutorial Frame Refresh
private suspend fun refreshTutorialFrames() {
    kotlinx.coroutines.delay(10)
    // 각 onGloballyPositioned에서 개별 반영되므로
    // 여기서는 레이아웃 갱신 타이밍을 한 번 더 보정하는 용도입니다.
}