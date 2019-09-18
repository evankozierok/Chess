# Chess

This is a Java implementation of chess for two local players on one machine using JavaFX for a graphical interface.

## Installation and Running the Program

### Java 8

For machines with Java 8 installed, simply run the `Chess(12.31.2017)(Java 8).jar` file and the program will begin.

### Java 12

For machines with Java 12 installed, I haven't quite figured out modularity works in Java, and there is no self-contained application to run. 
Make sure JavaFX 12 is also installed. JavaFX can be downloaded [here](https://openjfx.io/).

Then, navigate to the source folder of Chess in your terminal.

Next, run
```
java --module-path "<INSERT_PATH_TO_FX>\javafx-sdk-12.0.2\lib" --add-modules javafx.controls Chess
```
(Obviously, replace `<INSERT_PATH_TO_FX>` with the location where `javafx-sdk-12.0.2` is located.)

You should see the application launch.

## Usage

The program is fairly self-explanatory. The rules of chess can be found [here](http://www.chesscoachonline.com/chess-articles/chess-rules). The program will only allow legal moves and will automatically detect check, checkmate, and stalemate positions.

![Imgur](https://i.imgur.com/f0fs0gV.png "What you see at the start of the program")

Click to pick up a piece of the color whose turn it is. From there, you can click again to either place it in a legal position or back where it originally was. You can also click the right mouse button to return a piece that's been picked up to its original position.

![Imgur](https://i.imgur.com/rWZnhQp.png "A game in action")

Castling can be used by moving the castling king two spaces in the appropriate direction.

When a pawn reaches the back file, a promotion window will pop up - simply select on the piece to promote to.

If you would like to reset the game, simply press the `R` key.

---

If you have any questions, feel free to contact me at evan.kozierok@gmail.com.
