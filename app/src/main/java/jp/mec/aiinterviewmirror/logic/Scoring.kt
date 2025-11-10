package jp.mec.aiinterviewmirror.logic
import kotlin.random.Random

fun calculateScore(): Pair<Float, String> {
    val gaze = Random.nextFloat() * 20
    val smile = Random.nextFloat() * 20
    val posture = Random.nextFloat() * 20
    val speech = Random.nextFloat() * 30
    val time = 10f
    val total = gaze + smile + posture + speech + time
    val comment = when {
        total >= 85 -> "よくできました！自然な受け答えです。"
        total >= 70 -> "おおむね良好ですが、話すスピードを少し意識しましょう。"
        else -> "目線や表情をもう少し意識してみましょう。"
    }
    return Pair(total, comment)
}
