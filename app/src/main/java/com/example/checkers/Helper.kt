package com.example.checkers

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

class Helper {

    companion object {
        enum class PLAYERTURN { NONE, PLAYER1, PLAYER2 }
        enum class PIECEVALUE { EMPTY, PLAYER1_NORMAL, PLAYER2_NORMAL, PLAYER1_KING, PLAYER2_KING }
        enum class DIRECTION(var position: Position) {
            TOPLEFT(Position(-1, -1)),
            TOPRIGHT(Position(-1, 1)),
            BOTTOMLEFT(Position(1, -1)),
            BOTTOMRIGHT(Position(1, 1))
        }

        enum class ITEMVALUE  {
            PLAYER1,
            PLAYER2,
            PRIMARY_SQUARE,
            SECONDARY_SQUARE
        }

        data class Item (
            var item: ITEMVALUE,
            var color: Color
        )
        data class Position(var x: Int, var y: Int)

        data class ScoreBoard(
            var player1: Int = 0,
            var player2: Int = 0,
            var isGameCompleted: Boolean = false
        )

        data class Piece(
            var x: Int = -1,
            var y: Int = -1,
            var value: PIECEVALUE = PIECEVALUE.EMPTY,
            var isSelectedPiece: Boolean = false,    // flag for selected piece
            var isMovablePiece: Boolean = false,     // flag for movable piece
            var isCapturingPiece: Boolean = false,  // flag for attacking piece
            var highlightMoveCapturePiece: Boolean = false,   // flag for movable piece background
            var isHighlightedPiece: Boolean = false // flag for highlighted piece (possible move for other piece)
        )

        private var playerTurn: PLAYERTURN = PLAYERTURN.NONE
        private var itemColorsList: List<Item> = getInitialItemColorsList()

        fun updateCapturingPieces(board: Array<Array<Piece>>): Array<Array<Piece>> {
            val board = resetSuggestionsOnBoard(board)
            var isCapturingPossibleFlag = false
            for (row in 0 until Constants.BOARD_SIZE) {
                for (col in 0 until Constants.BOARD_SIZE) {
                    val piece = board[row][col].copy()
                    if ((isPlayer1Turn() && isPlayer1Piece(piece)) || (!isPlayer1Turn() && isPlayer2Piece(piece))) {
                        val possibleMovesForPiece = getPossibleMoves(piece, board)
                        for (possibleMove in possibleMovesForPiece) {
                            // check if attacks possible
                            val highlightedPiece = board[possibleMove.x][possibleMove.y]
                            if (isCapturingPosition(piece, highlightedPiece, board)) {
                                piece.highlightMoveCapturePiece = true
                                piece.isCapturingPiece = true
                                isCapturingPossibleFlag = true
                            }
                        }
                    }
                    board[row][col] = piece
                }
            }
            return board
        }

        fun isChainingPossibleForPiece(piece: Piece, board: Array<Array<Piece>>): Boolean {
            var possibleMoves = getPossibleMoves(piece, board)
            for(position in possibleMoves) {
                val otherPiece = board[position.x][position.y]
                if(isCapturingPosition(piece, otherPiece, board)) return true
            }
            return false
        }

        fun updateChainingMovesForPiece(piece: Piece, board: Array<Array<Piece>>): Array<Array<Piece>> {
            val board = resetSuggestionsOnBoard(board)
            piece.isCapturingPiece = true
            piece.isMovablePiece = false
            piece.isHighlightedPiece = false
            piece.highlightMoveCapturePiece = true
            piece.isSelectedPiece = true
            board[piece.x][piece.y] = piece
            return board
        }

        fun updatePossibleMovesForPieces(board: Array<Array<Piece>>): Array<Array<Piece>> {
            val board = resetSuggestionsOnBoard(board)
            for (row in 0 until Constants.BOARD_SIZE) {
                for (col in 0 until Constants.BOARD_SIZE) {
                    val piece = board[row][col].copy()
                    // turn and piece should be of same player
                    if ((isPlayer1Turn() && isPlayer1Piece(piece)) || (!isPlayer1Turn() && isPlayer2Piece(piece))) {
                        // get possible moves for a piece
                        val possibleMovesForPiece = getPossibleMoves(piece, board)
                        // check the possible moves for a piece
                        for (possibleMove in possibleMovesForPiece) {
                            // check if attacks possible
                            piece.highlightMoveCapturePiece = true
                            piece.isMovablePiece = true
                        }
                    }
                    board[row][col] = piece
                }
            }
            return board
        }

        fun isCapturingPosition(
            piece: Piece,
            highlightedPiece: Piece,
            board: Array<Array<Piece>>
        ): Boolean {

            val dirsAllowed: List<DIRECTION> = if (isKingPiece(piece))
                listOf(
                    DIRECTION.TOPLEFT,
                    DIRECTION.BOTTOMLEFT,
                    DIRECTION.TOPRIGHT,
                    DIRECTION.BOTTOMRIGHT
                )
            else
                if (isPlayer1Piece(piece)) listOf(DIRECTION.TOPLEFT, DIRECTION.TOPRIGHT)
                else listOf(
                    DIRECTION.BOTTOMLEFT,
                    DIRECTION.BOTTOMRIGHT
                )

            for (dir in dirsAllowed) {
                if (piece.x + (2 * dir.position.x) == highlightedPiece.x && piece.y + (2 * dir.position.y) == highlightedPiece.y) {
                    val capturedPieceX = (abs(piece.x + highlightedPiece.x) / 2).toInt()
                    val capturedPieceY = (abs(piece.y + highlightedPiece.y) / 2).toInt()
                    val capturedPiece = board[capturedPieceX][capturedPieceY]
                    if ((isPlayer1Piece(piece) && isPlayer2Piece(capturedPiece)) || (isPlayer2Piece(
                            piece
                        ) && isPlayer1Piece(capturedPiece))
                    )
                        return true
                }
            }
            return false
        }


        fun getPossibleMoves(piece: Piece, board: Array<Array<Piece>>): List<Position> {
            return if (isNormalPiece(piece)) {
                // get possible moves for normal piece
                getPossibleMovesForNormalPiece(piece, board)
            } else if (isKingPiece(piece)) {
                // get possible moves for king piece
                getPossibleMovesForKingPiece(piece, board)
            } else {
                listOf<Position>(Position(-1, -1))
            }
        }

        private fun getPossibleMovesForNormalPiece(
            piece: Piece,
            board: Array<Array<Piece>>
        ): List<Position> {
            val dirLeft =
                if (piece.value == PIECEVALUE.PLAYER1_NORMAL) DIRECTION.TOPLEFT else DIRECTION.BOTTOMLEFT
            val dirRight =
                if (piece.value == PIECEVALUE.PLAYER1_NORMAL) DIRECTION.TOPRIGHT else DIRECTION.BOTTOMRIGHT
            val possibleMoves = listOf<Position>(
                getPosition(piece, board, dirLeft),
                getPosition(piece, board, dirRight)
            )
            return getValidPossibleMoves(possibleMoves)
        }

        private fun getPossibleMovesForKingPiece(
            piece: Piece,
            board: Array<Array<Piece>>
        ): List<Position> {
            val possibleMoves = listOf<Position>(
                getPosition(piece, board, DIRECTION.TOPLEFT),
                getPosition(piece, board, DIRECTION.TOPRIGHT),
                getPosition(piece, board, DIRECTION.BOTTOMLEFT),
                getPosition(piece, board, DIRECTION.BOTTOMRIGHT)
            )
            return getValidPossibleMoves(possibleMoves)
        }

        private fun getPosition(
            piece: Piece,
            board: Array<Array<Piece>>,
            dir: DIRECTION
        ): Position {
            val x = piece.x + dir.position.x
            val y = piece.y + dir.position.y
            if (!isValidPosition(x, y)) return Position(-1, -1)
            else {
                val otherPiece = board[x][y]
                if ((isPlayer1Piece(piece) && isPlayer2Piece(otherPiece)) || (isPlayer2Piece(piece) && isPlayer1Piece(
                        otherPiece
                    ))
                ) {
                    val x2 = otherPiece.x + dir.position.x
                    val y2 = otherPiece.y + dir.position.y
                    if (!isValidPosition(x2, y2)) return Position(-1, -1)
                    val otherPiece2 = board[x2][y2]
                    return if (otherPiece2.value == PIECEVALUE.EMPTY) Position(x2, y2)
                    else Position(-1, -1)
                } else if (otherPiece.value == PIECEVALUE.EMPTY) {
                    return Position(x, y)
                } else {
                    return Position(-1, -1)
                }
            }
        }

        private fun resetSuggestionsOnBoard(board: Array<Array<Piece>>): Array<Array<Piece>> {
            // remove highlights, selected, movable in board
            for (row in 0 until Constants.BOARD_SIZE) {
                for (col in 0 until Constants.BOARD_SIZE) {
                    val tempPiece = board[row][col].copy()
                    board[row][col] = Piece(x = tempPiece.x, y = tempPiece.y, value = tempPiece.value)
                }
            }
            return board
        }

        fun isMovesPossibleForPlayer(board: Array<Array<Piece>>): Boolean {
            val isPlayer1Turn = isPlayer1Turn()
            for (row in 0 until Constants.BOARD_SIZE) {
                for (col in 0 until Constants.BOARD_SIZE) {
                    val piece = board[row][col]
                    if ((!isPlayer1Turn && isPlayer2Piece(piece)) || (isPlayer1Turn && isPlayer1Piece(piece)) && piece.isMovablePiece)
                        return true
                }
            }
            return false
        }

        fun getSelectedPiece(checkerboard: Array<Array<Piece>>): Piece {
            for (row in 0 until Constants.BOARD_SIZE) {
                for (col in 0 until Constants.BOARD_SIZE) {
                    if (checkerboard[row][col].isSelectedPiece) {
                        return checkerboard[row][col].copy()
                    }
                }
            }
            return Piece(-1, -1, PIECEVALUE.EMPTY)
        }

        private fun getValidPossibleMoves(possibleMoves: List<Position>): List<Position> {
            var validPossibleMoves = listOf<Position>()
            for (possibleMove in possibleMoves) {
                if (isValidPosition(possibleMove.x, possibleMove.y)) validPossibleMoves =
                    validPossibleMoves + listOf(possibleMove)
            }
            return validPossibleMoves
        }


        fun isValidPosition(x: Int, y: Int): Boolean {
            return x in 0 until Constants.BOARD_SIZE && y in 0 until Constants.BOARD_SIZE
        }

        fun isKingPromotion(piece1: Piece, piece2: Piece): Boolean {
            return ((isNormalPiece(piece1) && isEmptyPiece(piece2))
                    && (((isPlayer1Piece(piece1) && piece2.x == 0)
                    || (isPlayer2Piece(piece1) && piece2.x == Constants.BOARD_SIZE - 1))))
        }

        fun makeKingPromotion(piece: Piece): Piece {
            if(isPlayer1Piece(piece)) piece.value = PIECEVALUE.PLAYER1_KING
            else piece.value = PIECEVALUE.PLAYER2_KING
            return piece
        }

        fun doMoveOrJump(piece1: Piece, piece2: Piece, board: Array<Array<Piece>>): Array<Array<Piece>> {
            board[piece2.x][piece2.y] = piece1.copy(x = piece2.x, y = piece2.y)
            board[piece1.x][piece1.y] = Piece(x = piece1.x, y = piece1.y)
            return board
        }

        fun capturePieceAndUpdateBoard(piece1: Piece, piece2: Piece, board: Array<Array<Piece>>): Array<Array<Piece>> {
            val capturedPieceX = abs((piece2.x + piece1.x) / 2).toInt()
            val capturedPieceY = abs((piece2.y + piece1.y) / 2).toInt()
            var capturedPiece = board[capturedPieceX][capturedPieceY].copy()

            // remove piece from board
            capturedPiece.value = PIECEVALUE.EMPTY
            board[capturedPieceX][capturedPieceY] = capturedPiece

            return board
        }

        fun isPlayer1Piece(piece: Piece): Boolean {
            return piece.value == PIECEVALUE.PLAYER1_NORMAL || piece.value == PIECEVALUE.PLAYER1_KING
        }

        fun isPlayer2Piece(piece: Piece): Boolean {
            return piece.value == PIECEVALUE.PLAYER2_NORMAL || piece.value == PIECEVALUE.PLAYER2_KING
        }

        fun isNormalPiece(piece: Piece): Boolean {
            return piece.value == PIECEVALUE.PLAYER1_NORMAL || piece.value == PIECEVALUE.PLAYER2_NORMAL
        }

        fun isKingPiece(piece: Piece): Boolean {
            return piece.value == PIECEVALUE.PLAYER1_KING || piece.value == PIECEVALUE.PLAYER2_KING
        }

        fun isEmptyPiece(piece: Piece): Boolean {
            return piece.value == PIECEVALUE.EMPTY
        }

        fun isPlayer1Turn(): Boolean {
            return if (playerTurn == PLAYERTURN.PLAYER1) return true else false
        }

        fun updatePlayerTurn() {
            playerTurn = if (isPlayer1Turn()) PLAYERTURN.PLAYER2 else PLAYERTURN.PLAYER1
        }

        fun getInitScoreboard(): ScoreBoard {
            playerTurn = PLAYERTURN.PLAYER1
            return ScoreBoard(
                Constants.TOTAL_PIECES * Constants.POINT_PER_PIECE,
                Constants.TOTAL_PIECES * Constants.POINT_PER_PIECE,
                false
            )
        }

        fun getInitCheckerboard(): Array<Array<Piece>> {
            return Array(Constants.BOARD_SIZE) { row ->
                Array(Constants.BOARD_SIZE) { col ->
                    when {
                        row >= Constants.BOARD_SIZE - 3
                                && row < Constants.BOARD_SIZE
                                && (row + col) % 2 != 0 -> Piece(
                            row, col, PIECEVALUE.PLAYER1_NORMAL,
                            isMovablePiece = (row == Constants.BOARD_SIZE - 3),
                            highlightMoveCapturePiece = (row == Constants.BOARD_SIZE - 3)
                        )

                        row < 3 && (row + col) % 2 != 0 -> Piece(
                            row,
                            col,
                            PIECEVALUE.PLAYER2_NORMAL
                        )

                        else -> Piece(row, col, PIECEVALUE.EMPTY)
                    }
                }
            }
        }

        fun getItemColor(item: ITEMVALUE) : Color {
            val itemColorObj = itemColorsList.find{it.item == item}
            return itemColorObj?.color ?: Color.Gray
        }

        fun getItemColorsList(): List<Item> {
            return itemColorsList
        }

        fun setItemColorsList(tempItemColorsList: List<Item>) {
            itemColorsList = tempItemColorsList
        }

        fun getInitialItemColorsList(): List<Item> {
            return listOf(
                Item(ITEMVALUE.PLAYER1, Color(Constants.PLAYER1_COLOR)),
                Item(ITEMVALUE.PLAYER2, Color(Constants.PLAYER2_COLOR)),
                Item(ITEMVALUE.PRIMARY_SQUARE, Color(Constants.PRIMARY_SQUARE_COLOR)),
                Item(ITEMVALUE.SECONDARY_SQUARE, Color(Constants.SECONDARY_SQUARE_COLOR)),
            )
        }
    }
}