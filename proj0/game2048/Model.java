package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author TODO: YOUR NAME HERE
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;
        board.setViewingPerspective(side);
        int col,row;
        int size=board.size();
        int row_searching;//The index that indicates which row is being checked
        int search_boundary;//The highest row that could be checked, which is used to prevent merging multiple times
        Tile t;
        for(col=0;col<size;col++)
        {
            search_boundary=3;
            for(row=size-2;row>=0;row--)
            {
                if(board.tile(col,row)==null)
                    continue;
                t=board.tile(col,row);
                row_searching=row+1;
                //For every tile that is not empty, the computer will search upwards to find the place the tile can be moved to
                while(true)
                {
                    if(board.tile(col,row_searching)!=null)//The tile meets another tile
                    {
                        if(board.tile(col,row_searching).value()==t.value())//Merge
                        {
                            search_boundary=row_searching-1;//A merged tile should not merge again
                            score+=t.value()+board.tile(col,row_searching).value();
                            board.move(col,row_searching,t);
                        }
                        else //The tile is blocked by another tile
                        {
                            board.move(col,row_searching-1,t);
                        }
                        changed=true;
                        break;
                    }
                    else if(row_searching==search_boundary)//If the boundary is reached, move the tile to it
                    {
                        board.move(col,row_searching,t);
                        changed=true;
                        break;
                    }
                    row_searching++;
                }

            }
        }
        board.setViewingPerspective(Side.NORTH);
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.
        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        int col,row;
        int size=b.size();
        for(row=0;row<size;row++)
        {
            for(col=0;col<size;col++)
            {
                if(b.tile(col,row)==null)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        int col,row;
        int size=b.size();
        for(row=0;row<size;row++)
        {
            for(col=0;col<size;col++)
            {
                if(b.tile(col,row)!=null&&b.tile(col,row).value()==MAX_PIECE)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        if(emptySpaceExists(b))
            return true;
        int col,row;
        int size=b.size();
        for(row=0;row<size;row++)
        {
            for(col=0;col<size;col++)
            {
                if(row==0)
                {
                    if(col==0)
                    {
                        if(b.tile(col,row).value()==b.tile(col+1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row+1).value())
                            return true;
                    }
                    else if(col==size-1)
                    {
                        if(b.tile(col,row).value()==b.tile(col-1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row+1).value())
                            return true;
                    }
                    else
                    {
                        if(b.tile(col,row).value()==b.tile(col-1,row).value()||
                                b.tile(col,row).value()==b.tile(col+1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row+1).value())
                            return true;
                    }
                }
                else if(row==size-1)
                {
                    if(col==0)
                    {
                        if(b.tile(col,row).value()==b.tile(col+1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row-1).value())
                            return true;
                    }
                    else if(col==size-1)
                    {
                        if(b.tile(col,row).value()==b.tile(col-1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row-1).value())
                            return true;
                    }
                    else
                    {
                        if(b.tile(col,row).value()==b.tile(col-1,row).value()||
                                b.tile(col,row).value()==b.tile(col+1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row-1).value())
                            return true;
                    }
                }
                else
                {
                    if(col==0)
                    {
                        if(b.tile(col,row).value()==b.tile(col+1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row-1).value()||
                                b.tile(col,row).value()==b.tile(col,row+1).value())
                            return true;
                    }
                    else if(col==size-1)
                    {
                        if(b.tile(col,row).value()==b.tile(col-1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row-1).value()||
                                b.tile(col,row).value()==b.tile(col,row+1).value())
                            return true;
                    }
                    else
                    {
                        if(b.tile(col,row).value()==b.tile(col-1,row).value()||
                                b.tile(col,row).value()==b.tile(col+1,row).value()||
                                b.tile(col,row).value()==b.tile(col,row-1).value()||
                                b.tile(col,row).value()==b.tile(col,row+1).value())
                            return true;
                    }
                }
            }
        }
        return false;
    }


    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
