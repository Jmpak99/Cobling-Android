// 블록 드래그 전체 흐름 관리

package com.cobling.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import com.cobling.app.model.Block
import com.cobling.app.model.BlockType
import java.util.UUID

// MARK: - 드래그 출처
enum class DragSource {
    PALETTE,
    CANVAS
}

// MARK: - DragManager (최종 안정 버전)
class DragManager {
    var isDragging: Boolean by mutableStateOf(false)
    var draggingType: BlockType? by mutableStateOf(null)
    var draggingBlock: Block? by mutableStateOf(null)
    var draggingBlockID: UUID? by mutableStateOf(null)
    var dragSource: DragSource by mutableStateOf(DragSource.PALETTE)
    var dragPosition: Offset by mutableStateOf(Offset.Zero)
    var dragStartOffset: Offset by mutableStateOf(Offset.Zero)

    var containerFrame: Rect? by mutableStateOf(null)

    // ✅ 캔버스 드롭 정보(캔버스가 계산해서 넣어줌)
    var isOverCanvas: Boolean by mutableStateOf(false)
    var canvasInsertIndex: Int? by mutableStateOf(null)

    var isOverContainer: Boolean by mutableStateOf(false)
    var containerTargetBlock: Block? by mutableStateOf(null)

    var containerInsertIndex: Int? by mutableStateOf(null)

    var draggingParentContainer: Block? by mutableStateOf(null)

    fun prepareDragging(
        type: BlockType,
        at: Offset,
        offset: Offset,
        block: Block? = null,
        parentContainer: Block? = null,
        source: DragSource
    ) {
        if (isDragging) return

        draggingType = type
        draggingBlock = block
        draggingBlockID = block?.id
        draggingParentContainer = parentContainer
        dragSource = source

        dragPosition = at
        dragStartOffset = offset

        isDragging = true
    }

    fun updateDragPosition(position: Offset) {
        if (!isDragging) return
        dragPosition = position
    }

    fun finishDrag(
        at: Offset,
        onFinish: (
            endPosition: Offset,
            source: DragSource,
            type: BlockType?,
            block: Block?
        ) -> Unit
    ) {
        if (!isDragging) return
        onFinish(at, dragSource, draggingType, draggingBlock)
        reset()
    }

    fun reset() {
        isDragging = false
        draggingType = null
        draggingBlock = null
        draggingBlockID = null
        draggingParentContainer = null
        dragSource = DragSource.PALETTE
        dragPosition = Offset.Zero
        dragStartOffset = Offset.Zero

        // ✅ 캔버스 상태도 리셋
        isOverCanvas = false
        canvasInsertIndex = null

        isOverContainer = false
        containerTargetBlock = null
        containerInsertIndex = null
    }
}