import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Arrays;

/* CURRENT BUGS:
 * Castling probably still bugged somehow?
 */

class Piece
{
    private char color; // B = black; W = white, X = empty
    private char type; // B = bishop, K = king, N = knight, P = pawn, Q = queen, R = rook, X = empty
    private boolean pickedUp, moved; // moved is relevant for pawns (first move can be 2 spaces), rooks+kings (castling)
	private boolean justMoved2 = false; // for en passant

    public Piece(char c, char t, boolean p, boolean m)
    {
        setValues(c, t, p, m);
    }

	public Piece(Piece p)
	{
		setValues(p.getColor(), p.getType(), p.isPickedUp(), p.hasMoved());
	}

    public void setValues(char c, char t, boolean p, boolean m)
    {
        color = c;
        type = t;
        pickedUp = p;
        moved = m;
    }

    public void setColor(char c)
    {
        color = c;
    }
    public void setType(char t)
    {
        type = t;
    }
    public void setPickedUp(boolean p)
    {
        pickedUp = p;
    }
    public void setMoved(boolean m)
    {
        moved = m;
    }
	public void setJustMoved2(boolean j)
	{
		justMoved2 = j;
	}

    public char getColor()
    {
        return color;
    }
    public char getType()
    {
        return type;
    }
    public boolean isPickedUp()
    {
        return pickedUp;
    }
    public boolean hasMoved()
    {
        return moved;
    }
	public boolean hasJustMoved2()
	{
		return justMoved2;
	}

    public Image getImage()
    {
        String fileName = "";
        fileName += color;
        fileName += type;
        fileName += ".png";
        Image img = new Image(fileName);
        return img;
    }

    public String toString()
    {
        return color + "" + type;
    }
}

class RowCol
{
	private int row, col;

	public RowCol(int r, int c)
	{
		setValues(r, c);
	}

	public void setValues(int r, int c)
	{
		row = r;
		col = c;
	}

	public int getRow()
	{
		return row;
	}

	public int getCol()
	{
		return col;
	}
}

public class Chess extends Application
{
    private Piece[][] board;
    private boolean isWhiteTurn, pawnIsWhite, isStalemate;
	private double mouseX, mouseY;
	private int pawnRow, pawnCol;
	private char checkedPlayer, checkmatedPlayer;
	/*
	public static void main(String[] args) {
		launch(args);
	}
	*/
    @Override
    public void start(Stage stage)
    {
		//System.out.println(Font.getFamilies().toString());
        Group root = new Group();
        Scene scene = new Scene(root);
		Group pawnRoot = new Group();
		Scene pawnScene = new Scene(pawnRoot);

        Canvas canvas = new Canvas(800,600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
		
		Stage pawnStage = new Stage();
		Canvas pawnCanvas = new Canvas(300, 120);
		GraphicsContext pawnGc = pawnCanvas.getGraphicsContext2D();
		
		pawnStage.setTitle("Pawn Promotion");
		pawnStage.setScene(pawnScene);
		pawnStage.initOwner(stage);
		pawnStage.initModality(Modality.WINDOW_MODAL); // makes the pawn promotion window focus - you can't interact with the board until you choose a piece to promote to
		pawnStage.initStyle(StageStyle.UNDECORATED);

        board = buildBoard();
        isWhiteTurn = true;
		pawnIsWhite = true;
		isStalemate = false;
		checkedPlayer = 'X';
		checkmatedPlayer = 'X';

		AnimationTimer pawnDialogue = new AnimationTimer()
		{
			@Override
			public void handle(long now)
			{
				pawnGc.clearRect(0,0,300,120);
				pawnGc.strokeRect(0,0,300,120);
				if (pawnIsWhite)
				{
					pawnGc.drawImage(new Image("WN.png"), 30, 30);
					pawnGc.drawImage(new Image("WB.png"), 90, 30);
					pawnGc.drawImage(new Image("WR.png"), 150, 30);
					pawnGc.drawImage(new Image("WQ.png"), 210, 30);
				}
				else
				{
					pawnGc.drawImage(new Image("BN.png"), 30, 30);
					pawnGc.drawImage(new Image("BB.png"), 90, 30);
					pawnGc.drawImage(new Image("BR.png"), 150, 30);
					pawnGc.drawImage(new Image("BQ.png"), 210, 30);
				}
			}
		};
		
        scene.addEventHandler(MouseEvent.MOUSE_CLICKED,
        new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent e)
            {
                //System.out.println("getX: "+e.getX());
                //System.out.println("getY: "+e.getY());
                RowCol rc = findPickedUpPiece(board); // gives row/col position of the picked up piece
                if (e.getButton() == MouseButton.SECONDARY && rc != null)
                {
                    board[rc.getRow()][rc.getCol()].setPickedUp(false);
                    return;
                }

				if (e.getX() < 60 || e.getY() < 60)
				{
					return; //exits if the click is outside the board (on the top or left margins)
				}
                int boardCol = (int)(e.getX() - 60)/60;
                int boardRow = (int)(e.getY() - 60)/60;

                /*System.out.println("boardRow: "+boardRow);
                System.out.println("boardCol: "+boardCol);*/
				if (boardRow > 7 || boardCol > 7 || boardRow < 0 || boardCol < 0)
				{
					return; // exits if the click is outside the board
				}
                if (rc == null) // if there's no piece in hand...
                {
					if (!board[boardRow][boardCol].toString().equals("XX")) // and if the selected piece isn't empty...
                    {
						if (isWhiteTurn && board[boardRow][boardCol].getColor() == 'W') // and if it's white's turn and the piece is white...
						{
							board[boardRow][boardCol].setPickedUp(true); // pick it up
						}
						else if (!isWhiteTurn && board[boardRow][boardCol].getColor() == 'B') // else if it's black's turn and the piece is black
						{
							board[boardRow][boardCol].setPickedUp(true); // pick it up
						}
					}
					//displayBoard(board);
                }
                else // if there is...
                {
                    if (rc.getRow() == boardRow && rc.getCol() == boardCol) // put the piece back down in the same place
                    {
                        board[boardRow][boardCol].setPickedUp(false);
                        return;
                    }
                    boolean[][] validMoves = validMovesForPiece(board, rc);
                    if (validMoves[boardRow][boardCol])
                    {
						//Castling
						if (board[rc.getRow()][rc.getCol()].getType() == 'K')
						{
							RowCol left2 = new RowCol(rc.getRow(), rc.getCol()-2);
							RowCol left1 = new RowCol(rc.getRow(), rc.getCol()-1);
							RowCol right2 = new RowCol(rc.getRow(), rc.getCol()+2);
							RowCol right1 = new RowCol(rc.getRow(), rc.getCol()+1);
							if (board[rc.getRow()][rc.getCol()].getColor() == 'W') //White
							{
								//Kingside
								if (!board[rc.getRow()][rc.getCol()].hasMoved() && board[7][7].getType() == 'R' && !board[7][7].hasMoved() && board[7][5].toString().equals("XX") && board[7][6].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, right1, 'W') && !isUnderAttackCastle(board, right2, 'W') && boardRow == 7 && boardCol == 6)
								{
									board[7][5] = new Piece('W','R',false,true);
									board[7][7].setValues('X', 'X', false, false);
								}
								//Queenside
								if (!board[rc.getRow()][rc.getCol()].hasMoved() && board[7][0].getType() == 'R' && !board[7][0].hasMoved() && board[7][1].toString().equals("XX") && board[7][2].toString().equals("XX") && board[7][3].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, left1, 'W') && !isUnderAttackCastle(board, left2, 'W') && boardRow == 7 && boardCol == 2)
								{
									board[7][3] = new Piece('W','R',false,true);
									board[7][0].setValues('X', 'X', false, false);
								}
							}
							if (board[rc.getRow()][rc.getCol()].getColor() == 'B') //Black
							{
								//Kingside
								if (!board[rc.getRow()][rc.getCol()].hasMoved() && board[0][7].getType() == 'R' && !board[0][7].hasMoved() && board[0][5].toString().equals("XX") && board[0][6].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, right1, 'B') && !isUnderAttackCastle(board, right2, 'B') && boardRow == 0 && boardCol == 6)
								{
									board[0][5] = new Piece('B','R',false,true);
									board[0][7].setValues('X', 'X', false, false);
								}
								//Queenside
								if (!board[rc.getRow()][rc.getCol()].hasMoved() && board[0][0].getType() == 'R' && !board[0][0].hasMoved() && board[0][1].toString().equals("XX") && board[0][2].toString().equals("XX") && board[0][3].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, left1, 'B') && !isUnderAttackCastle(board, left2, 'B') && boardRow == 0 && boardCol == 2)
								{
									board[0][3] = new Piece('B','R',false,true);
									board[0][0].setValues('X', 'X', false, false);
								}
							}
						}
                        /*//Castling 
                        if (board[rc.getRow()][rc.getCol()].toString().equals("WK") && boardRow == 7 && boardCol == 6)
                        {
                            board[7][5] = new Piece('W','R',false,true);
                            board[7][7].setValues('X', 'X', false, false);
                        }
                        else if (board[rc.getRow()][rc.getCol()].toString().equals("WK") && boardRow == 7 && boardCol == 2)
                        {
                            board[7][3] = new Piece('W','R',false,true);
                            board[7][0].setValues('X', 'X', false, false);
                        }
                        else if (board[rc.getRow()][rc.getCol()].toString().equals("BK") && boardRow == 0 && boardCol == 6)
                        {
                            board[0][5] = new Piece('B','R',false,true);
                            board[0][7].setValues('X', 'X', false, false);
                        }
                        else if (board[rc.getRow()][rc.getCol()].toString().equals("BK") && boardRow == 0 && boardCol == 2)
                        {
                            board[0][3] = new Piece('B','R',false,true);
                            board[0][0].setValues('X', 'X', false, false);
                        }*/
                        //moving normal pieces
                        Piece oldPiece = new Piece(board[rc.getRow()][rc.getCol()]); // clones the picked up piece
						Piece destinationPiece = new Piece(board[boardRow][boardCol]); // clones the destination piece
                        board[boardRow][boardCol] = new Piece(oldPiece); // clones the oldPiece to the clicked location
                        board[boardRow][boardCol].setPickedUp(false);
                        board[boardRow][boardCol].setMoved(true);
	                    board[rc.getRow()][rc.getCol()].setValues('X', 'X', false, false); // sets the picked up piece to be empty

                        if (isUnderAttack(board, findKing(board, board[boardRow][boardCol].getColor())))
                        { // if moving would put/keep your king in check, revert to old game state
                            //System.out.println("Invalid move - this would place YOUR king in check!");
                            board[rc.getRow()][rc.getCol()] = new Piece(oldPiece);
                            board[boardRow][boardCol] = new Piece(destinationPiece);
							return; // ends this move
                        }
						
						//Pawn Promotion
						if (board[boardRow][boardCol].getType() == 'P' && (boardRow == 0 || boardRow == 7)) // if we move a pawn to the back rank...
						{
							pawnRow = boardRow;
							pawnCol = boardCol;
							pawnIsWhite = (board[boardRow][boardCol].getColor() == 'W');
							pawnDialogue.start();
							pawnStage.show();
						}
    					//displayBoard(board);
						
						// En Passant
						// Kills pieces that have been "en passant-ed"
						if (board[boardRow][boardCol].getType() == 'P')
						{
							if (board[boardRow][boardCol].getColor() == 'W' && board[boardRow+1][boardCol].hasJustMoved2())
							{
								board[boardRow+1][boardCol].setValues('X', 'X', false, false);
							}
							if (board[boardRow][boardCol].getColor() == 'B' && board[boardRow-1][boardCol].hasJustMoved2())
							{
								board[boardRow-1][boardCol].setValues('X', 'X', false, false);
							}
						}
						// Removes justMoved2 from all pieces
						for (Piece[] row : board)
						{
							for (Piece col : row)
							{
								if (col.hasJustMoved2())
								{
									col.setJustMoved2(false);
								}
							}
						}
						// Gives justMoved2 to an appropriate pawn
						if (board[boardRow][boardCol].getType() == 'P' && !oldPiece.hasMoved())
						{
							if (board[boardRow][boardCol].getColor() == 'W' && boardRow == 4)
							{
								board[boardRow][boardCol].setJustMoved2(true);
							}
							else if (board[boardRow][boardCol].getColor() == 'B' && boardRow == 3)
							{
								board[boardRow][boardCol].setJustMoved2(true);
							}
						}
						
						checkedPlayer = 'X'; // if you've made it here, you can't be in check
						
						// Checkmate and Stalemate
						if (noValidMoves(board, reverseColor(board[boardRow][boardCol].getColor())))
						{
							if (isUnderAttack(board, findKing(board, reverseColor(board[boardRow][boardCol].getColor()))))
							{
								//System.out.println("Checkmate! "+board[boardRow][boardCol].getColor()+" wins!");
								checkmatedPlayer = reverseColor(board[boardRow][boardCol].getColor());
							}
							else
							{
								//System.out.println("Stalemate! There are no valid moves for "+reverseColor(board[boardRow][boardCol].getColor())+".");
								isStalemate = true;
							}
						}
						else if (isUnderAttack(board, findKing(board, reverseColor(board[boardRow][boardCol].getColor())))) // if you've put the opposite color in check
						{
							//System.out.println("Good move! Your opponent is now in check.");
							if (isWhiteTurn)
							{
								checkedPlayer = 'B';
							}
							else
							{
								checkedPlayer = 'W';
							}
						}
						isWhiteTurn = !isWhiteTurn;
                    }
                }
				
            }
        });

		scene.addEventHandler(MouseEvent.MOUSE_MOVED,
		new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent e)
			{ // tracks mouse movements
				mouseX = e.getX();
				mouseY = e.getY();
			}
		});
		
		scene.addEventHandler(KeyEvent.KEY_RELEASED,
		new EventHandler<KeyEvent>()
		{
			@Override
			public void handle(KeyEvent t)
			{
				if (t.getCode() == KeyCode.R)
				{
					board = buildBoard();
					isWhiteTurn = true;
					pawnIsWhite = true;
					isStalemate = false;
					checkedPlayer = 'X';
					checkmatedPlayer = 'X';
				}
			}
		});
		pawnScene.addEventHandler(MouseEvent.MOUSE_CLICKED,
		new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent e)
			{
				if (!(e.getX() < 30 || e.getX() > 270 || e.getY() < 30 || e.getY() > 90)) // if you clicked on one of the pieces
				{
					if (e.getX() < 90)
					{
						board[pawnRow][pawnCol].setType('N');
					}
					else if (e.getX() < 150)
					{
						board[pawnRow][pawnCol].setType('B');
					}
					else if (e.getX() < 210)
					{
						board[pawnRow][pawnCol].setType('R');
					}
					else
					{
						board[pawnRow][pawnCol].setType('Q');
					}
					pawnStage.close();
					pawnDialogue.start();
				}
			}
		});

        root.getChildren().add(canvas);
        canvas.setVisible(true);
		pawnRoot.getChildren().add(pawnCanvas);
		pawnCanvas.setVisible(true);

        stage.setTitle("Chess");
        stage.setScene(scene);
        stage.show();

        new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
				gc.setFont(Font.font("Copperplate Gothic Bold", 30.0));
				gc.clearRect(0,0,800,600);
                gc.setFill(Color.TAN);
                gc.fillRect(60,60,480,480);
                gc.setFill(Color.BROWN);
                for (int x = 60; x < 540; x += 60)
                {
                    int y;
                    if (x % 120 == 0)
                    {
                        y = 60;
                    }
                    else
                    {
                        y = 120;
                    }
                    for (; y < 540; y += 120)
                    {
                        gc.fillRect(x, y, 60, 60);
                    }
                }

                for (int row = 0; row < 8; row++)
                {
                    for (int col = 0; col < 8; col++)
                    {
                        if (board[row][col].isPickedUp())
                        {
                            Image empty = new Image("XX.png");
                            gc.drawImage(empty, col*60 + 60, row*60 + 60);
                        }
                        else
                        {
                            gc.drawImage(board[row][col].getImage(), col*60 + 60, row*60 + 60);
                        }
                    }
                }
				gc.setFill(Color.BLACK);
				
				if (isWhiteTurn)
				{
					gc.fillText("White to move", 180, 45);
				}
				else
				{
					gc.fillText("Black to move", 180, 45);
				}
				if (checkedPlayer != 'X')
				{
					gc.fillText(checkedPlayer+" is in check!", 550, 310);
				}
				else if (checkmatedPlayer != 'X')
				{
					gc.fillText("Checkmate! ", 550, 295);
					gc.fillText(reverseColor(checkmatedPlayer)+" wins!", 550, 320);
				}
				else if (isStalemate)
				{
					gc.fillText("Stalemate!", 550, 310);
				}
				
				RowCol rc = findPickedUpPiece(board);
				if (rc != null)
				{
					gc.drawImage(board[rc.getRow()][rc.getCol()].getImage(), mouseX - 30, mouseY - 30);
				}
				
            }
        }.start();
    }

    public static Piece[][] buildBoard()
    {
        Piece[][] board = new Piece[8][8];

		//4 Empty Rows
        for (int row = 2; row < 6; row++)
		{
			for (int col = 0; col < 8; col++)
			{
				board[row][col] = new Piece('X', 'X', false, false);
			}
		}
        //Rooks
        board[0][0] = new Piece('B', 'R', false, false);
        board[0][7] = new Piece('B', 'R', false, false);
        board[7][0] = new Piece('W', 'R', false, false);
        board[7][7] = new Piece('W', 'R', false, false);
        //Knights
        board[0][1] = new Piece('B', 'N', false, false);
        board[0][6] = new Piece('B', 'N', false, false);
        board[7][1] = new Piece('W', 'N', false, false);
        board[7][6] = new Piece('W', 'N', false, false);
        //Bishops
        board[0][2] = new Piece('B', 'B', false, false);
        board[0][5] = new Piece('B', 'B', false, false);
        board[7][2] = new Piece('W', 'B', false, false);
        board[7][5] = new Piece('W', 'B', false, false);
        //Queens
        board[0][3] = new Piece('B', 'Q', false, false);
        board[7][3] = new Piece('W', 'Q', false, false);
        //Kings
        board[0][4] = new Piece('B', 'K', false, false);
        board[7][4] = new Piece('W', 'K', false, false);
        //Pawns
        for (int i = 0; i < 8; i++)
        {
            board[1][i] = new Piece('B', 'P', false, false);
            board[6][i] = new Piece('W', 'P', false, false);
        }

        return board;
    }

    public RowCol findPickedUpPiece(Piece[][] board)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                if (board[row][col].isPickedUp())
                {
                    RowCol rc = new RowCol(row, col);
					return rc;
                }
            }
        }
        return null;
    }

	public void displayBoard(Piece[][] board)
	{
		for (Piece[] row : board)
        {
            for (Piece col : row)
            {
                System.out.print(col.toString()+" ");
            }
            System.out.println();
        }
		System.out.println("------------------------");
	}

    public boolean[][] validMovesForPiece(Piece[][] board, RowCol rc)
    {
        boolean[][] moves = new boolean[8][8];
        Piece currentPiece = board[rc.getRow()][rc.getCol()];

        moves[rc.getRow()][rc.getCol()] = true; // lets you put the piece back down where it was before

        if (currentPiece.getType() == 'P') // Pawn movement
        {
            if (currentPiece.getColor() == 'W') // White pawn
            {
                if (inBounds(rc.getRow()-1, rc.getCol()) && board[rc.getRow()-1][rc.getCol()].toString().equals("XX")) // if the space ahead is empty
                {
                    moves[rc.getRow()-1][rc.getCol()] = true;
                    if (inBounds(rc.getRow()-2, rc.getCol()) && !currentPiece.hasMoved() && board[rc.getRow()-2][rc.getCol()].toString().equals("XX")) // if the space 2 ahead is empty AND the pawn hasn't moved yet (AND the piece 1 ahead is empty too)
                    {
                        moves[rc.getRow()-2][rc.getCol()] = true; // first movement can be 2 spaces
                    }
                }

                if (inBounds(rc.getRow()-1, rc.getCol()-1) && board[rc.getRow()-1][rc.getCol()-1].getColor() == 'B') // if there's a Black piece to the top left
                {
                    moves[rc.getRow()-1][rc.getCol()-1] = true;
                }
                if (inBounds(rc.getRow()-1, rc.getCol()+1) && board[rc.getRow()-1][rc.getCol()+1].getColor() == 'B') // if there's a Black piece to the top right
                {
                    moves[rc.getRow()-1][rc.getCol()+1] = true;
                }
				if (inBounds(rc.getRow(), rc.getCol()-1) && board[rc.getRow()][rc.getCol()-1].hasJustMoved2() && board[rc.getRow()][rc.getCol()-1].getColor() == 'B')
				{ // en passant: top left
					moves[rc.getRow()-1][rc.getCol()-1] = true;
				}
				if (inBounds(rc.getRow(), rc.getCol()+1) && board[rc.getRow()][rc.getCol()+1].hasJustMoved2() && board[rc.getRow()][rc.getCol()+1].getColor() == 'B')
				{ // en passant: top right
					moves[rc.getRow()-1][rc.getCol()+1] = true;
				}
            }
            else // Black pawn
            {
                if (inBounds(rc.getRow()+1, rc.getCol()) && board[rc.getRow()+1][rc.getCol()].toString().equals("XX")) // if the space ahead is empty
                {
                    moves[rc.getRow()+1][rc.getCol()] = true;
                    if (inBounds(rc.getRow()+2, rc.getCol()) && !currentPiece.hasMoved() && board[rc.getRow()+2][rc.getCol()].toString().equals("XX")) // if the space 2 ahead is empty AND the pawn hasn't moved yet (AND the piece 1 ahead is empty too)
                    {
                        moves[rc.getRow()+2][rc.getCol()] = true; // first movement can be 2 spaces
                    }
                }

                if (inBounds(rc.getRow()+1, rc.getCol()-1) && board[rc.getRow()+1][rc.getCol()-1].getColor() == 'W') // White piece to the bottom left
                {
                    moves[rc.getRow()+1][rc.getCol()-1] = true;
                }
                if (inBounds(rc.getRow()+1, rc.getCol()+1) && board[rc.getRow()+1][rc.getCol()+1].getColor() == 'W') // White piece to the bottom right
                {
                    moves[rc.getRow()+1][rc.getCol()+1] = true;
                }
				if (inBounds(rc.getRow(), rc.getCol()-1) && board[rc.getRow()][rc.getCol()-1].hasJustMoved2() && board[rc.getRow()][rc.getCol()-1].getColor() == 'W')
				{ // en passant: bottom left
					moves[rc.getRow()+1][rc.getCol()-1] = true;
				}
				if (inBounds(rc.getRow(), rc.getCol()+1) && board[rc.getRow()][rc.getCol()+1].hasJustMoved2() && board[rc.getRow()][rc.getCol()+1].getColor() == 'W')
				{ // en passant: bottom right
					moves[rc.getRow()+1][rc.getCol()+1] = true;
				}
            }
        }
        else if (currentPiece.getType() == 'R' || currentPiece.getType() == 'Q') //Rooks AND QUEENS (which have the moves of rook+bishop)
        {
            //up movement
            int mutRow = rc.getRow() - 1;
            int mutCol = rc.getCol();
            while (mutRow >= 0)
            {
                if (board[mutRow][mutCol].toString().equals("XX")) // mutRow starts 1 less (above) the piece
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // up movement now blocked
                }
                mutRow--;
            }
            //down movement
            mutRow = rc.getRow() + 1;
            mutCol = rc.getCol();
            while (mutRow <= 7)
            {
                if (board[mutRow][mutCol].toString().equals("XX")) // mutRow starts 1 greater (below) the piece
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // down movement now blocked
                }
                mutRow++;
            }
            //left movement
            mutRow = rc.getRow();
            mutCol = rc.getCol() - 1;
            while (mutCol >= 0)
            {
                if (board[mutRow][mutCol].toString().equals("XX"))
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // left movement now blocked
                }
                mutCol--;
            }
            //right movement
            mutRow = rc.getRow();
            mutCol = rc.getCol() + 1;
            while (mutCol <= 7)
            {
                if (board[mutRow][mutCol].toString().equals("XX"))
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // right movement now blocked
                }
                mutCol++;
            }
        }
        if (currentPiece.getType() == 'B' || currentPiece.getType() == 'Q') // Bishops AND QUEENS (note: not an else if because then queens would not have bishop movement)
        {
            //up left movement
            int mutRow = rc.getRow() - 1;
            int mutCol = rc.getCol() - 1;
            while (mutRow >= 0 && mutCol >= 0)
            {
                if (board[mutRow][mutCol].toString().equals("XX"))
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // up left movement now blocked
                }
                mutRow--;
                mutCol--;
            }
            //down left movement
            mutRow = rc.getRow() + 1;
            mutCol = rc.getCol() - 1;
            while (mutRow <= 7 && mutCol >= 0)
            {
                if (board[mutRow][mutCol].toString().equals("XX"))
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // down left movement now blocked
                }
                mutRow++;
                mutCol--;
            }
            //up right movement
            mutRow = rc.getRow() - 1;
            mutCol = rc.getCol() + 1;
            while (mutRow >= 0 && mutCol <= 7)
            {
                if (board[mutRow][mutCol].toString().equals("XX"))
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // up right movement now blocked
                }
                mutRow--;
                mutCol++;
            }
            //down right movement
            mutRow = rc.getRow() + 1;
            mutCol = rc.getCol() + 1;
            while (mutRow <= 7 && mutCol <= 7)
            {
                if (board[mutRow][mutCol].toString().equals("XX"))
                {
                    moves[mutRow][mutCol] = true;
                }
                else // space not empty
                {
                    if (board[mutRow][mutCol].getColor() != currentPiece.getColor()) // Different color: can capture!
                    {
                        moves[mutRow][mutCol] = true;
                    }
                    break; // down right movement now blocked
                }
                mutRow++;
                mutCol++;
            }
        }
        else if (currentPiece.getType() == 'N') //Knights
        {
            if (inBounds(rc.getRow()-2, rc.getCol()-1) && board[rc.getRow()-2][rc.getCol()-1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-2][rc.getCol()-1] = true;
            }
            if (inBounds(rc.getRow()-2, rc.getCol()+1) && board[rc.getRow()-2][rc.getCol()+1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-2][rc.getCol()+1] = true;
            }
            if (inBounds(rc.getRow()+2, rc.getCol()-1) && board[rc.getRow()+2][rc.getCol()-1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+2][rc.getCol()-1] = true;
            }
            if (inBounds(rc.getRow()+2, rc.getCol()+1) && board[rc.getRow()+2][rc.getCol()+1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+2][rc.getCol()+1] = true;
            }
            if (inBounds(rc.getRow()+1, rc.getCol()-2) && board[rc.getRow()+1][rc.getCol()-2].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+1][rc.getCol()-2] = true;
            }
            if (inBounds(rc.getRow()+1, rc.getCol()+2) && board[rc.getRow()+1][rc.getCol()+2].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+1][rc.getCol()+2] = true;
            }
            if (inBounds(rc.getRow()-1, rc.getCol()-2) && board[rc.getRow()-1][rc.getCol()-2].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-1][rc.getCol()-2] = true;
            }
            if (inBounds(rc.getRow()-1, rc.getCol()+2) && board[rc.getRow()-1][rc.getCol()+2].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-1][rc.getCol()+2] = true;
            }
        }
        else if (currentPiece.getType() == 'K') // Kings
        {
            if (inBounds(rc.getRow()-1, rc.getCol()-1) && board[rc.getRow()-1][rc.getCol()-1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-1][rc.getCol()-1] = true;
            }
            if (inBounds(rc.getRow()-1, rc.getCol()) && board[rc.getRow()-1][rc.getCol()].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-1][rc.getCol()] = true;
            }
            if (inBounds(rc.getRow()-1, rc.getCol()+1) && board[rc.getRow()-1][rc.getCol()+1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()-1][rc.getCol()+1] = true;
            }
            if (inBounds(rc.getRow(), rc.getCol()-1) && board[rc.getRow()][rc.getCol()-1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()][rc.getCol()-1] = true;
            }
            if (inBounds(rc.getRow(), rc.getCol()+1) && board[rc.getRow()][rc.getCol()+1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()][rc.getCol()+1] = true;
            }
            if (inBounds(rc.getRow()+1, rc.getCol()-1) && board[rc.getRow()+1][rc.getCol()-1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+1][rc.getCol()-1] = true;
            }
            if (inBounds(rc.getRow()+1, rc.getCol()) && board[rc.getRow()+1][rc.getCol()].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+1][rc.getCol()] = true;
            }
            if (inBounds(rc.getRow()+1, rc.getCol()+1) && board[rc.getRow()+1][rc.getCol()+1].getColor() != currentPiece.getColor())
            {
                moves[rc.getRow()+1][rc.getCol()+1] = true;
            }
            //castling
			RowCol left2 = new RowCol(rc.getRow(), rc.getCol()-2);
			RowCol left1 = new RowCol(rc.getRow(), rc.getCol()-1);
			RowCol right2 = new RowCol(rc.getRow(), rc.getCol()+2);
			RowCol right1 = new RowCol(rc.getRow(), rc.getCol()+1);
            if (currentPiece.getColor() == 'W') //White
            {
                //Kingside
                if (!currentPiece.hasMoved() && board[7][7].getType() == 'R' && !board[7][7].hasMoved() && board[7][5].toString().equals("XX") && board[7][6].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, right1, 'W') && !isUnderAttackCastle(board, right2, 'W'))
                {
                    moves[7][6] = true;
                }
                //Queenside
                if (!currentPiece.hasMoved() && board[7][0].getType() == 'R' && !board[7][0].hasMoved() && board[7][1].toString().equals("XX") && board[7][2].toString().equals("XX") && board[7][3].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, left1, 'W') && !isUnderAttackCastle(board, left2, 'W'))
                {
                    moves[7][2] = true;
                }
            }
            if (currentPiece.getColor() == 'B') //Black
            {
                //Kingside
                if (!currentPiece.hasMoved() && board[0][7].getType() == 'R' && !board[0][7].hasMoved() && board[0][5].toString().equals("XX") && board[0][6].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, right1, 'B') && !isUnderAttackCastle(board, right2, 'B'))
                {
                    moves[0][6] = true;
                }
                //Queenside
                if (!currentPiece.hasMoved() && board[0][0].getType() == 'R' && !board[0][0].hasMoved() && board[0][1].toString().equals("XX") && board[0][2].toString().equals("XX") && board[0][3].toString().equals("XX") && !isUnderAttack(board, rc) && !isUnderAttackCastle(board, left1, 'B') && !isUnderAttackCastle(board, left2, 'B'))
                {
                    moves[0][2] = true;
                }
            }
        }

        return moves;
    }

    public boolean inBounds(int row, int col)
    {
        return !(row < 0 || row > 7 || col < 0 || col > 7);
    }

    public boolean isUnderAttack(Piece[][] board, RowCol rc)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                if (row == rc.getRow() && col == rc.getCol()) // skip the square for testing itself
                {
                    continue;
                }
                RowCol testPiece = new RowCol(row, col);
				if (board[testPiece.getRow()][testPiece.getCol()].getColor() == 'X') // empty spaces can't attack
				{
					continue;
				}
				if (board[testPiece.getRow()][testPiece.getCol()].getColor() == board[rc.getRow()][rc.getCol()].getColor()) // skip same color attacks
				{
					continue;
				}
                if (validMovesForPiece(board, testPiece)[rc.getRow()][rc.getCol()])
                {
                    return true;
                }
            }
        }
        return false;
    }
	
	public boolean isUnderAttackCastle(Piece[][] board, RowCol rc, char kingColor)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                RowCol testPiece = new RowCol(row, col);
				if (board[testPiece.getRow()][testPiece.getCol()].getColor() == 'X') // empty spaces can't attack
				{
					continue;
				}
				if (board[testPiece.getRow()][testPiece.getCol()].getColor() == kingColor) // skip same color attacks
				{
					continue;
				}
                if (validMovesForPiece(board, testPiece)[rc.getRow()][rc.getCol()])
                {
                    return true;
                }
            }
        }
        return false;
    }
	
    public RowCol findKing(Piece[][] board, char color)
    {
        for (int row = 0; row < 8; row++)
        {
            for (int col = 0; col < 8; col++)
            {
                if (board[row][col].getType() == 'K' && board[row][col].getColor() == color)
                {
                    RowCol pos = new RowCol(row, col);
                    return pos;
                }
            }
        }
        return null;
    }
	
	public boolean noValidMoves(Piece[][] board, char color)
	{
		Piece[][] mutBoard = new Piece[8][8];
		for (int row = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++)
			{
				mutBoard[row][col] = new Piece(board[row][col]); // clones the board
			}
		}
		for (int row = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++)
			{
				if (mutBoard[row][col].getColor() != color) // skips opposite color pieces (and empty spaces)
				{
					continue;
				}
				boolean[][] validMoves = validMovesForPiece(mutBoard, new RowCol(row, col));
				Piece oldPiece = new Piece(mutBoard[row][col]);
				for (int mrow = 0; mrow < 8; mrow++)
				{
					for (int mcol = 0; mcol < 8; mcol++)
					{
						if (mrow == row && mcol == col) // skips putting the piece back down: that obviously doesn't help
						{
							continue;
						}
						if (validMoves[mrow][mcol])
						{
							Piece destinationPiece = new Piece(mutBoard[mrow][mcol]);
							mutBoard[mrow][mcol] = new Piece(oldPiece);
							mutBoard[row][col].setValues('X', 'X', false, false);
							if (!isUnderAttack(mutBoard, findKing(mutBoard, color))) // if this move would keep you out of check...
							{
								return false;
							}
							mutBoard[row][col] = new Piece(oldPiece); // resets the board to its original state assuming that you're in check
							mutBoard[mrow][mcol] = new Piece(destinationPiece);
						}
					}
				}
			}
		}
		return true;
	}
	
	public char reverseColor(char color)
	{
		if (color == 'B')
		{
			return 'W';
		}
		if (color == 'W')
		{
			return 'B';
		}
		return color; // this is most likely 'X'
	}
}