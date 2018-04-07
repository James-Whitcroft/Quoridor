package Players.TroublingTwosome;
import Interface.Coordinate;
import java.util.ArrayList;

/**
 * @author James Whitcroft
 *
 * A node class to represent squares on the playing board
 */
public class Node {
    private ArrayList<Node> neighbors=new ArrayList<>();
    private Coordinate position;

    /**
     * Construct a new node
     * @param y the nodes coordinate
     */
    public Node(Coordinate y){
        this.position=y;
    }

    /**a method to add a neighbor to nodes list of neighbors
     *
     * @param neighbor a node to be added to neighbors
     * @param board a board object in which the nodes exist(used for size)
     */
    public void addNeighbor(Node neighbor, Board board){
        //check if neighbor is a valid coordinate
        if(neighbor.position.getRow()>=0 && neighbor.position.getRow()<board.getSize()
                && neighbor.position.getCol()>=0 && neighbor.position.getCol()<board.getSize()) {
            //check if neighbor is already a neighbor, if not add each other as neighbors
            if(!this.neighbors.contains(neighbor)) {
                this.neighbors.add(neighbor);
                neighbor.neighbors.add(this);
            }
        }
    }

    /**
     * a method to remove a neighbor from a nodes neighbor list
     * @param neighbor the node to be removed
     */
    public void removeNeighbor(Node neighbor){
        //if both nodes exist as ech others neighbor,
        //remove both nodes from each others neighbor list
        if(this.getNeighbors().contains(neighbor) && neighbor.getNeighbors().contains(this)){
            this.neighbors.remove(neighbor);
            neighbor.neighbors.remove(this);
        }
    }

    /**
     * a method to return the position of the node in the graph
     * @return a coordinate for the given nodes position
     */
    public Coordinate getPosition(){
        return this.position;
    }

    /**
     * a method to retrieve a nodes neighbors
     * @return a list of node objects connected to given node
     */
    public ArrayList<Node> getNeighbors(){
        return this.neighbors;
    }

}
