package Players.TroublingTwosome;
import Engine.Logger;
import Interface.Coordinate;
import Interface.PlayerModule;
import Interface.PlayerMove;
import java.util.*;

/**
 * @author James Whitcroft
 * Player Class
 *
 */
public class TroublingTwosome implements PlayerModule {

    private Board screen;
    private int id;
    private Logger logFile;
    private Wall wallMagic;

    @Override
    public void init(Logger logger, int playerId, int numWalls, Map<Integer, Coordinate> map) {
        //initialize a new board object to store games state
        Board screen=new Board(9);
        this.wallMagic=new Wall(screen,playerId);

        //adding players to board object
        for(int i=1; i<=map.size(); i++){
            screen.addPlayers(i, new Hero(i,numWalls,map.get(i),screen.getSize()));
        }
        this.screen=screen;
        this.id=playerId;
        this.logFile=logger;
    }

    @Override
    public void lastMove(PlayerMove playerMove) {
        Node holderStart=screen.getMap().get(playerMove.getStart());

        if(playerMove.isMove()){
            //set hero to new pos
            screen.getPlayers().get(playerMove.getPlayerId()).setPos(playerMove.getEnd());
            screen.getPlayers().get(playerMove.getPlayerId()).adjustSurroundings();
        } else {
            //checks to make sure each player can always reach their finish
            if (playerMove.getStartRow() == playerMove.getEndRow()) {
                //for horizontal walls
                holderStart.removeNeighbor(screen.getMap().get(new Coordinate(playerMove.getStartRow() - 1, playerMove.getStartCol())));
                screen.getMap().get(new Coordinate(playerMove.getEndRow(), playerMove.getEndCol() - 1)).removeNeighbor(screen.getMap().get(new Coordinate(playerMove.getEndRow() - 1, playerMove.getEndCol() - 1)));
            } else if (playerMove.getStartCol() == playerMove.getEndCol()) {
                //for vertical walls
                holderStart.removeNeighbor(screen.getMap().get(new Coordinate(playerMove.getStartRow(), playerMove.getStartCol() - 1)));
                screen.getMap().get(new Coordinate(playerMove.getEndRow() - 1, playerMove.getEndCol())).removeNeighbor(screen.getMap().get(new Coordinate(playerMove.getEndRow() - 1, playerMove.getEndCol() - 1)));
            }


            //MODDING WALLS do not change
            Set<PlayerMove> tobeRemoved = new HashSet<>();
            for (PlayerMove wall : wallMagic.getAllWalls()) {
                if (!wallMagic.checkMidpoints(playerMove, wall) && !wallMagic.checkMidpoints(wall, playerMove)) {
                    tobeRemoved.add(wall);
                }
            }
            wallMagic.adjustWalls(playerMove);
            for (PlayerMove p : tobeRemoved) {
                wallMagic.adjustWalls(p);
            }
            tobeRemoved.clear();
            //reduces the number of walls for given player by 1
            screen.getPlayers().get(playerMove.getPlayerId()).setWalls();
            //do not change above!!

            Set<PlayerMove> takeMeAway = new HashSet<>();
            for (PlayerMove wall : wallMagic.getAllWalls()) {
                if (!canIreach(wall)) {
                    takeMeAway.add(wall);
                }
            }
            for(PlayerMove p:takeMeAway){
                wallMagic.adjustWalls(p);
            }
        }
    }

    @Override
    public void playerInvalidated(int i) {
    }

    @Override
    public PlayerMove move() {

        List<PlayerMove> moves = new LinkedList<>(allPossibleMoves());
        //all my piece moves
        List<PlayerMove> pieceMoves = new LinkedList<>();
        //all my wall moves
        List<PlayerMove> wallMoves = new LinkedList<>();
        //a map of the "best" wall moves
        HashMap<Integer, PlayerMove> bestWall = new HashMap<>();
        //a map of wall moves that lengthen my goal :(
        HashMap<Integer, PlayerMove> tooLongWall = new HashMap<>();
        //who da f knows what this jizz is
        //no really its a map of player ids to their shortest path(currently)
        HashMap<Integer,List<Coordinate>> playersShortest=new HashMap<>();
        for(PlayerMove p: moves){
            if(p.isMove()){
                pieceMoves.add(p);
            }else{
                wallMoves.add(p);
            }
        }
        for(int x=1;x<=screen.getPlayers().size();x++){
            playersShortest.put(x, adjustShortest(x));
        }
        for(int y=1;y<=screen.getPlayers().size();y++){
            tooLongWall.clear();
            if(y!=getID()) {
                //the mad dash, if they have no walls and im closer, run for it
                if(screen.getPlayers().get(y).getWalls()<=0 &&
                        playersShortest.get(id).size()<playersShortest.get(y).size()){
                    HashMap<Integer, PlayerMove> theBest=new HashMap<>();
                    for(PlayerMove move:pieceMoves){
                        theBest.putAll(bestPieceMove(move));
                    }
                    int findMe = 100;
                    for (int lowest : theBest.keySet()) {
                        if (lowest < findMe) {
                            findMe = lowest;
                        }
                    }
                    return theBest.get(findMe);
                }
                //whoa bro, back up... you are almost finished, let me try and stop that shiz
                if(playersShortest.get(y).size()<=3 && screen.getPlayers().get(id).getWalls()>0 &&
                        playersShortest.get(id).size()>playersShortest.get(y).size()){
                    for(PlayerMove wall:wallMoves){
                        bestWall.putAll(isThisLonger(wall,y,playersShortest.get(y)));
                        //does this wall make MY path longer?
                        tooLongWall.putAll(isThisLonger(wall,id,playersShortest.get(y)));
                    }
                    HashMap<PlayerMove, Integer> toBeRemoved=new HashMap<>();
                    for(int i:bestWall.keySet()){
                        for(PlayerMove p:tooLongWall.values()) {
                            if (bestWall.get(i).equals(p)){
                                //if my path is longer, that wall isnt an option
                                toBeRemoved.put(p, i);
                            }
                        }
                    }
                    for(PlayerMove move:toBeRemoved.keySet()){
                        if(bestWall.containsValue(move)){
                            //OFF WITH ITS HEAD!
                            bestWall.remove(toBeRemoved.get(move));
                        }
                    }
                    int beatMe = 0;
                    for (int x : bestWall.keySet()) {
                        if (x > beatMe) {
                            //beat it ;)
                            beatMe = x;
                        }
                    }
                    return bestWall.get(beatMe);

                }
                //if im closer or equal+1(i dont want to place a bunch of dumb
                // walls if im second player)distance to finish, or im out of walls
                //find my best move and make it
                if (playersShortest.get(id).size() <= playersShortest.get(y).size()+1 || screen.getPlayers().get(id).getWalls()==0) {
                    HashMap<Integer, PlayerMove> theBest=new HashMap<>();
                    for(PlayerMove move:pieceMoves){
                        theBest.putAll(bestPieceMove(move));
                    }
                    int findMe = 100;
                    for (int lowest : theBest.keySet()) {
                        if (lowest < findMe) {
                            //get low ;)
                            findMe = lowest;
                        }
                    }
                    return theBest.get(findMe);

                }else{
                    //if none of that applies, see which direction opponent is going and
                    //place a wall in their way
                    //horizontal
                    if(playersShortest.get(y).get(1).getRow()==screen.getPlayers().get(y).getPos().getRow()){
                        if(playersShortest.get(y).get(1).getCol()<screen.getPlayers().get(y).getPos().getCol()){
                            for(PlayerMove wall:wallMoves){
                                bestWall.putAll(isThisLonger(wall,y,playersShortest.get(y)));
                                tooLongWall.putAll(isThisLonger(wall,id,playersShortest.get(y)));
                            }
                            HashMap<PlayerMove, Integer> toBeRemoved=new HashMap<>();
                            for(int i:bestWall.keySet()){
                                for(PlayerMove p:tooLongWall.values()) {
                                    if (bestWall.get(i).equals(p)){
                                        toBeRemoved.put(p,i);
                                    }
                                }
                            }
                            for(PlayerMove move:toBeRemoved.keySet()){
                                if(bestWall.containsValue(move)){
                                    bestWall.remove(toBeRemoved.get(move));
                                }
                            }
                            int beatMe = 0;
                            for (int x : bestWall.keySet()) {
                                if (x > beatMe) {
                                    beatMe = x;
                                }
                            }
                            return bestWall.get(beatMe);
                        }else{
                            for(PlayerMove wall:wallMoves){
                                bestWall.putAll(isThisLonger(wall,y,playersShortest.get(y)));
                                tooLongWall.putAll(isThisLonger(wall,id,playersShortest.get(y)));
                            }
                            HashMap<PlayerMove, Integer> toBeRemoved=new HashMap<>();
                            for(int i:bestWall.keySet()){
                                for(PlayerMove p:tooLongWall.values()) {
                                    if (bestWall.get(i).equals(p)){
                                        toBeRemoved.put(p,i);
                                    }
                                }
                            }
                            for(PlayerMove move:toBeRemoved.keySet()){
                                if(bestWall.containsValue(move)){
                                    bestWall.remove(toBeRemoved.get(move));
                                }
                            }
                            int beatMe = 0;
                            for (int x : bestWall.keySet()) {
                                if (x > beatMe) {
                                    beatMe = x;
                                }
                            }
                            return bestWall.get(beatMe);
                        }
                    }else{
                        //if none of that applies, see which direction opponent is going and
                        //place a wall in their way
                        //vertical
                        if(playersShortest.get(y).get(1).getCol()==screen.getPlayers().get(y).getPos().getCol()) {
                            if (playersShortest.get(y).get(1).getRow() < screen.getPlayers().get(y).getPos().getRow()) {
                                for(PlayerMove wall:wallMoves){
                                    bestWall.putAll(isThisLonger(wall,y,playersShortest.get(y)));
                                    tooLongWall.putAll(isThisLonger(wall,id,playersShortest.get(y)));
                                }
                                HashMap<PlayerMove, Integer> toBeRemoved=new HashMap<>();
                                for(int i:bestWall.keySet()){
                                    for(PlayerMove p:tooLongWall.values()) {
                                        if (bestWall.get(i).equals(p)){
                                            toBeRemoved.put(p,i);
                                        }
                                    }
                                }
                                for(PlayerMove move:toBeRemoved.keySet()){
                                    if(bestWall.containsValue(move)){
                                        bestWall.remove(toBeRemoved.get(move));
                                    }
                                }
                                int beatMe = 0;
                                for (int x : bestWall.keySet()) {
                                    if (x > beatMe) {
                                        beatMe = x;
                                    }
                                }
                                return bestWall.get(beatMe);
                            } else {
                                for(PlayerMove wall:wallMoves){
                                    bestWall.putAll(isThisLonger(wall,y,playersShortest.get(y)));
                                    tooLongWall.putAll(isThisLonger(wall,id,playersShortest.get(y)));
                                }
                                HashMap<PlayerMove, Integer> toBeRemoved=new HashMap<>();
                                for(int i:bestWall.keySet()){
                                    for(PlayerMove p:tooLongWall.values()) {
                                        if (bestWall.get(i).equals(p)){
                                            toBeRemoved.put(p,i);
                                        }
                                    }
                                }
                                for(PlayerMove move:toBeRemoved.keySet()){
                                    if(bestWall.containsValue(move)){
                                        bestWall.remove(toBeRemoved.get(move));
                                    }
                                }
                                int beatMe = 0;
                                for (int x : bestWall.keySet()) {
                                    if (x > beatMe) {
                                        beatMe = x;
                                    }
                                }
                                return bestWall.get(beatMe);
                            }
                        }
                    }
                }
                for(Coordinate c: playersShortest.get(id)) {
                    if (pieceMoves.contains(new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), c))) {
                        playersShortest.get(id).remove(c);
                        return new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), c);
                    }
                }
            }
        }
        if(pieceMoves.size()>0) {
            //ideally this will never happen, but even more so, it will stop me from randomly generating a move
            //as shown below *
            HashMap<Integer, PlayerMove> lastChance = new HashMap<>();
            for (PlayerMove move : pieceMoves) {
                lastChance.putAll(bestPieceMove(move));
            }
            int findMe = 100;
            for (int lowest : lastChance.keySet()) {
                if (lowest < findMe) {
                    findMe = lowest;
                }
            }
            return lastChance.get(findMe);
        }
        //*HI, IM DUMB AT THINGS AND STUFF!
        return moves.get(0);
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public Set<Coordinate> getNeighbors(Coordinate coordinate) {
        Set<Coordinate> neigh=new HashSet<>();
       for(Node n: screen.getMap().get(coordinate).getNeighbors()){
           neigh.add(n.getPosition());
       }
        return neigh;
    }

    @Override
    public List<Coordinate> getShortestPath(Coordinate coordinate, Coordinate coordinate1) {
        // assumes input check occurs previously
        Node startNode, finishNode;
        startNode = this.screen.getMap().get(coordinate);
        finishNode = this.screen.getMap().get(coordinate1);

        // prime the dispenser (stack) with the starting node
        List<Node> dispenser = new ArrayList<>();
        dispenser.add(0, startNode);

        // construct the predecessors data structure
        Map<Node, Node> predecessors = new HashMap<>();
        // put the starting node in, and just assign itself as predecessor
        predecessors.put(startNode, startNode);

        // loop until either the finish node is found, or the
        // dispenser is empty (no path)
        while (!dispenser.isEmpty()) {
            //this is very very slow... but returns 100%
            //I will fix this and speed it up
            Node current = dispenser.remove(dispenser.size()-1);
            if (current == finishNode) {
                break;
            }
            //loop over all neighbors of current
            for (Node nbr : current.getNeighbors()) {
                //process unvisited neighbors
                if(!predecessors.containsKey(nbr)) {
                    predecessors.put(nbr, current);
                    dispenser.add(0, nbr);
                }
            }
        }

        List<Node> nodes= constructPath(predecessors, startNode, finishNode);
        List<Coordinate> finish=new ArrayList<>();
        for(Node x:nodes){
            finish.add(x.getPosition());
        }
        return finish;
    }

    @Override
    public int getWallsRemaining(int i) {
        return screen.getPlayers().get(i).getWalls();
    }

    @Override
    public Coordinate getPlayerLocation(int i) {
        return screen.getPlayers().get(i).getPos();
    }
    @Override
    public Map<Integer, Coordinate> getPlayerLocations() {
        Map<Integer,Coordinate> locs=new HashMap<>();
        for(int x=1;x<=screen.getPlayers().size();x++){
            locs.put(x,screen.getPlayers().get(x).getPos());
        }
        return locs;
    }

    @Override
    public Set<PlayerMove> allPossibleMoves() {
        HashSet<PlayerMove> validMoves = new HashSet<>();
        validMoves.addAll(getAllPieceMoves());
        if(screen.getPlayers().get(getID()).getWalls()>0) {
            validMoves.addAll(wallMagic.getAllWalls());
        }
        return validMoves;
    }

    /**
     *
     * @param predecessors a map of nodes linked to predecessor nodes
     * @param startNode a node representing the starting position
     * @param finishNode a node representing the ending position
     * @return a list representing a path from start to finish if exists
     */
    private List<Node> constructPath(Map<Node,Node> predecessors,Node startNode, Node finishNode) {

        //use predecessors to work backwards from finish to start,
        //all the while dumping everything into a linked list
        List<Node> path = new LinkedList<>();

        if(predecessors.containsKey(finishNode)) {
            Node currNode = finishNode;
            while (currNode != startNode) {
                path.add(0, currNode);
                currNode = predecessors.get(currNode);
            }
            path.add(0, startNode);
        }
        return path;
    }


    /**a method to return all VALID player moves
     *
     * @return a set of valid Playermove objects
     */
    private Set<PlayerMove> getAllPieceMoves() {
        Set<PlayerMove> validmoves = new HashSet<>();
        for(Node n: screen.getMap().get(screen.getPlayers().get(this.getID()).getPos()).getNeighbors()) {
            validmoves.addAll(this.validatePiece(this.getID(), n.getPosition()));
        }
        return validmoves;
    }

    /**a method to validate a player move based
     * on a given coordinate
     *
     * @param id the given players id
     * @param destination the coordinate object to be determined valid
     * @return a set of validated Playermove objects
     */
    private Set<PlayerMove> validatePiece(int id, Coordinate destination){
        String direction="";
        Set<PlayerMove> possibleMoves = new HashSet<>();
        //if the destination is in my surroundings...
        if (screen.getPlayers().get(id).getSurroundings().contains(destination)) {

            //what direction am i going?
            if(screen.getPlayers().get(id).getPos().getRow() < destination.getRow()){direction="south";}
            else if(screen.getPlayers().get(id).getPos().getRow() > destination.getRow()){direction="north";}
            else if(screen.getPlayers().get(id).getPos().getCol() > destination.getCol()){direction="west";}
            else if(screen.getPlayers().get(id).getPos().getCol() < destination.getCol()){direction="east";}
            //loop over all player's coordinates
            for (int x = 1; x <= screen.getPlayers().values().size(); x++) {
                //if a player is in my destination, can i jump directly over them?
                //if so, return that spot as a possible move without checking
                //players other surroundings, this is important!
                if (screen.getPlayers().get(x).getPos().equals(destination)) {
                    if(screen.getMap().get(screen.getPlayers().get(id).getPos()).getNeighbors().contains(screen.getMap().get(destination))) {
                        if (direction.equals("south")) {
                            if (screen.getMap().get(destination).getNeighbors().contains(
                                    screen.getMap().get(new Coordinate(screen.getMap().get(destination).getPosition().getRow() + 1,
                                            screen.getMap().get(destination).getPosition().getCol())))) {
                                if(!isSomeonehere(new Coordinate(screen.getMap().get(destination).getPosition().getRow() + 1,
                                        screen.getMap().get(destination).getPosition().getCol()))){
                                    Coordinate coord = new Coordinate(screen.getMap().get(destination).getPosition().getRow() + 1,
                                            screen.getMap().get(destination).getPosition().getCol());
                                    possibleMoves.add(new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), coord));
                                    return possibleMoves;
                                }
                            }
                        }
                        if (direction.equals("north")) {
                            if (screen.getMap().get(destination).getNeighbors().contains(
                                    screen.getMap().get(new Coordinate(screen.getMap().get(destination).getPosition().getRow() - 1,
                                            screen.getMap().get(destination).getPosition().getCol())))){
                                if(!isSomeonehere(new Coordinate(screen.getMap().get(destination).getPosition().getRow() - 1,
                                            screen.getMap().get(destination).getPosition().getCol()))){
                                    Coordinate coord = new Coordinate(screen.getMap().get(destination).getPosition().getRow() - 1,
                                            screen.getMap().get(destination).getPosition().getCol());
                                    possibleMoves.add(new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), coord));
                                    return possibleMoves;
                                }
                            }
                        }
                        if (direction.equals("west")) {
                            if (screen.getMap().get(destination).getNeighbors().contains(
                                    screen.getMap().get(new Coordinate(screen.getMap().get(destination).getPosition().getRow(),
                                            screen.getMap().get(destination).getPosition().getCol() - 1)))) {
                                if(!isSomeonehere(new Coordinate(screen.getMap().get(destination).getPosition().getRow(),
                                        screen.getMap().get(destination).getPosition().getCol() - 1))) {
                                    Coordinate coord = new Coordinate(screen.getMap().get(destination).getPosition().getRow(),
                                            screen.getMap().get(destination).getPosition().getCol() - 1);
                                    possibleMoves.add(new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), coord));
                                    return possibleMoves;
                                }
                            }
                        }
                        if (direction.equals("east")) {
                            if (screen.getMap().get(destination).getNeighbors().contains(
                                    screen.getMap().get(new Coordinate(screen.getMap().get(destination).getPosition().getRow(),
                                            screen.getMap().get(destination).getPosition().getCol() + 1)))) {
                                if(!isSomeonehere(new Coordinate(screen.getMap().get(destination).getPosition().getRow(),
                                        screen.getMap().get(destination).getPosition().getCol() + 1))) {
                                    Coordinate coord = new Coordinate(screen.getMap().get(destination).getPosition().getRow(),
                                            screen.getMap().get(destination).getPosition().getCol() + 1);
                                    possibleMoves.add(new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), coord));
                                    return possibleMoves;
                                }
                            }
                        }
                        //am i stuck? 1 one neighbor indicates an alley
                        if(screen.getMap().get(screen.getPlayers().get(id).getPos()).getNeighbors().size()==1) {
                            for (int w = 1; w <= screen.getPlayers().values().size(); w++) {
                                //is there a player in front of me?
                                if (screen.getMap().get(screen.getPlayers().get(id).getPos()).getNeighbors()
                                        .contains(screen.getMap().get(screen.getPlayers().get(w).getPos()))) {
                                    if(screen.getPlayers().get(id).getWalls()<=0) {
                                        PlayerMove pass = new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), screen.getPlayers().get(id).getPos());
                                        possibleMoves.add(pass);
                                        return possibleMoves;
                                    }
                                }
                            }
                        }


                        //if the players coordinate is the same as the destination
                        //you need to check players location for valid surroundings
                        //using nodes ensures walls are accounted for
                        Set<PlayerMove> toBeRemoved = new HashSet<>();
                        for (Node n : screen.getMap().get(destination).getNeighbors()) {
                            for (int t = 1; t <= screen.getPlayers().values().size(); t++) {
                                //if the spot is not occupied it is valid
                                //if (screen.getPlayers().get(t).getPos().getRow() != n.getPosition().getRow() ||
                                //        screen.getPlayers().get(t).getPos().getCol() != n.getPosition().getCol()) {
                                if(!screen.getPlayers().get(t).getPos().equals(n.getPosition())){
                                    PlayerMove possibleMove = new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), n.getPosition());
                                    possibleMoves.add(possibleMove);
                                } else {
                                    //prevents the case of first player invalidating move, but then second player validating the move
                                    PlayerMove possibleMove = new PlayerMove(id, true, screen.getPlayers().get(id).getPos(), n.getPosition());
                                    toBeRemoved.add(possibleMove);
                                }
                            }
                        }
                        possibleMoves.removeAll(toBeRemoved);
                        return  possibleMoves;
                    }
                }
            }
            //if there is no player in the spot
            //check the players current location's neighbors for destination
            //this ensures there is no wall in the way
            if(screen.getMap().get(screen.getPlayers().get(id).getPos()).getNeighbors().contains(screen.getMap().get(destination))){
                PlayerMove possibleMove=new PlayerMove(id,true,screen.getPlayers().get(id).getPos(),destination);
                possibleMoves.add(possibleMove);
                return possibleMoves;
            }
        }
        return possibleMoves;
    }

    /**
     * a method to check if a node is currently occupied
     * @param c the coordinate to check
     * @return a boolean, true if node is occupied
     */
    public boolean isSomeonehere(Coordinate c){
        boolean yes=false;
        for (int x = 1; x <= screen.getPlayers().values().size(); x++) {
            if(c.equals(screen.getPlayers().get(x).getPos())){
                yes=true;
            }
        }
        return yes;
    }

    /**
     * a method to "simulate" placing a wall and checking can reach for all players
     * @param wall the playermove (wall) to place
     * @return a boolean, true if all players can reach their destinations
     */
    private boolean canIreach(PlayerMove wall){
        String wallFace="";
        boolean canReach=false;
        //place the wall
        if (wall.getStartRow() == wall.getEndRow()) {
            //for horizontal walls
            screen.getMap().get(wall.getStart()).removeNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow() - 1, wall.getStartCol())));
            screen.getMap().get(new Coordinate(wall.getEndRow(), wall.getEndCol() - 1)).removeNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)));
            wallFace="horizontal";
        } else if (wall.getStartCol() == wall.getEndCol()) {
            //for vertical walls
            screen.getMap().get(wall.getStart()).removeNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow(), wall.getStartCol() - 1)));
            screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol())).removeNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)));
            wallFace="vertical";
        }

        for (int i=1;i<=screen.getPlayers().size();i++) {
            //the issue is in here.. if player one returns false, and player two returns true...

            canReach=false;
            if (screen.getPlayers().get(i).getDirection().equals("north")) {
                for (int r = 0; r < screen.getSize(); r++) {
                    if (wallMagic.canReachDFS(screen.getPlayers().get(i).getPos(), new Coordinate(screen.getPlayers().get(i).getEnd(), r))) {
                        canReach=true;
                        break;
                    }
                }
            }
            if (screen.getPlayers().get(i).getDirection().equals("south")) {
                for (int r = 0; r < screen.getMap().size(); r++) {
                    if (wallMagic.canReachDFS(screen.getPlayers().get(i).getPos(), new Coordinate(screen.getPlayers().get(i).getEnd(), r))) {
                        canReach=true;
                        break;
                    }
                }
            }

            if (screen.getPlayers().get(i).getDirection().equals("east")) {
                for (int c = 0; c < screen.getMap().size(); c++) {
                    if (wallMagic.canReachDFS(screen.getPlayers().get(i).getPos(), new Coordinate(c, screen.getPlayers().get(i).getEnd()))) {
                        canReach=true;
                        break;
                    }
                }
            }
            if (screen.getPlayers().get(i).getDirection().equals("west")) {
                for (int c = 0; c < screen.getMap().size(); c++) {
                    if (wallMagic.canReachDFS(screen.getPlayers().get(i).getPos(), new Coordinate(c, screen.getPlayers().get(i).getEnd()))) {
                        canReach=true;
                        break;
                    }
                }
            }
            //i think this will fix the issue by breaking out if any one player can not reach any goal
            if(!canReach){
                break;
            }
        }
        //take the wall away
        if(wallFace.equals("horizontal")){
            screen.getMap().get(wall.getStart()).addNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow() - 1, wall
                    .getStartCol())), screen);
            screen.getMap().get(new Coordinate(wall.getEndRow(), wall.getEndCol() - 1))
                    .addNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)), screen);
        }else if(wallFace.equals("vertical")){
            screen.getMap().get(wall.getStart()).addNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow(), wall
                    .getStartCol() - 1)), screen);
            screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol()))
                    .addNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)), screen);
        }

        return canReach;
    }

    /**
     * calculates the single shortest path a player has, if multiple paths of same
     * size exist, the first found is used
     * @param id the players id
     * @return a list of coordinates making up the players shortest path to goal
     */
    private List<Coordinate> adjustShortest(int id){
        HashMap<Integer,List<Coordinate>>shorts=new HashMap<>();

        if (screen.getPlayers().get(id).getDirection().equals("north")) {
            for (int r = 0; r < screen.getSize(); r++) {
                List<Coordinate> path=getShortestPath(screen.getPlayers().get(id).getPos(), new Coordinate(screen.getPlayers().get(id).getEnd(), r));
                if(path.size()>0) {
                    shorts.put(path.size(), path);
                }
            }
        }
        if (screen.getPlayers().get(id).getDirection().equals("south")) {
            for (int r = 0; r < screen.getSize(); r++) {
                List<Coordinate> path=getShortestPath(screen.getPlayers().get(id).getPos(), new Coordinate(screen.getPlayers().get(id).getEnd(), r));
                if(path.size()>0) {
                    shorts.put(path.size(), path);
                }
            }
        }

        if (screen.getPlayers().get(id).getDirection().equals("east")) {
            for (int c = 0; c < screen.getSize(); c++) {
                List<Coordinate> path=getShortestPath(screen.getPlayers().get(id).getPos(),new Coordinate(c,screen.getPlayers().get(id).getEnd()));
                if(path.size()>0) {
                    shorts.put(path.size(), path);
                }
            }
        }
        if (screen.getPlayers().get(id).getDirection().equals("west")) {
            for (int c = 0; c < screen.getSize(); c++) {
                List<Coordinate> path=getShortestPath(screen.getPlayers().get(id).getPos(),new Coordinate(c,screen.getPlayers().get(id).getEnd()));
                if(path.size()>0) {
                    shorts.put(path.size(), path);
                }
            }
        }
        //trim the fat
        int findMe=100;
        for(int lowest:shorts.keySet()){
            if(lowest<findMe){
                findMe=lowest;
            }
        }
        return shorts.get(findMe);

    }

    /**
     * a method to simulate a wall placement, then, check if that wall placement made the opponents shortest path longer
     * @param wall a PlayerMove object(wall)
     * @param id the players id
     * @param path the players current shortest path, used to compare
     * @return a hashmap<after wall path size, wall>
     */
    private HashMap<Integer,PlayerMove> isThisLonger(PlayerMove wall, int id, List<Coordinate> path){
        HashMap<Integer, PlayerMove> longest=new HashMap<>();
        //initially this method returned a boolean, i left this as an option in case i change my mind
        boolean isIt=false;
        String wallFace="";
        if (wall.getStartRow() == wall.getEndRow()) {
            //for horizontal walls
            screen.getMap().get(wall.getStart()).removeNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow() - 1, wall.getStartCol())));
            screen.getMap().get(new Coordinate(wall.getEndRow(), wall.getEndCol() - 1)).removeNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)));
            wallFace="horizontal";
        } else if (wall.getStartCol() == wall.getEndCol()) {
            //for vertical walls
            screen.getMap().get(wall.getStart()).removeNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow(), wall.getStartCol() - 1)));
            screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol())).removeNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)));
            wallFace="vertical";
        }

        if (screen.getPlayers().get(id).getDirection().equals("north")) {
            for (int r = 0; r < screen.getSize(); r++) {
                List<Coordinate> p=getShortestPath(screen.getPlayers().get(id).getPos(), new Coordinate(screen.getPlayers().get(id).getEnd(), r));
                if(p.size()>0) {
                    if (p.get(p.size() - 1).equals(path.get(path.size() - 1)) && p.size() > path.size()) {
                        longest.put(p.size(),wall);
                        isIt = true;
                    }
                }
            }
        }
        if (screen.getPlayers().get(id).getDirection().equals("south")) {
            for (int r = 0; r < screen.getSize(); r++) {
                List<Coordinate> p=getShortestPath(screen.getPlayers().get(id).getPos(), new Coordinate(screen.getPlayers().get(id).getEnd(), r));
                if(p.size()>0) {
                    if (p.get(p.size() - 1).equals(path.get(path.size() - 1)) && p.size() > path.size()) {
                        longest.put(p.size(),wall);
                        isIt = true;
                    }
                }
            }
        }

        if (screen.getPlayers().get(id).getDirection().equals("east")) {
            for (int c = 0; c < screen.getSize(); c++) {
                List<Coordinate> p=getShortestPath(screen.getPlayers().get(id).getPos(), new Coordinate(c,screen.getPlayers().get(id).getEnd()));
                if(p.size()>0) {
                    if (p.get(p.size() - 1).equals(path.get(path.size() - 1)) && p.size() > path.size()) {
                        longest.put(p.size(),wall);
                        isIt = true;
                    }
                }
            }
        }
        if (screen.getPlayers().get(id).getDirection().equals("west")) {
            for (int c = 0; c < screen.getSize(); c++) {
                List<Coordinate> p=getShortestPath(screen.getPlayers().get(id).getPos(), new Coordinate(c,screen.getPlayers().get(id).getEnd()));
                if(p.size()>0) {
                    if (p.get(p.size() - 1).equals(path.get(path.size() - 1)) && p.size() > path.size()) {
                        longest.put(p.size(),wall);
                        isIt = true;
                    }
                }
            }
        }

        if(wallFace.equals("horizontal")){
            screen.getMap().get(wall.getStart()).addNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow() - 1, wall
                    .getStartCol())), screen);
            screen.getMap().get(new Coordinate(wall.getEndRow(), wall.getEndCol() - 1))
                    .addNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)), screen);
        }else if(wallFace.equals("vertical")){
            screen.getMap().get(wall.getStart()).addNeighbor(screen.getMap().get(new Coordinate(wall.getStartRow(), wall
                    .getStartCol() - 1)), screen);
            screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol()))
                    .addNeighbor(screen.getMap().get(new Coordinate(wall.getEndRow() - 1, wall.getEndCol() - 1)), screen);
        }
        return longest;
        //return isIt;
    }

    /**
     * a method to determine which piece move will result in the shortest path
     * @param move the piece move test against
     * @return a hashmap mapping the path size to the player move that caused it
     */
    private HashMap<Integer, PlayerMove> bestPieceMove(PlayerMove move) {
        HashMap<Integer, List<Coordinate>> shorts = new HashMap<>();
        HashMap<Integer, PlayerMove> all=new HashMap<>();
        if (screen.getPlayers().get(id).getDirection().equals("north")) {
            for (int r = 0; r < screen.getSize(); r++) {
                List<Coordinate> path = getShortestPath(move.getEnd(), new Coordinate(screen.getPlayers().get(id).getEnd(), r));
                if (path.size() > 0) {
                    shorts.put(path.size(), path);
                }
            }
        }
        if (screen.getPlayers().get(id).getDirection().equals("south")) {
            for (int r = 0; r < screen.getSize(); r++) {
                List<Coordinate> path = getShortestPath(move.getEnd(), new Coordinate(screen.getPlayers().get(id).getEnd(), r));
                if (path.size() > 0) {
                    shorts.put(path.size(), path);
                }
            }
        }

        if (screen.getPlayers().get(id).getDirection().equals("east")) {
            for (int c = 0; c < screen.getSize(); c++) {
                List<Coordinate> path = getShortestPath(move.getEnd(), new Coordinate(c, screen.getPlayers().get(id).getEnd()));
                if (path.size() > 0) {
                    shorts.put(path.size(), path);
                }
            }
        }
        if (screen.getPlayers().get(id).getDirection().equals("west")) {
            for (int c = 0; c < screen.getSize(); c++) {
                List<Coordinate> path = getShortestPath(move.getEnd(), new Coordinate(c, screen.getPlayers().get(id).getEnd()));
                if (path.size() > 0) {
                    shorts.put(path.size(), path);
                }
            }
        }

        int findMe = 100;
        for (int lowest : shorts.keySet()) {
            if (lowest < findMe) {
                findMe = lowest;
            }
        }
        all.put(findMe,move);
        return all;

    }

}

