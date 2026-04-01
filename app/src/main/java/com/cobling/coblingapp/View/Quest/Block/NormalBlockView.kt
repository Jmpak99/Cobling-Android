package com.cobling.app.view.quest

import com.cobling.app.viewmodel.DragSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cobling.app.model.Block
import com.cobling.app.model.BlockType
import com.cobling.app.viewmodel.DragManager
import com.cobling.app.viewmodel.QuestViewModel

@Composable
fun NormalBlockView(
    block: Block,
    parentContainer: Block?,
    showChildren: Boolean = true,
    dragManager: DragManager,
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    // ✅ 드래그 중 시각효과만 위한 로컬 상태
    var isDraggingLocal by remember { mutableStateOf(false) }

    var blockPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var blockSizeInWindow by remember { mutableStateOf(IntSize.Zero) }

    val isExecutingThisBlock = viewModel.currentExecutingBlockID == block.id

    val scale = if (isDraggingLocal || isExecutingThisBlock) 1.05f else 1.0f

    val currentOpacity = when {
        isDraggingLocal -> 0.8f
        viewModel.isExecuting && !isExecutingThisBlock -> 0.3f
        else -> 1.0f
    }

    val animatedOpacity by animateFloatAsState(
        targetValue = currentOpacity,
        animationSpec = tween(durationMillis = 200),
        label = "blockOpacity"
    )

    val blockSize = when (block.type) {
        BlockType.START -> IntSize(160, 50)
        else -> IntSize(120, 30)
    }

    Column(
        modifier = modifier
            .padding(1.dp)
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .height(blockSize.height.dp)
                .onGloballyPositioned { coordinates ->
                    blockPositionInWindow = coordinates.positionInWindow()
                    blockSizeInWindow = coordinates.size
                    coordinates.boundsInWindow()
                }
        ) {
            Image(
                painter = painterResource(id = block.type.imageResId),
                contentDescription = null,
                modifier = Modifier
                    .size(
                        width = blockSize.width.dp,
                        height = blockSize.height.dp
                    )
                    .scale(scale)
                    .graphicsLayer {
                        alpha = animatedOpacity
                    }
                    .then(
                        if (block.type == BlockType.START || viewModel.isExecuting) {
                            Modifier
                        } else {
                            Modifier.pointerInput(
                                block.id,
                                dragManager.draggingBlockID,
                                dragManager.isDragging,
                                parentContainer,
                                blockPositionInWindow,
                                blockSizeInWindow
                            ) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val ownerID = dragManager.draggingBlockID
                                        if (ownerID != null && ownerID != block.id) {
                                            return@detectDragGestures
                                        }

                                        isDraggingLocal = true

                                        val position = Offset(
                                            x = blockPositionInWindow.x + offset.x,
                                            y = blockPositionInWindow.y + offset.y
                                        )

                                        if (!dragManager.isDragging) {
                                            dragManager.prepareDragging(
                                                type = block.type,
                                                at = position,
                                                offset = Offset.Zero,
                                                block = block,
                                                parentContainer = parentContainer,
                                                source = DragSource.CANVAS
                                            )
                                        }

                                        dragManager.updateDragPosition(position)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()

                                        val ownerID = dragManager.draggingBlockID
                                        if (ownerID != null && ownerID != block.id) {
                                            return@detectDragGestures
                                        }

                                        isDraggingLocal = true

                                        val position = Offset(
                                            x = blockPositionInWindow.x + change.position.x,
                                            y = blockPositionInWindow.y + change.position.y
                                        )

                                        if (!dragManager.isDragging) {
                                            dragManager.prepareDragging(
                                                type = block.type,
                                                at = position,
                                                offset = dragAmount,
                                                block = block,
                                                parentContainer = parentContainer,
                                                source = DragSource.CANVAS
                                            )
                                        }

                                        dragManager.updateDragPosition(position)
                                    },
                                    onDragEnd = {
                                        isDraggingLocal = false

                                        val endPosition = dragManager.dragPosition ?: Offset(
                                            x = blockPositionInWindow.x,
                                            y = blockPositionInWindow.y
                                        )

                                        // ✅ 드래그 종료 알림
                                        dragManager.finishDrag(at = endPosition) { _, _, _, _ ->
                                            // 실제 삽입 / 이동 처리는 CanvasView에서 수행
                                        }
                                    },
                                    onDragCancel = {
                                        isDraggingLocal = false

                                        val endPosition = dragManager.dragPosition ?: Offset(
                                            x = blockPositionInWindow.x,
                                            y = blockPositionInWindow.y
                                        )

                                        dragManager.finishDrag(at = endPosition) { _, _, _, _ ->
                                            // 실제 삽입 / 이동 처리는 CanvasView에서 수행
                                        }
                                    }
                                )
                            }
                        }
                    )
            )
        }
    }
}