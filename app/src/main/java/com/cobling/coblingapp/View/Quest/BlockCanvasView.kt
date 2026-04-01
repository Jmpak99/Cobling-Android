// 블록 조립 캔버스 영역
//
//  BlockCanvasView.kt
//  Cobling
//

package com.cobling.app.view.quest

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.util.UUID
import com.cobling.app.model.Block
import com.cobling.app.viewmodel.DragManager
import com.cobling.app.viewmodel.QuestViewModel

// MARK: - Drop Indicator Bar
@Composable
fun DropIndicatorBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                color = androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
            )
    )
}

// MARK: - Block Canvas View
@Composable
fun BlockCanvasView(
    dragManager: DragManager,
    viewModel: QuestViewModel,
    paletteFrame: Rect,
    modifier: Modifier = Modifier
) {
    var canvasFrame by remember { mutableStateOf(Rect.Zero) }

    var isDropTarget by remember { mutableStateOf(false) }
    var previousChildCount by remember { mutableIntStateOf(0) }

    val blockFrames = remember { mutableStateMapOf<UUID, Rect>() }
    var insertIndex by remember { mutableStateOf<Int?>(null) }

    // ✅ StartBlock 하위 들여쓰기 값
    val childIndent = 20.dp
    val childBlockWidth = 120.dp

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .onGloballyPositioned { coordinates ->
                canvasFrame = coordinates.boundsInWindow()
            }
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(bottom = 100.dp)
                .padding(start = 10.dp)
                .fillMaxWidth()
        ) {

            // Start Block
            BlockView(
                block = viewModel.startBlock,
                parentContainer = null,
                dragManager = dragManager,
                viewModel = viewModel
            )

            // 실행 블록
            viewModel.startBlock.children.forEachIndexed { index, block ->

                // 중간 삽입 인디케이터
                if (
                    dragManager.isDragging &&
                    dragManager.containerTargetBlock == null &&
                    insertIndex == index
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Spacer(modifier = Modifier.width(childIndent))

                        Box(modifier = Modifier.width(childBlockWidth)) {
                            DropIndicatorBar()
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(start = childIndent)
                        .onGloballyPositioned { coordinates ->
                            blockFrames[block.id] = coordinates.boundsInWindow()
                        }
                ) {
                    BlockView(
                        block = block,
                        parentContainer = null,
                        dragManager = dragManager,
                        viewModel = viewModel
                    )
                }
            }

            // 마지막 위치 인디케이터
            if (
                dragManager.isDragging &&
                dragManager.containerTargetBlock == null &&
                insertIndex == viewModel.startBlock.children.size
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Spacer(modifier = Modifier.width(childIndent))

                    Box(modifier = Modifier.width(childBlockWidth)) {
                        DropIndicatorBar()
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Box(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
        }
    }

    // ⭐ 핵심 수정 부분
    // ✅ 캔버스 드롭 타겟 판정 + 삽입 인덱스 계산
    LaunchedEffect(
        dragManager.isDragging,
        dragManager.dragPosition,
        dragManager.containerTargetBlock,
        canvasFrame,
        blockFrames.size,
        viewModel.startBlock.children.size
    ) {
        if (!dragManager.isDragging) {
            insertIndex = null
            dragManager.canvasInsertIndex = null
            dragManager.isOverCanvas = false
            isDropTarget = false
            return@LaunchedEffect
        }

        // ✅ 컨테이너가 활성화된 상태면, 캔버스는 드롭 타겟이 되면 안 됨
        if (dragManager.containerTargetBlock != null) {
            dragManager.isOverCanvas = false
            insertIndex = null
            dragManager.canvasInsertIndex = null
            isDropTarget = false
            return@LaunchedEffect
        }

        val globalPos = dragManager.dragPosition

        if (canvasFrame.contains(globalPos)) {
            dragManager.isOverCanvas = true
            isDropTarget = true

            // ✅ 삽입 위치 계산
            val idx = calculateInsertIndex(
                dragY = globalPos.y,
                blocks = viewModel.startBlock.children,
                blockFrames = blockFrames
            )

            insertIndex = idx
            dragManager.canvasInsertIndex = idx
        } else {
            dragManager.isOverCanvas = false
            isDropTarget = false
            insertIndex = null
            dragManager.canvasInsertIndex = null
        }
    }

    // 자동 스크롤
    LaunchedEffect(viewModel.startBlock.children.size) {
        val newCount = viewModel.startBlock.children.size
        if (newCount > previousChildCount) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        previousChildCount = newCount
    }

    LaunchedEffect(Unit) {
        previousChildCount = viewModel.startBlock.children.size
    }
}

private fun calculateInsertIndex(
    dragY: Float,
    blocks: List<Block>,
    blockFrames: Map<UUID, Rect>
): Int {
    for ((index, block) in blocks.withIndex()) {
        val frame = blockFrames[block.id] ?: continue
        if (dragY < frame.center.y) {
            return index
        }
    }
    return blocks.size
}