package cl.jacevedo.droidcontroller.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.jacevedo.droidcontroller.R
import cl.jacevedo.droidcontroller.data.BluetoothDroidObject
import cl.jacevedo.droidinterfaces.ButtonDroidEntity
import cl.jacevedo.droidcontroller.ui.theme.ChopperControllerTheme
import cl.jacevedo.droidcontroller.viewmodel.DroidActivityViewModel

@Preview(
    device = "spec:width=800dp,height=360dp,dpi=420,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun DroidViewPreview() {
    val mainDroidViewModel = DroidActivityViewModel()
    val listObject = mutableListOf(
        ButtonDroidEntity(1,1, 1, "test", "1"),
        ButtonDroidEntity(1,1, 2, "test", "1"),
        ButtonDroidEntity(1,1, 2, "test", "1"),
        ButtonDroidEntity(1,1, 2, "test", "1"),
        ButtonDroidEntity(1,1, 2, "asdasdasdasd", "1"),
        ButtonDroidEntity(1,1, 2, "test", "1"),
        ButtonDroidEntity(1,1, 2, "test", "1"),
        ButtonDroidEntity(1,1, 2, "test", "1"),
        )

    mainDroidViewModel.audioButtonsList.value = listObject
    mainDroidViewModel.macroButtonsList.value = listObject
    ChopperControllerTheme {
        DroidActivityView(mainDroidViewModel, buttonClick = {}, sliderUpdate = {})
    }
}

@Composable
fun DroidActivityView(droidActivityViewModel: DroidActivityViewModel = viewModel(),
                      bluetoothDroidObject: BluetoothDroidObject? = BluetoothDroidObject("","",0, cl.jacevedo.droidinterfaces.DeviceType.CHOPPER),
                      buttonClick : (ButtonDroidEntity) -> Unit,
                      sliderUpdate : (Float) -> Unit) {
    val macroButtonList = droidActivityViewModel.macroButtonsList.observeAsState()
    val audioButtonList = droidActivityViewModel.audioButtonsList.observeAsState()
    val uploadVolume = droidActivityViewModel.droidVolume.observeAsState(25f)
    val viviMode = droidActivityViewModel.viviMode.observeAsState(true)
    val currentTestAction by droidActivityViewModel.currentTestAction.observeAsState()
    var showModeDialog by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
                .background(Color.Black)
                .fillMaxSize()

        ) {
            Column(
                Modifier
                    .width((LocalConfiguration.current.screenWidthDp / 3).dp)
                    .fillMaxHeight()
                    .padding(
                        top = 16.dp, bottom = 16.dp, start = padding.calculateStartPadding(
                            LayoutDirection.Ltr
                        ) + 20.dp
                    )) {
                Image(
                    painter = painterResource(id = getButtonImage(bluetoothDroidObject)),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 32.dp, start = 16.dp),
                    contentDescription = "chopper"
                )
                Text(
                    text = "Vol: ${uploadVolume.value.toInt()}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val newVal = (uploadVolume.value - 5f).coerceIn(0f, 50f)
                            droidActivityViewModel.droidVolume.value = newVal
                            sliderUpdate(50f - newVal)
                        },
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.width(36.dp)
                    ) { Text("-") }
                    Slider(
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties { canFocus = false },
                        value = uploadVolume.value,
                        onValueChangeFinished = { sliderUpdate(50f - uploadVolume.value) },
                        onValueChange = { droidActivityViewModel.droidVolume.value = it },
                        steps = 9,
                        valueRange = 0f..50f
                    )
                    Button(
                        onClick = {
                            val newVal = (uploadVolume.value + 5f).coerceIn(0f, 50f)
                            droidActivityViewModel.droidVolume.value = newVal
                            sliderUpdate(50f - newVal)
                        },
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.width(36.dp)
                    ) { Text("+") }
                }
            }
            Column(modifier = Modifier
                .padding(
                    top = 10.dp,
                    bottom = 10.dp,
                    end = padding.calculateEndPadding(LayoutDirection.Ltr) + 20.dp
                )
                .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween){
               Row (modifier = Modifier
                   .height((LocalConfiguration.current.screenHeightDp.dp))
                   .focusProperties { canFocus = false }){
                    LazyVerticalGrid( modifier = Modifier
                        .weight(2f)
                        .focusProperties { canFocus = false }, columns = GridCells.Fixed(2),  verticalArrangement = Arrangement.Top) {
                        items(audioButtonList.value ?: emptyList()) { button ->
                            componentButton(label = button.label ?: "", onClick = {buttonClick(button)})
                        }
                    }
                    LazyVerticalGrid( modifier = Modifier.weight(1f), columns = GridCells.Fixed(1),  verticalArrangement = Arrangement.Top) {
                        items(macroButtonList.value ?: emptyList()) { button ->
                            componentButton(label = button.label ?: "", onClick = {buttonClick(button)})
                        }
                    }
                }

            }
        }
        if (droidActivityViewModel.supportsViviMode) {
            Button(
                onClick = { showModeDialog = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = padding.calculateTopPadding() + 8.dp, start = 8.dp)
            ) {
                Text(if (viviMode.value == true) "Vivi Mode" else "Normal Mode")
            }
        }
        if (showModeDialog) {
            val switchingToVivi = viviMode.value != true
            AlertDialog(
                onDismissRequest = { showModeDialog = false },
                title = { Text(if (switchingToVivi) "Switch to Vivi Mode?" else "Switch to Normal Mode?") },
                text = { Text(if (switchingToVivi) "Left joystick will be capped at 50." else "Left joystick will use full range (100).") },
                confirmButton = {
                    TextButton(onClick = {
                        droidActivityViewModel.viviMode.value = switchingToVivi
                        showModeDialog = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showModeDialog = false }) { Text("Cancel") }
                }
            )
        }
        currentTestAction?.let { action ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Test Mode") },
                text = { Text("Execute: ${action.label ?: "Unknown"}") },
                confirmButton = {
                    TextButton(onClick = {
                        buttonClick(action)
                        droidActivityViewModel.showNextTestAction()
                    }) { Text("OK") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { droidActivityViewModel.showNextTestAction() }) {
                            Text("No")
                        }
                        TextButton(onClick = { droidActivityViewModel.cancelTestSequence() }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
        }
    }

}

@Composable
fun componentButton(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, // Gold color for Star Wars feel
            contentColor = Color.Black
        )) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.button),
                contentDescription = "Chopper",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            ) // Maintain aspect ratio and fit within bounds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp),
                contentAlignment = Alignment.Center

            ) {
                DroidTextView(
                    color = Color.Gray,
                    overflow = TextOverflow.Ellipsis,
                    maxlines = 1,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                    text = label,
                )
            }
        }
    }
}