// 블록코딩 블록 데이터 구조 및 트리 구조 정의
//
//  Block.kt
//  Cobling
//
//  Created by 박종민 on 2025/07/02.
//

package com.cobling.app.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cobling.coblingapp.R
import java.util.UUID

enum class BlockType(val rawValue: String) {
    START("start"),
    MOVE_FORWARD("moveForward"),
    TURN_LEFT("turnLeft"),
    TURN_RIGHT("turnRight"),
    ATTACK("attack"),
    REPEAT_COUNT("repeatCount"),
    REPEAT_FOREVER("repeatForever"),
    IF("if"),
    IF_ELSE("ifElse"),
    BREAK_LOOP("breakLoop"),
    CONTINUE_LOOP("continueLoop");

    val id: String
        get() = rawValue

    val imageName: String
        get() = when (this) {
            START -> "block_start"
            MOVE_FORWARD -> "block_move"
            TURN_LEFT -> "block_turn_left"
            TURN_RIGHT -> "block_turn_right"
            ATTACK -> "block_attack"
            REPEAT_COUNT -> "block_repeat_count"
            REPEAT_FOREVER -> "block_repeat_forever"
            IF -> "block_if"
            IF_ELSE -> "block_if_else"
            BREAK_LOOP -> "block_break"
            CONTINUE_LOOP -> "block_continue"
        }

    val imageResId: Int
        get() = when (this) {
            START -> R.drawable.block_start
            MOVE_FORWARD -> R.drawable.block_move
            TURN_LEFT -> R.drawable.block_turn_left
            TURN_RIGHT -> R.drawable.block_turn_right
            ATTACK -> R.drawable.block_attack
            REPEAT_COUNT -> R.drawable.block_repeat_count
            REPEAT_FOREVER -> R.drawable.block_repeat_forever
            IF -> R.drawable.block_if
            IF_ELSE -> R.drawable.block_if_else
            BREAK_LOOP -> R.drawable.block_break
            CONTINUE_LOOP -> R.drawable.block_continue
        }

    val isContainer: Boolean
        get() = when (this) {
            REPEAT_COUNT, REPEAT_FOREVER, IF, IF_ELSE -> true
            else -> false
        }

    companion object {
        fun fromRawValue(value: String): BlockType? {
            return entries.find { it.rawValue == value }
        }
    }
}

enum class IfCondition(val rawValue: String) {
    FRONT_IS_CLEAR("frontIsClear"),
    FRONT_IS_BLOCKED("frontIsBlocked"),
    AT_FLAG("atFlag"),
    ENEMY_IN_FRONT("enemyInFront"),
    ALWAYS("always");

    val id: String
        get() = rawValue

    val label: String
        get() = when (this) {
            FRONT_IS_CLEAR -> "앞이 비어있으면"
            FRONT_IS_BLOCKED -> "앞이 막혀있으면"
            ENEMY_IN_FRONT -> "앞에 적이 있으면"
            AT_FLAG -> "깃발에 도착했으면"
            ALWAYS -> "항상"
        }

    companion object {
        fun fromRawValue(value: String?): IfCondition? {
            return entries.find { it.rawValue == value }
        }
    }
}

class Block(
    val type: BlockType,
    value: String? = null,
    condition: IfCondition = IfCondition.FRONT_IS_CLEAR,
    children: List<Block> = emptyList(),
    elseChildren: List<Block> = emptyList()
) {

    val id: UUID = UUID.randomUUID()

    // 공통: 컨테이너 기본 영역(Repeat, if(then)은 여기 사용)
    val children = mutableStateListOf<Block>()

    // ifElse 전용 else 영역 (if는 비워둬도 됨)
    val elseChildren = mutableStateListOf<Block>()

    var parent: Block? = null

    // repeatCount 등 값
    var value by mutableStateOf(value)

    // if 조건값
    var condition by mutableStateOf(condition)

    init {
        this.children.addAll(children)
        this.elseChildren.addAll(elseChildren)

        for (child in this.children) {
            child.parent = this
        }
        for (child in this.elseChildren) {
            child.parent = this
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Block) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}