// 개별 블록 UI

package com.cobling.app.view.quest

import androidx.compose.runtime.Composable
import com.cobling.app.model.Block
import androidx.compose.ui.Modifier
import com.cobling.app.viewmodel.DragManager
import com.cobling.app.viewmodel.QuestViewModel

@Composable
fun BlockView(
    block: Block,
    parentContainer: Block?,
    dragManager: DragManager,
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    if (block.type.isContainer) {
        // 🔁 반복문 / if / ifElse
        ContainerBlockView(
            block = block,
            dragManager = dragManager,
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        // ▶️ 이동 / 회전 / 공격 / 시작
        NormalBlockView(
            block = block,
            parentContainer = parentContainer,
            dragManager = dragManager,
            viewModel = viewModel,
            modifier = modifier
        )
    }
}