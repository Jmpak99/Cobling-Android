package com.cobling.app.view.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cobling.app.model.Block
import com.cobling.app.model.BlockType
import com.cobling.app.model.IfCondition
import com.cobling.app.viewmodel.DragManager
import com.cobling.app.viewmodel.QuestViewModel
import java.util.UUID

@Composable
fun ContainerBlockView(
    block: Block,
    dragManager: DragManager,
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val blockFrames = remember { mutableStateMapOf<UUID, Rect>() }
    var insertIndex by remember { mutableStateOf<Int?>(null) }

    val blockWidth = 165.dp
    val leftBarWidth = 12.dp

    // MARK: - 실행 중인 반복문인지 판별
    val isExecutingThisContainer = viewModel.currentExecutingBlockID == block.id

    // MARK: - NormalBlockView와 동일한 opacity 규칙
    val containerContentOpacity = if (viewModel.isExecuting && !isExecutingThisContainer) {
        0.3f
    } else {
        1.0f
    }

    // 컨테이너 타입에 따라 색상 분기 (repeat / if / ifElse)
    val containerTint = when (block.type) {
        BlockType.REPEAT_COUNT, BlockType.REPEAT_FOREVER -> Color(0xFF86B0FF)
        BlockType.IF, BlockType.IF_ELSE -> Color(0xFF4CCB7A)
        else -> Color(0xFF86B0FF)
    }

    // 컨테이너 타입에 따라 빈 안내문구 분기
    val emptyGuideText = when (block.type) {
        BlockType.REPEAT_COUNT, BlockType.REPEAT_FOREVER -> "여기에 블록을 넣어주세요"
        BlockType.IF -> "조건이 맞으면 실행할 블록을 넣어주세요"
        BlockType.IF_ELSE -> "조건이 맞으면 실행할 블록을 넣어주세요"
        else -> "여기에 블록을 넣어주세요"
    }

    var containerBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var contentBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val currentDragPosition by rememberUpdatedState(dragManager.dragPosition)

    // =========================
    // ⭐ 반복문 내부 frame 변화 반영 + 드래그 위치에 따른 삽입 인덱스 계산
    // =========================
    LaunchedEffect(
        currentDragPosition,
        dragManager.isDragging,
        dragManager.containerTargetBlock?.id,
        block.children.size,
        contentBoundsInRoot,
        blockFrames.size
    ) {
        val globalPos = currentDragPosition
        val frame = contentBoundsInRoot

        if (
            frame == null ||
            globalPos == null ||
            !frame.contains(globalPos) ||
            !dragManager.isDragging ||
            dragManager.containerTargetBlock?.id != block.id
        ) {
            insertIndex = null
            dragManager.containerInsertIndex = null
        } else {
            val localY = globalPos.y - frame.top
            val idx = calculateInsertIndex(
                dragY = localY,
                children = block.children,
                blockFrames = blockFrames,
                contentTop = frame.top
            )

            insertIndex = idx
            dragManager.containerInsertIndex = idx
        }
    }

    Row(
        modifier = modifier
            .padding(bottom = 2.dp)
            .onGloballyPositioned { coordinates ->
                containerBoundsInRoot = coordinates.boundsInRoot()
            },
        verticalAlignment = Alignment.Top
    ) {
        // =========================
        // 왼쪽 세로 바
        // =========================
        Box(
            modifier = Modifier
                .width(leftBarWidth)
                .background(
                    color = containerTint,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        bottomStart = 12.dp
                    )
                )
                .height(
                    1.dp // 실제 높이는 부모 Column에 맞춰짐
                )
                .fillMaxWidth(0f)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // =========================
            // 반복문 헤더
            // =========================
            Box(
                modifier = Modifier
                    .width(blockWidth)
                    .height(36.dp)
                    .onGloballyPositioned { geo ->
                        // header 좌표는 드래그 시작/종료 계산에 사용
                    }
                    .background(
                        color = containerTint,
                        shape = RoundedCornerShape(
                            topEnd = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .scale(if (isExecutingThisContainer) 1.05f else 1.0f)
                    .alpha(containerContentOpacity)
                    .pointerInput(block.id, dragManager.draggingBlockID, dragManager.isDragging) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (
                                    dragManager.draggingBlockID != null &&
                                    dragManager.draggingBlockID != block.id
                                ) {
                                    return@detectDragGestures
                                }

                                val frame = containerBoundsInRoot
                                val position = if (frame != null) {
                                    Offset(
                                        x = frame.left + offset.x,
                                        y = frame.top + offset.y
                                    )
                                } else {
                                    offset
                                }

                                if (!dragManager.isDragging) {
                                    dragManager.prepareDragging(
                                        type = block.type,
                                        at = position,
                                        offset = Offset.Zero,
                                        block = block,
                                        parentContainer = viewModel.findParentContainer(block),
                                        source = DragSource.CANVAS
                                    )
                                }

                                dragManager.updateDragPosition(position)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                if (
                                    dragManager.draggingBlockID != null &&
                                    dragManager.draggingBlockID != block.id
                                ) {
                                    return@detectDragGestures
                                }

                                val current = change.position
                                val frame = containerBoundsInRoot
                                val position = if (frame != null) {
                                    Offset(
                                        x = frame.left + current.x,
                                        y = frame.top + current.y
                                    )
                                } else {
                                    current
                                }

                                if (!dragManager.isDragging) {
                                    dragManager.prepareDragging(
                                        type = block.type,
                                        at = position,
                                        offset = dragAmount,
                                        block = block,
                                        parentContainer = viewModel.findParentContainer(block),
                                        source = DragSource.CANVAS
                                    )
                                }

                                dragManager.updateDragPosition(position)
                            },
                            onDragEnd = {
                                val position = dragManager.dragPosition ?: Offset.Zero
                                dragManager.finishDrag(position) { _, _, _, _ -> }
                            },
                            onDragCancel = {
                                val position = dragManager.dragPosition ?: Offset.Zero
                                dragManager.finishDrag(position) { _, _, _, _ -> }
                            }
                        )
                    }
            ) {
                when (block.type) {
                    BlockType.REPEAT_COUNT, BlockType.REPEAT_FOREVER -> {
                        RepeatHeaderView(
                            block = block,
                            modifier = Modifier
                                .width(blockWidth)
                                .height(36.dp)
                        )
                    }

                    BlockType.IF, BlockType.IF_ELSE -> {
                        IfHeaderView(
                            block = block,
                            options = viewModel.currentAllowedIfConditions,
                            defaultCondition = viewModel.currentDefaultIfCondition,
                            modifier = Modifier
                                .width(blockWidth)
                                .height(36.dp)
                        )
                    }

                    else -> {
                        RepeatHeaderView(
                            block = block,
                            modifier = Modifier
                                .width(blockWidth)
                                .height(36.dp)
                        )
                    }
                }
            }

            // =========================
            // 반복문 내부 영역
            // =========================
            Column(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .onGloballyPositioned { coordinates ->
                        contentBoundsInRoot = coordinates.boundsInRoot()
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 블록이 하나도 없을 때
                if (block.children.isEmpty()) {
                    Text(
                        text = emptyGuideText,
                        fontWeight = FontWeight.Bold,
                        color = containerTint.copy(alpha = 0.35f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // ─────────────
                // 블록이 있을 때
                // ─────────────
                block.children.forEachIndexed { index, child ->

                    // ⭐ 중간 삽입 인디케이터
                    if (
                        dragManager.isDragging &&
                        dragManager.containerTargetBlock?.id == block.id &&
                        insertIndex == index
                    ) {
                        DropIndicatorBar(
                            modifier = Modifier
                                .width(120.dp)
                                .padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }

                    // 실제 블록
                    Box(
                        modifier = Modifier.onGloballyPositioned { geo ->
                            blockFrames[child.id] = geo.boundsInRoot()
                        }
                    ) {
                        BlockView(
                            block = child,
                            parentContainer = block,
                            dragManager = dragManager,
                            viewModel = viewModel
                        )
                    }
                }

                // 마지막 위치 인디케이터
                if (
                    dragManager.isDragging &&
                    dragManager.containerTargetBlock?.id == block.id &&
                    insertIndex == block.children.count
                ) {
                    DropIndicatorBar(
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            }

            // =========================
            // 하단 캡
            // =========================
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp)
                    .background(
                        color = containerTint,
                        shape = RoundedCornerShape(
                            topEnd = 6.dp,
                            bottomEnd = 6.dp
                        )
                    )
            )
        }
    }

    // 반복문 자체 드롭 타겟 판정
    LaunchedEffect(
        currentDragPosition,
        dragManager.isDragging,
        containerBoundsInRoot,
        dragManager.containerTargetBlock?.id
    ) {
        val globalPos = currentDragPosition
        val frame = containerBoundsInRoot

        if (globalPos == null || frame == null) return@LaunchedEffect
        if (!dragManager.isDragging) return@LaunchedEffect

        if (frame.contains(globalPos)) {
            // 기존 타겟이 없으면 바로 설정
            if (dragManager.containerTargetBlock == null) {
                dragManager.containerTargetBlock = block
                dragManager.isOverContainer = true
                dragManager.isOverCanvas = false
                return@LaunchedEffect
            }

            // 기존 타겟이 있는데,
            // 내가 더 안쪽(자식) 컨테이너라면 교체 허용
            val current = dragManager.containerTargetBlock
            if (current != null && viewModel.isDescendant(block, current)) {
                dragManager.containerTargetBlock = block
                dragManager.isOverContainer = true
                dragManager.isOverCanvas = false
            }
        } else if (dragManager.containerTargetBlock?.id == block.id) {
            // ❗️다른 더 안쪽 컨테이너가 없을 때만 해제
            if (dragManager.isOverContainer) {
                dragManager.containerTargetBlock = null
                dragManager.isOverContainer = false
                dragManager.isOverCanvas = true
            }
        }
    }
}

// MARK: - Container전용 DragGesture(중첩 대응 버전)
fun containerDragGesture(
    block: Block,
    dragManager: DragManager,
    viewModel: QuestViewModel
): suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit = {
    detectDragGestures(
        onDragStart = { position ->
            if (
                dragManager.draggingBlockID != null &&
                dragManager.draggingBlockID != block.id
            ) {
                return@detectDragGestures
            }

            if (!dragManager.isDragging) {
                dragManager.prepareDragging(
                    type = block.type,
                    at = position,
                    offset = Offset.Zero,
                    block = block,
                    parentContainer = viewModel.findParentContainer(block),
                    source = DragSource.CANVAS
                )
            }

            dragManager.updateDragPosition(position)
        },
        onDrag = { change, dragAmount ->
            change.consume()

            if (
                dragManager.draggingBlockID != null &&
                dragManager.draggingBlockID != block.id
            ) {
                return@detectDragGestures
            }

            val position = change.position

            if (!dragManager.isDragging) {
                dragManager.prepareDragging(
                    type = block.type,
                    at = position,
                    offset = dragAmount,
                    block = block,
                    parentContainer = viewModel.findParentContainer(block),
                    source = DragSource.CANVAS
                )
            }

            dragManager.updateDragPosition(position)
        },
        onDragEnd = {
            val position = dragManager.dragPosition ?: Offset.Zero
            dragManager.finishDrag(position) { _, _, _, _ -> }
        },
        onDragCancel = {
            val position = dragManager.dragPosition ?: Offset.Zero
            dragManager.finishDrag(position) { _, _, _, _ -> }
        }
    )
}

// MARK: - 반복문 내부 삽입 위치 계산
private fun calculateInsertIndex(
    dragY: Float,
    children: List<Block>,
    blockFrames: Map<UUID, Rect>,
    contentTop: Float
): Int {
    children.forEachIndexed { index, child ->
        val frame = blockFrames[child.id] ?: return@forEachIndexed
        val localMidY = frame.center.y - contentTop
        if (dragY < localMidY) {
            return index
        }
    }
    return children.count
}

// MARK: - RoundedCorner Shape
// Compose에서는 RoundedCornerShape로 대체 가능
typealias RoundedCorner = RoundedCornerShape