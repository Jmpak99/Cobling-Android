// 블록 팔레트 영역

package com.cobling.app.view.quest

import com.cobling.app.viewmodel.DragSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cobling.app.viewmodel.DragManager
import com.cobling.app.viewmodel.QuestViewModel

@Composable
fun BlockPaletteView(
    dragManager: DragManager,
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(top = 16.dp, start = 12.dp, end = 8.dp)
    ) {
        viewModel.allowedBlocks.forEach { type ->
            var frameInRoot: Rect? = null

            Image(
                painter = painterResource(id = type.imageResId),
                contentDescription = null,
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        frameInRoot = coordinates.boundsInRoot()
                    }
                    .width(120.dp)
                    .height(if (type.isContainer) 60.dp else 30.dp)
                    .scale(0.85f)
                    .pointerInput(type) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val frame = frameInRoot ?: return@detectDragGestures
                                val position = Offset(
                                    x = frame.left + startOffset.x,
                                    y = frame.top + startOffset.y
                                )

                                if (!dragManager.isDragging) {
                                    val offset = Offset(
                                        x = startOffset.x - 80f,
                                        y = startOffset.y - 20f
                                    )
                                    dragManager.prepareDragging(
                                        type = type,
                                        at = position,
                                        offset = offset,
                                        block = null,
                                        source = DragSource.PALETTE
                                    )
                                }

                                dragManager.updateDragPosition(position)
                            },
                            onDrag = { change, _ ->
                                change.consume()

                                val frame = frameInRoot ?: return@detectDragGestures
                                val position = Offset(
                                    x = frame.left + change.position.x,
                                    y = frame.top + change.position.y
                                )

                                if (!dragManager.isDragging) {
                                    val offset = Offset(
                                        x = change.position.x - 80f,
                                        y = change.position.y - 20f
                                    )
                                    dragManager.prepareDragging(
                                        type = type,
                                        at = position,
                                        offset = offset,
                                        block = null,
                                        source = DragSource.PALETTE
                                    )
                                }

                                dragManager.updateDragPosition(position)
                            },
                            onDragEnd = {
                                // 🔥 Palette에서는 드래그 종료 처리 ❌
                                // Canvas가 finishDrag를 담당함
                            },
                            onDragCancel = {
                                // 별도 처리 없음
                            }
                        )
                    }
            )

            androidx.compose.foundation.layout.Box(
                modifier = Modifier.height(if (type.isContainer) 44.dp else 40.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}