package Players.TroublingTwosome;
import Interface.Coordinate;
import Interface.PlayerMove;

import java.util.*;

/**
 * @author James Whitcroft
 * A class representation of a single player
 */

public class Hero{

    //player id(1...4)
    private int id;
    //number of walls remaining
    private int walls;
    //current position on board
    private Coordinate pos;
    //surrounding nodes
    private List<Coordinate> surroundings=new ArrayList<>();

    private int boardSize;

    //dont even ask about this shit, im sorry, okay?
    private int end;
    private String direction;


    /**
     *Construct a new Hero(player)
     *
     * @param id int representing players id(1-4)
     * @param walls int representing starting number of walls for hero
     * @param c the current coordinate of the hero
     * @param boardSize the int size of the board boardSize x boardSize
     */
    public Hero(int id, int walls, Coordinate c, int boardSize){
        this.id=id;
        this.walls=walls;
        this.pos=c;
        this.boardSize=boardSize;
        this.adjustSurroundings();
        if(c.getCol()==0){
            this.end=boardSize-1;
            this.direction="east";
        }
        if(c.getCol()==boardSize-1){
            this.end=0;
            this.direction="west";
        }
        if(c.getRow()==0){
            this.end=boardSize-1;
            this.direction="south";
        }
        if(c.getRow()==boardSize-1){
            this.end=0;
            this.direction="north";
        }
    }

    /**
     * after a new hero position is set an
     * adjustment must occur in order to maintain a proper list
     * of valid neighboring coordinates
     */
    public void adjustSurroundings(){
        //if hero has surroundings, clear them all
        if(this.surroundings.size()>0) {
            this.surroundings.clear();
        }
        Coordinate n,e,w,s;
        //create new coords, assume they are valid
        n=new Coordinate(this.pos.getRow()-1,this.pos.getCol());
        e=new Coordinate(this.pos.getRow(),this.pos.getCol()+1);
        s=new Coordinate(this.pos.getRow()+1,this.pos.getCol());
        w=new Coordinate(this.pos.getRow(),this.pos.getCol()-1);
        //add coords to a set
        this.surroundings.add(n);
        this.surroundings.add(e);
        this.surroundings.add(s);
        this.surroundings.add(w);
        //check validity of coords in surroundings, remove if invalid
        this.surroundings.removeAll(this.removeInvalid());
    }

    /**
     * marks coordinates as shit to be removed
     *
     * @return a list of shit
     */
    private List<Coordinate> removeInvalid(){
        List<Coordinate> toRemove=new ArrayList<>();
        for(Coordinate c: this.surroundings) {
            if (c.getRow() < 0) {
                toRemove.add(c);
            }
            if (c.getRow() >= this.boardSize) {
                toRemove.add(c);
            }
            if (c.getCol() < 0) {
                toRemove.add(c);
            }
            if (c.getCol() >= this.boardSize) {
                toRemove.add(c);
            }
        }
        return toRemove;
    }
    /**
     * set a new standing position for a hero
     * @param c the coordinate of the new position
     */
    public void setPos(Coordinate c){
        this.pos=c;
    }

    /**
     * @return an int representing the number of walls a player has remaining
     */
    public int getWalls(){
        return this.walls;
    }

    /**
     * method to get current position of hero
     * @return coordinate representing hero's position
     */
    public Coordinate getPos(){
        return this.pos;
    }

    /**
     * method to get the hero's player id
     * @return an int representing the players id
     */
    public int getId(){
        return this.id;
    }

    /**
     * method to decrement the players walls
     */
    public void setWalls(){
        this.walls-=1;
    }

    /**
     * a method to get the surrounding nodes, does not account for walls
     * @return a list of surrounding nodes
     */
    public List<Coordinate> getSurroundings(){
        return this.surroundings;
    }

    /**a method to return the players end location
     * this can represent a row or column
     * so be careful
     *
     * @return an int representing a row or column based on players start point
     * this and the method below this are really poor implementations... sorry :( i am shamed
     */
    public int getEnd(){
        return this.end;
    }

    /**a method to find out where the player is heading
     *
     * @return a String representing the players direction
     */
    public String getDirection(){
        return this.direction;
    }

}