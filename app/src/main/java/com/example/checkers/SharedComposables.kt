package com.example.checkers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.checkers.ui.theme.CheckersTheme

@Composable
fun CheckersGameScreen(navHostController: NavHostController) {
    var checkerBoard by remember { mutableStateOf(Helper.getInitCheckerboard()) }
    var scoreBoard by remember { mutableStateOf(Helper.getInitScoreboard()) }
    var isSnackBarVisible by remember { mutableStateOf(false) }

    fun onTap(piece: Helper.Companion.Piece) {
        // selected piece should not be empty and should also belongs to same player
        if(scoreBoard.isGameCompleted) return

        var tempCheckerBoard = checkerBoard.clone()
        var tempScoreBoard = scoreBoard.copy()

        if(!Helper.isEmptyPiece(piece)) {
            // to show possible moves for a piece
            if(!((Helper.isPlayer1Turn() && Helper.isPlayer1Piece(piece)) || (!Helper.isPlayer1Turn() && Helper.isPlayer2Piece(piece)))) return

            // if attack possible then piece can attack and movable, else piece should be movable
            if(!piece.isMovablePiece && !piece.isCapturingPiece) return

            val possibleMoves = Helper.getPossibleMoves(piece, checkerBoard)
            for (row in 0 until Constants.BOARD_SIZE) {
                for (col in 0 until Constants.BOARD_SIZE) {
                    tempCheckerBoard[row][col].isSelectedPiece = false
                    tempCheckerBoard[row][col].isHighlightedPiece = false
                    tempCheckerBoard[row][col].highlightMoveCapturePiece = false
                }
            }
            for (position in possibleMoves) {
                val possibleMove = tempCheckerBoard[position.x][position.y]
                if(piece.isMovablePiece || (piece.isCapturingPiece && Helper.isCapturingPosition(piece, possibleMove, tempCheckerBoard)))
                    tempCheckerBoard[possibleMove.x][possibleMove.y].isHighlightedPiece = true
            }
            tempCheckerBoard[piece.x][piece.y].isSelectedPiece = true
        } else {
            // to move a piece or capture a opponent piece
            if(!piece.isHighlightedPiece) return

            var higlightedPiece = piece.copy()
            var selPiece = Helper.getSelectedPiece(checkerBoard)

            // select piece should only be allowed
            if(!selPiece.isSelectedPiece || !Helper.isValidPosition(selPiece.x, selPiece.y)) return

            // check for possibility of piece become king and make it
            if(Helper.isKingPromotion(selPiece, higlightedPiece))
                selPiece = Helper.makeKingPromotion(selPiece)

            // move or jump the piece
            tempCheckerBoard = Helper.doMoveOrJump(selPiece, higlightedPiece, tempCheckerBoard)

            // updates score and removes captured piece from board
            if(Helper.isCapturingPosition(selPiece, higlightedPiece, tempCheckerBoard)) {

                tempCheckerBoard = Helper.capturePieceAndUpdateBoard(selPiece, higlightedPiece, tempCheckerBoard)

                // update score board
                if (Helper.isPlayer1Piece(selPiece)) tempScoreBoard.player2--
                else tempScoreBoard.player1--

                // check if game completed or not
                tempScoreBoard.isGameCompleted = tempScoreBoard.player1 == 0 || tempScoreBoard.player2 == 0

                if(!scoreBoard.isGameCompleted) {
                    // chaining logic
                    selPiece =  Helper.getSelectedPiece(tempCheckerBoard)
                    if(Helper.isChainingPossibleForPiece(selPiece, tempCheckerBoard)) {
                        tempCheckerBoard = Helper.updateChainingMovesForPiece(selPiece, tempCheckerBoard)
                    }
                    else {
                        Helper.updatePlayerTurn()
                        tempCheckerBoard = Helper.updatePossibleMovesForPieces(tempCheckerBoard)
                        tempScoreBoard.isGameCompleted = !Helper.isMovesPossibleForPlayer(tempCheckerBoard)
                    }
                }
            } else {
                Helper.updatePlayerTurn()
                tempCheckerBoard = Helper.updatePossibleMovesForPieces(tempCheckerBoard)
                tempScoreBoard.isGameCompleted = !Helper.isMovesPossibleForPlayer(tempCheckerBoard)
            }
        }
        isSnackBarVisible = tempScoreBoard.isGameCompleted
        scoreBoard = tempScoreBoard
        checkerBoard = tempCheckerBoard
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(Constants.PRIMARY_COLOR),
                        Color(Constants.GAME_BG_COLOR),
                        Color(Constants.PRIMARY_COLOR),
                    ),
                    startX = 0.0f,
                    endX = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = {
                    scoreBoard = Helper.getInitScoreboard()
                    checkerBoard = Helper.getInitCheckerboard()
                    isSnackBarVisible = false
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .height(32.dp)
                        .width(32.dp),
                    imageVector = Icons.Default.Refresh,
                    tint = Color(Constants.SECONDARY_COLOR),
                    contentDescription = "Refresh"
                )
            }
            Spacer(
                modifier = Modifier
                    .weight(1f)
            )
            IconButton(
                onClick = {
                    navHostController.navigate("SettingsScreen")
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .height(32.dp)
                        .width(32.dp),
                    imageVector = Icons.Default.Settings,
                    tint = Color(Constants.SECONDARY_COLOR),
                    contentDescription = "Settings"
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row (
                modifier = Modifier
                    .width(160.dp)
                    .height(40.dp)
                    .border(width = 2.dp, color = Color.Gray, shape = RoundedCornerShape(20.dp))
                    .background(
                        color = Color(Constants.PLAYER2_COLOR),
                        shape = RoundedCornerShape(20.dp)
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Player2", fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.width(14.dp))
                Text(text = "-", fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.width(14 .dp))
                Text(text = scoreBoard.player2.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Spacer(modifier = Modifier.height(30.dp))
            Canvas(
                modifier = Modifier
                    .aspectRatio(1.0f)
                    .padding(10.dp)
                    .border(width = 1.dp, color = Color(Constants.SECONDARY_COLOR))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val boardHeight =
                                    Constants.BOARD_SIZE * (canvasWidth / Constants.BOARD_SIZE)
                                val restHeight = canvasHeight - boardHeight
                                val boardStartOffset = (restHeight / 2).toFloat()
                                val boardEndOffset = (boardStartOffset + boardHeight).toFloat()

                                // check whether user tapped on checker canvas board
                                if (tapOffset.y >= boardStartOffset && tapOffset.y <= boardEndOffset) {
                                    val tileSize = size.width / Constants.BOARD_SIZE
                                    val row =
                                        ((tapOffset.y - boardStartOffset) / tileSize).toInt()
                                    val col = (tapOffset.x / tileSize).toInt()
                                    val selectPiece = checkerBoard[row][col]
                                    onTap(selectPiece)
                                }
                            }
                        )
                    }
            )  {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val boardWidth = canvasWidth
                val boardHeight = Constants.BOARD_SIZE * (canvasWidth / Constants.BOARD_SIZE)
                val restHeight = canvasHeight - boardHeight
                val boardOffset = restHeight / 2

                drawCheckerboard(
                    boardWidth, 0F,
                    boardOffset
                )
                drawCheckerPieces(checkerBoard, boardWidth, 0F, boardOffset)
            }
            Spacer(modifier = Modifier.height(30.dp))
            Column (
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row (
                    modifier = Modifier
                        .width(160.dp)
                        .height(40.dp)
                        .border(width = 2.dp, color = Color.Gray, shape = RoundedCornerShape(20.dp))
                        .background(
                            color = Color(Constants.PLAYER1_COLOR),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Player1", fontSize = 14.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = "-", fontSize = 14.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = scoreBoard.player1.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        }
        if(isSnackBarVisible) {
            Snackbar(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.BottomCenter),
                action = {
                    TextButton(
                        onClick = { isSnackBarVisible = !isSnackBarVisible }
                    ) {
                        Text("DISMISS")
                    }
                }
            ) {
                var message = ""
                if(scoreBoard.player2 == 0 || !Helper.isPlayer1Turn()) message = "Player-1 won the game !!!"
                else if(scoreBoard.player1 == 0 || Helper.isPlayer1Turn()) message = "Player-2 won the game !!!"

                if(!message.isEmpty())
                    Text(text = message)
            }
        }
    }
}

@Composable
fun SettingsScreen(navHostController: NavHostController) {

    var defaultSelectedItem = Helper.Companion.ITEMVALUE.PLAYER1
    var defaultSelectedItemColor = Helper.getItemColor(defaultSelectedItem)

    var redValue by remember { mutableStateOf(defaultSelectedItemColor.hashCode().red) }
    var greenValue by remember { mutableStateOf(defaultSelectedItemColor.hashCode().green) }
    var blueValue by remember { mutableStateOf(defaultSelectedItemColor.hashCode().blue) }

    var selectedItem: Helper.Companion.ITEMVALUE by remember { mutableStateOf(defaultSelectedItem) }


    fun onClickOfColorPicker(item: Helper.Companion.ITEMVALUE) {
        selectedItem = item
        val selectedItemColor = Helper.getItemColor(selectedItem)
        redValue = selectedItemColor.hashCode().red
        greenValue = selectedItemColor.hashCode().green
        blueValue = selectedItemColor.hashCode().blue
    }

    fun updateColor() {
        val selectedItemColor = Color(redValue, greenValue, blueValue)
        val tempItemColorsList = Helper.getItemColorsList()
        tempItemColorsList.find{it.item == selectedItem}?.color = selectedItemColor
        Helper.setItemColorsList(tempItemColorsList)
    }

    fun restoreDefaults() {
        val tempItemColorsList = Helper.getInitialItemColorsList()
        Helper.setItemColorsList(tempItemColorsList)
        navHostController.navigateUp()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(Constants.PRIMARY_COLOR),
                        Color(Constants.GAME_BG_COLOR),
                        Color(Constants.GAME_BG_COLOR),
                        Color(Constants.PRIMARY_COLOR),
                    ),
                    startY = 0.0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(vertical = 20.dp, horizontal = 10.dp)
    ) {
        Row (
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                navHostController.navigateUp()
            }) {
                Icon(
                    modifier = Modifier
                        .height(26.dp)
                        .width(26.dp),
                    imageVector = Icons.Default.ArrowBack,
                    tint = Color(Constants.SECONDARY_COLOR),
                    contentDescription = "Back"
                )
            }
            Text(text = "Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(Constants.SECONDARY_COLOR))
            Spacer(modifier = Modifier.weight(1f))
        }
        // Spacer(modifier = Modifier.height(32.dp))
        Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "Piece", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(Constants.SECONDARY_COLOR))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            Text(text = "Player 1 -", color = Color(Constants.SECONDARY_COLOR))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Helper.getItemColor(
                                            Helper.Companion.ITEMVALUE.PLAYER1
                                        ), shape = CircleShape
                                    )
                                    .border(1.5.dp, shape = CircleShape,
                                        color = if(selectedItem == Helper.Companion.ITEMVALUE.PLAYER1)
                                                    Color(Constants.HIGHLIGHT_COLOR)
                                                else Color.Gray)
                                    .size(30.dp)
                                    .padding(16.dp)
                                    .clickable {
                                        onClickOfColorPicker(
                                            Helper.Companion.ITEMVALUE.PLAYER1
                                        )
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Row {
                            Text(text = "Player 2 -", color = Color(Constants.SECONDARY_COLOR))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Helper.getItemColor(
                                            Helper.Companion.ITEMVALUE.PLAYER2
                                        ), shape = CircleShape
                                    )
                                    .border(1.5.dp, shape = CircleShape,
                                            color = if(selectedItem == Helper.Companion.ITEMVALUE.PLAYER2)
                                                        Color(Constants.HIGHLIGHT_COLOR)
                                                    else Color.Gray)
                                    .size(30.dp)
                                    .padding(16.dp)
                                    .clickable {
                                        onClickOfColorPicker(
                                            Helper.Companion.ITEMVALUE.PLAYER2
                                        )
                                    }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "Square", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(Constants.SECONDARY_COLOR))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            Text(text = "Dark -", color = Color(Constants.SECONDARY_COLOR))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Helper.getItemColor(
                                            Helper.Companion.ITEMVALUE.PRIMARY_SQUARE
                                        ), shape = CircleShape
                                    )
                                    .border(1.5.dp, shape = CircleShape,
                                        color = if(selectedItem == Helper.Companion.ITEMVALUE.PRIMARY_SQUARE)
                                                    Color(Constants.HIGHLIGHT_COLOR)
                                                else Color.Gray)
                                    .size(30.dp)
                                    .padding(16.dp)
                                    .clickable {
                                        onClickOfColorPicker(
                                            Helper.Companion.ITEMVALUE.PRIMARY_SQUARE
                                        )
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Row {
                            Text(text = "Light -", color = Color(Constants.SECONDARY_COLOR))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Helper.getItemColor(
                                            Helper.Companion.ITEMVALUE.SECONDARY_SQUARE
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(1.5.dp, shape = CircleShape,
                                        color = if(selectedItem == Helper.Companion.ITEMVALUE.SECONDARY_SQUARE)
                                                    Color(Constants.HIGHLIGHT_COLOR)
                                                else Color.Gray)
                                    .size(30.dp)
                                    .padding(16.dp)
                                    .clickable {
                                        onClickOfColorPicker(
                                            Helper.Companion.ITEMVALUE.SECONDARY_SQUARE
                                        )
                                    }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column (
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    // Display selected color
                    Row (
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Red slider
                        Text(text = "R", fontSize = 14.sp, color = Color(Constants.SECONDARY_COLOR))
                        Spacer(modifier = Modifier.width(4.dp))
                        Slider(
                            colors = SliderDefaults.colors(
                                thumbColor = Color(Constants.PRIMARY_COLOR),
                                activeTrackColor = Color(Constants.PRIMARY_COLOR),
                                inactiveTrackColor = Color(Constants.SECONDARY_COLOR).copy(alpha = 0.5f)
                            ),
                            value = redValue.toFloat(),
                            valueRange = 0f..255f,
                            steps = 255,
                            onValueChange = { value ->
                                redValue = value.toInt()
                                updateColor()
                            }
                        )
                    }

                    Row (
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green slider
                        Text(text = "G", fontSize = 14.sp, color = Color(Constants.SECONDARY_COLOR))
                        Spacer(modifier = Modifier.width(4.dp))
                        Slider(
                            colors = SliderDefaults.colors(
                                thumbColor = Color(Constants.PRIMARY_COLOR),
                                activeTrackColor = Color(Constants.PRIMARY_COLOR),
                                inactiveTrackColor = Color(Constants.SECONDARY_COLOR).copy(alpha = 0.5f)
                            ),
                            value = greenValue.toFloat(),
                            valueRange = 0f..255f,
                            steps = 255,
                            onValueChange = { value ->
                                greenValue = value.toInt()
                                updateColor()
                            }
                        )
                    }

                    Row (
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Blue slider
                        Text(text = "B", fontSize = 14.sp, color = Color(Constants.SECONDARY_COLOR))
                        Spacer(modifier = Modifier.width(4.dp))
                        Slider(
                            colors = SliderDefaults.colors(
                                thumbColor = Color(Constants.PRIMARY_COLOR),
                                activeTrackColor = Color(Constants.PRIMARY_COLOR),
                                inactiveTrackColor = Color(Constants.SECONDARY_COLOR).copy(alpha = 0.5f)
                            ),
                            value = blueValue.toFloat(),
                            valueRange = 0f..255f,
                            steps = 255,
                            onValueChange = { value ->
                                blueValue = value.toInt()
                                updateColor()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(Constants.SECONDARY_COLOR),
                        contentColor = Color(Constants.PRIMARY_COLOR)),
                    onClick = { restoreDefaults() }
                ) {
                    Text(text = "Restore Defaults")
                }
            }
        }
}

private fun DrawScope.drawCheckerboard(
    boardWidth: Float,
    offsetX: Float,
    offsetY: Float
) {
    val primarySquareColor = Helper.getItemColor(Helper.Companion.ITEMVALUE.PRIMARY_SQUARE)
    val secondarySquareColor = Helper.getItemColor(Helper.Companion.ITEMVALUE.SECONDARY_SQUARE)

    val tileSize = boardWidth / Constants.BOARD_SIZE
    for (row in 0 until Constants.BOARD_SIZE) {
        for (col in 0 until Constants.BOARD_SIZE) {
            val color = if ((row + col) % 2 != 0) primarySquareColor else secondarySquareColor
            drawRect(
                color = color,
                topLeft = Offset((col * tileSize) + offsetX, (row * tileSize) + offsetY),
                size = Size(tileSize, tileSize)
            )
        }
    }
}


private fun DrawScope.drawCheckerPieces(
    board: Array<Array<Helper.Companion.Piece>>,
    boardWidth: Float,
    offsetX: Float,
    offsetY: Float
) {
    val tileSize = boardWidth / Constants.BOARD_SIZE
    val player1Color = Helper.getItemColor(Helper.Companion.ITEMVALUE.PLAYER1)
    val player2Color = Helper.getItemColor(Helper.Companion.ITEMVALUE.PLAYER2)

    for (col in 0 until Constants.BOARD_SIZE) {
        for (row in 0 until Constants.BOARD_SIZE) {
            val piece = board[row][col]
            val centerX = (((col + 0.5F) * tileSize) + offsetX)
            val centerY = (((row + 0.5F) * tileSize) + offsetY)

            // highlight the possible moves for a piece
            if(piece.isHighlightedPiece) {
                drawCircle(
                    color = Color(Constants.HIGHLIGHT_COLOR),
                    radius = tileSize/8F,
                    center = Offset(centerX, centerY)
                )
            }

            if (Helper.isEmptyPiece(piece)) continue

            // highlight the selected piece
            if(piece.isSelectedPiece) {
                drawRect(
                    color = Color(Constants.HIGHLIGHT_COLOR),
                    topLeft = Offset((col * tileSize) + offsetX, (row * tileSize) + offsetY),
                    size = Size(tileSize, tileSize)
                )
            }

            // highlight the movable piece
            if(piece.highlightMoveCapturePiece) {
                drawCircle(
                    color = Color(Constants.HIGHLIGHT_COLOR),
                    radius = tileSize / 2.6F,
                    center = Offset(centerX, centerY)
                )
            }

            // draw the checker piece
            val pieceColor = if (Helper.isPlayer1Piece(piece)) player1Color else player2Color
            val pieceRingColor = if (Helper.isPlayer1Piece(piece)) Color(Constants.PLAYER1_RING_COLOR) else Color(Constants.PLAYER2_RING_COLOR)

            // piece outer part
            for (radiusIndex in 3 until 6) {
                val color = if (radiusIndex % 2 != 0) pieceColor else pieceRingColor
                drawCircle(
                    color = color,
                    radius = tileSize / radiusIndex,
                    center = Offset(centerX, centerY)
                )
            }

            if(Helper.isKingPiece(piece)) {
                // to show a king piece
                val dimSize = tileSize / 5
                drawPath(
                    path = Path().apply {
                        moveTo(col * tileSize + tileSize / 2 + offsetX, row * tileSize + tileSize / 2 - dimSize + offsetY)
                        lineTo(col * tileSize + tileSize / 2 + dimSize + offsetX, row * tileSize + tileSize / 2 + offsetY)
                        lineTo(col * tileSize + tileSize / 2 + offsetX, row * tileSize + tileSize / 2 + dimSize + offsetY)
                        lineTo(col * tileSize + tileSize / 2 - dimSize + + offsetX, row * tileSize + tileSize / 2 + offsetY)
                        close()
                    },
                    color = Color.Red
                )
            } else {
                // to show a normal piece
                for (radiusIndex in 3 until Constants.BOARD_SIZE) {
                    val color = if (radiusIndex % 2 != 0) pieceColor else pieceRingColor
                    drawCircle(
                        color = color,
                        radius = tileSize / radiusIndex,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

@Composable
fun CheckersGameApp(navHostController: NavHostController) {
    CheckersTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navHostController, startDestination = "CheckersGameScreen") {
                composable("CheckersGameScreen") {
                    CheckersGameScreen(navHostController)
                }
                composable("SettingsScreen") {
                    SettingsScreen(navHostController)
                }
            }
        }
    }
}

@Preview
@Composable
fun CheckersGameScreenPreview() {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val navHostController = rememberNavController()
        CheckersGameApp(navHostController)
    }
}