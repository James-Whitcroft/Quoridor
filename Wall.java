package Players.TroublingTwosome;
import Interface.Coordinate;
import Interface.PlayerMove;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * @author James Whitcroft
 *
 * Wall class that constructs a wall object
 */
public class Wall {

    private Board wallBoard;
    private int id;
    private Set<PlayerMove> allWalls=new HashSet<>();

    /**
     * constructor for walls
     * @param b the playing board
     * @param id the players id
     */
    public Wall(Board b, int id){
        this.wallBoard=b;
        this.id=id;
        wallMoves();
    }

    /**
     * method creating a set of all walls[
     * @return set of all walls
     */
    private Set<PlayerMove> wallMoves(){
        //make a bunch of walls, validate before adding
        for(int x=0;x<=wallBoard.getSize();x++){
            for(int y=0;y<wallBoard.getSize();y++){
                //this creates right corners which hang over the board
                //validate then "trims" these walls off
                PlayerMove maybeWallHorizontal=new PlayerMove(id,false,new Coordinate(x,y),new Coordinate(x,y+2));
                PlayerMove maybeWallVertical= new PlayerMove(id, false, new Coordinate(x,y), new Coordinate(x+2,y));
                if(validate(maybeWallHorizontal)) {
                    allWalls.add(maybeWallHorizontal);
                }
                if(validate(maybeWallVertical)){
                    allWalls.add(maybeWallVertical);
                }
            }
        }
        return allWalls;

    }

    /**
     * method for finding the midpoint of a wall
     * @param p a player move object (better be a wall mother ******!)
     * @return coordinate representing the midpoint
     */
    private Coordinate findMid(PlayerMove p){
        if(p.getStartCol()==p.getEndCol()){
            int mid=(p.getEndRow()+p.getStartRow())/2;
            return new Coordinate(mid,p.getStartCol());
        }
        if(p.getStartRow()==p.getEndRow()){
            int mid=(p.getStartCol()+p.getEndCol())/2;
            return new Coordinate(p.getEndRow(), mid);
        }
        return new Coordinate(0,0);
    }

    /**
     * method for checking 2 walls midpoint
     * @param wall1 player move that needs to be validated
     * @param wall2 player move that needs to be validated
     * @return boolean telling whether the 2 walls overlap or not
     */
    public boolean checkMidpoints(PlayerMove wall1, PlayerMove wall2){
        //ASSUME YOU ARE PASSING A WALL MOVE
        //check to see if wall 2 has a midpoint contained in wall 1
        //please pay attention to the order in which you pass arguments to this method
        Set<Coordinate> wall1Set=new HashSet<>();
        boolean valid=true;
        wall1Set.add(wall1.getStart());
        wall1Set.add(wall1.getEnd());
        wall1Set.add(findMid(wall1));
        if(wall1Set.contains(findMid(wall2))){
            valid=false;
        }
        return valid;
    }

    /**
     * method to return a set of all walls
     * @return set of all walls
     */
    public Set<PlayerMove> getAllWalls(){
        return this.allWalls;
    }

    /**
     *  a method to remove a wall from the set of all walls
     * @param wall a player move that is invalid for this set of walls
     */
    public void adjustWalls(PlayerMove wall){
        if(allWalls.contains(wall)){
            allWalls.remove(wall);
        }
    }


    /**a method to trim the edges, cutting off walls that fall out of bounds
     *
     * @param wall a single player move, assumed a wall, to be validated
     * @return a boolean, true for valid false otherwise
     */
    private boolean validate(PlayerMove wall){
        boolean valid=true;
        if(wall.getStartRow()>wallBoard.getSize()||wall.getStartCol()>wallBoard.getSize()
                    ||wall.getEndRow()>wallBoard.getSize()||wall.getEndCol()>wallBoard.getSize()){
            valid=false;
        }
        if(wall.getStartCol()==0 && wall.getEndCol()==0){
            valid=false;
        }
        if(wall.getStartCol()==wallBoard.getSize() && wall.getEndCol()==wallBoard.getSize()){
            valid=false;
        }
        if(wall.getStartRow()==0 && wall.getEndRow()==0){
            valid=false;
        }
        if(wall.getStartRow()==wallBoard.getSize() && wall.getEndRow()==wallBoard.getSize()){
            valid=false;
        }
        return valid;
    }


    /**
     * method to see if a player can reach the end zone
     * @param start start coordinate
     * @param finish end coordinate
     * @return boolean representing whether a player can reach the end zone or not
     */
    public boolean canReachDFS(Coordinate start, Coordinate finish) {
        // assumes input check occurs previously
        Node startNode, finishNode;
        startNode = wallBoard.getMap().get(start);
        finishNode = wallBoard.getMap().get(finish);

        // prime the stack with the starting node
        Stack<Node> stack = new Stack<Node>();
        stack.push(startNode);

        // create a visited set to prevent cycles
        Set<Node> visited = new HashSet<Node>();
        // add start node to it
        visited.add(startNode);

        // loop until either the finish node is found (path exists), or the
        // dispenser is empty (no path)
        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current == finishNode) {
                return true;
            }
            // loop over all neighbors of current
            for (Node nbr : current.getNeighbors()) {
                // process unvisited neighbors
                if (!visited.contains(nbr)) {
                    visited.add(nbr);
                    stack.push(nbr);
                }
            }
        }
        return false;
    }

}
