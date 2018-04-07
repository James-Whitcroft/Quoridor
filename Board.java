package Players.TroublingTwosome;
import Interface.Coordinate;
import java.util.HashMap;

/**
 * @author James Whitcroft
 *
 * A class representation of the game board
 */
public class Board {
    private HashMap<Coordinate, Node> map;
    private int size;
    private HashMap<Integer,Hero> players;

    /**
     * construct a new board object
     * @param size an int representing the boards dimensions
     *             size X size
     */
    public Board(int size){
        this.size=size;
        this.map=new HashMap<>();
        this.players=new HashMap<>();
        //nodes are added to map, but they are not connected here
        for(int y=0;y<size;y++){
            for(int x=0;x<size;x++){
                Coordinate c=new Coordinate(y,x);
                Node n=new Node(c);
                this.map.put(c,n);
            }
        }
        this.connectTheDots();
    }


    /**
     * a method to retrieve the size of the board
     * @return an int representing the dimensions of the board
     */
    public int getSize(){
        return this.size;
    }

    /**
     * a method to access the boards layout
     * @return a map of coordinates and their associated nodes
     */
    public HashMap<Coordinate,Node> getMap(){
        return this.map;
    }

    /**
     * a method to return a map of active players
     * @return a hash map containing player id and a hero instance
     */
    public HashMap<Integer,Hero> getPlayers(){
        return this.players;
    }

    /**
     * a method to add a player to the map
     * @param id an int that represents the players unique id
     * @param h a hero object the contains all player info
     */
    public void addPlayers(int id,Hero h){
        this.players.put(id, h);
    }


    /**
     * a method to create edges between nodes
     */
    private void connectTheDots(){
        for(int x=0; x<this.getSize(); x++) {
            for (int y = 0; y < this.getSize(); y++) {
                Coordinate nextRow = new Coordinate(x + 1, y);
                Coordinate nextCol = new Coordinate(x, y + 1);
                //first i add by row node--->node--->node
                if (nextRow.getRow() >= 0 && nextRow.getRow() < this.getSize()) {
                    this.getMap().get(new Coordinate(x, y)).addNeighbor(this.getMap().get(nextRow), this);
                }
                //then i add by column, top to bottom.
                if (nextCol.getCol() >= 0 && nextCol.getCol() < this.getSize()) {
                    this.getMap().get(new Coordinate(x, y)).addNeighbor(this.getMap().get(nextCol), this);
                }
            }
        }
    }
}
