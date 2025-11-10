package jp.mec.aiinterviewmirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.mec.aiinterviewmirror.logic.calculateScore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var score by remember { mutableStateOf(0f) }
                var comment by remember { mutableStateOf("準備ができました。") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("AIセルフプレゼン練習ミラー", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(20.dp))
                        Text("デモスコア: ${"%.1f".format(score)}点")
                        Spacer(Modifier.height(10.dp))
                        Text(comment)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = {
                            val (s, c) = calculateScore()
                            score = s
                            comment = c
                        }) {
                            Text("採点を試す")
                        }
                    }
                }
            }
        }
    }
}
