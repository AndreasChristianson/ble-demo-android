package com.pessimistic.blepresentation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun App(activity: MainActivity): (String) -> Unit {
    val (connectionStatus, setConnectionStatus) = remember { mutableStateOf("Disconnected") }
    Column(
        modifier = Modifier.fillMaxSize()
    )
    {
        Row {
            Button(onClick = activity::connect) {
                Text(text = "Connect")
            }
            Text(text = "status: $connectionStatus")
        }
        Row {
            Button(onClick = activity::closeAll) {
                Text(text = "Disconnect")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            Column {
                Button(onClick = { activity.setTracks(-256,255) }) {
                    Text("Left")
                }
            }
            Column {
                Button(onClick = {activity.setTracks(255,255)}) {
                    Text("Forwards")
                }
                Button(onClick = {activity.setTracks(0,0)}) {
                    Text("Stop")
                }
                Button(onClick = {activity.setTracks(-256,-256)}) {
                    Text("Backwards")
                }
            }
            Column {
                Button(onClick = {activity.setTracks(255,-256)}) {
                    Text("Right")
                }
            }
        }
    }
    return setConnectionStatus
}