package com.pessimistic.blepresentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    var status = "Disconnected"
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row {
            Button(onClick = {
                //todo
            }){
                Text(
                    text = "Connect"
                )
            }
            Text(
                text = "status: $status"
            )
        }
        Row(
            modifier = Modifier.fillMaxSize()
            .wrapContentSize(Alignment.Center)
        ) {
            Column {
                Button(onClick = {
                    Log.i("ble demo","left")
                }) {
                    Text("Left")
                }
            }
            Column {
                Button(onClick = {
                    Log.i("ble demo","forwards")
                }) {
                    Text("Forwards")
                }
                Button(onClick = {
                    Log.i("ble demo","stop")
                }) {
                    Text("Stop")
                }
                Button(onClick = {
                    Log.i("ble demo","backwards")
                }) {
                    Text("Backwards")
                }
            }
            Column {
                Button(onClick = {
                    Log.i("ble demo","right")
                }) {
                    Text("Right")
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun Preview() {
    MaterialTheme {
        App()
    }
}