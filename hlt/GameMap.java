package hlt;

import hlt.*;
import java.util.*;

public class GameMap {
    public final int width;
    public final int height;
    public final MapCell[][] cells;

    public GameMap(final int width, final int height) {
        this.width = width;
        this.height = height;

        cells = new MapCell[height][];
        for (int y = 0; y < height; ++y) {
            cells[y] = new MapCell[width];
        }
    }

    public MapCell at(final Position position) {
        final Position normalized = normalize(position);
        return cells[normalized.y][normalized.x];
    }

    public MapCell at(final Entity entity) {
        return at(entity.position);
    }

    public int calculateDistance(final Position source, final Position target) {
        final Position normalizedSource = normalize(source);
        final Position normalizedTarget = normalize(target);

        final int dx = Math.abs(normalizedSource.x - normalizedTarget.x);
        final int dy = Math.abs(normalizedSource.y - normalizedTarget.y);

        final int toroidal_dx = Math.min(dx, width - dx);
        final int toroidal_dy = Math.min(dy, height - dy);

        return toroidal_dx + toroidal_dy;
    }

    public Position normalize(final Position position) {
        final int x = ((position.x % width) + width) % width;
        final int y = ((position.y % height) + height) % height;
        return new Position(x, y);
    }

    public ArrayList<Direction> getUnsafeMoves(final Position source, final Position destination) {
        final ArrayList<Direction> possibleMoves = new ArrayList<>();

        final Position normalizedSource = normalize(source);
        final Position normalizedDestination = normalize(destination);

        final int dx = Math.abs(normalizedSource.x - normalizedDestination.x);
        final int dy = Math.abs(normalizedSource.y - normalizedDestination.y);
        final int wrapped_dx = width - dx;
        final int wrapped_dy = height - dy;

        if (normalizedSource.x < normalizedDestination.x) {
            possibleMoves.add(dx > wrapped_dx ? Direction.WEST : Direction.EAST);
        } else if (normalizedSource.x > normalizedDestination.x) {
            possibleMoves.add(dx < wrapped_dx ? Direction.WEST : Direction.EAST);
        }

        if (normalizedSource.y < normalizedDestination.y) {
            possibleMoves.add(dy > wrapped_dy ? Direction.NORTH : Direction.SOUTH);
        } else if (normalizedSource.y > normalizedDestination.y) {
            possibleMoves.add(dy < wrapped_dy ? Direction.NORTH : Direction.SOUTH);
        }

        return possibleMoves;
    }

    public Direction naiveNavigate(final Ship ship, final Position destination) {
        // getUnsafeMoves normalizes for us
        if (!(ship.halite >= at(ship).halite/Constants.MOVE_COST_RATIO)) {
            return Direction.STILL;
        }

        for (final Direction direction : getUnsafeMoves(ship.position, destination)) {
            final Position targetPos = ship.position.directionalOffset(direction);
            if (!at(targetPos).isOccupied()) {
                at(targetPos).markUnsafe(ship);
                at(ship).markSafe();
                return direction;
            }
        }


        return Direction.STILL;
    }

    void _update() {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                cells[y][x].ship = null;
            }
        }

        final int updateCount = Input.readInput().getInt();

        for (int i = 0; i < updateCount; ++i) {
            final Input input = Input.readInput();
            final int x = input.getInt();
            final int y = input.getInt();

            cells[y][x].halite = input.getInt();
        }
    }

    static GameMap _generate() {
        final Input mapInput = Input.readInput();
        final int width = mapInput.getInt();
        final int height = mapInput.getInt();

        final GameMap map = new GameMap(width, height);

        for (int y = 0; y < height; ++y) {
            final Input rowInput = Input.readInput();

            for (int x = 0; x < width; ++x) {
                final int halite = rowInput.getInt();
                map.cells[y][x] = new MapCell(new Position(x, y), halite);
            }
        }

        return map;
    }

    /*
    While ArrayList of ships with actions to choose is not empty, call newNavigate on ship at index 0

    If there is a conflict

    i could return an array of commands and append it to command queue*/

   public ArrayList<Command> newNavigate(Ship ship, ArrayList<Ship> shipsToMove, ArrayList<Command> commands) {
       //while ship.moves.isntEmpty
       //do all the same stuff, but add a section saying "if ship.moves at collision contains opposite direction, swap"
//        for (final Direction direction : ship.moves) {
       Log.log("Movement for Ship: "+ship.id);
       while( !(ship.moves.isEmpty()) ) {
            final Position targetPos = ship.position.directionalOffset(ship.moves.get(0));

            if (at(targetPos).isOccupied()) {
                Log.log("Target position is occupied,");
                if (at(targetPos).ship.owner == ship.owner &&
                        shipsToMove.contains(at(targetPos).ship)){
                    Log.log("the ship is mine and still needs to move,");
                    if (at(targetPos).ship.visited) {
                        Log.log("and the ship has been visited.");
                        Direction oppositeDir = canSwap(ship.moves.get(0), at(targetPos).ship.moves);
                        if (oppositeDir != null) {
                            Log.log("Ship Dir: " + ship.moves.get(0) + " Other Ship Dir: " + oppositeDir);
                            commands.add(ship.move(ship.moves.get(0)));
                            commands.add(at(targetPos).ship.move(oppositeDir));
                            shipsToMove.remove(ship);
                            shipsToMove.remove(at(targetPos).ship);
                            ship.moves.clear();
                            at(targetPos).ship.moves.clear();
                            return commands;
                        }
                    } else {
                        Log.log("and the ship has NOT been visited.");
                        ship.visited = true;
                        commands = newNavigate(at(targetPos).ship, shipsToMove, commands);
                    }
                }
                if (!(ship.moves.isEmpty()))
                    ship.moves.remove(0);
            } else {
                Log.log("The target position is not occupied.");
                at(targetPos).markUnsafe(ship);
                at(ship).markSafe();
                shipsToMove.remove(ship);
                commands.add(ship.move(ship.moves.get(0)));
                ship.moves.clear();
                return commands;
            }
        }
        if(shipsToMove.contains(ship)) {
            Log.log("The ship is staying still.");
            shipsToMove.remove(ship);
            ship.moves.clear();
            commands.add(ship.stayStill());
        }
        return commands;
    }

    public Direction canSwap(Direction direction, ArrayList<Direction> moves) {
       Direction oppositeDir = null;
       if(direction == Direction.NORTH) {
           oppositeDir = Direction.SOUTH;
       } else if(direction == Direction.SOUTH) {
           oppositeDir = Direction.NORTH;
       } else if(direction == Direction.EAST) {
            oppositeDir = Direction.WEST;
        } else if(direction == Direction.WEST) {
            oppositeDir = Direction.EAST;
        }
        for(Direction otherDirection: moves){
           if(oppositeDir == otherDirection){
               return oppositeDir;
           }
       }
       return null;
    }

    public boolean canMove(Ship ship) {
       if(ship.halite >= at(ship).halite/Constants.MOVE_COST_RATIO)
           return true;
       return false;
    }


    public Direction crashNavigate(final Ship ship, final Position destination) {
        // getUnsafeMoves normalizes for us
        if (!(ship.halite >= at(ship).halite/Constants.MOVE_COST_RATIO)) {
            return Direction.STILL;
        }

        for (final Direction direction : getUnsafeMoves(ship.position, destination)) {
            final Position targetPos = ship.position.directionalOffset(direction);
            if (!at(targetPos).isOccupied() || at(targetPos).hasStructure()) {
                at(targetPos).markUnsafe(ship);
                at(ship).markSafe();
                return direction;
            }
        }

        return Direction.STILL;
    }

    public Entity getNearestDropoff(Ship ship, Player me) {
        Entity nearestDrop = me.shipyard;
        for (Dropoff dropoff: me.dropoffs.values())
            if (calculateDistance(ship.position, dropoff.position) < calculateDistance(ship.position, nearestDrop.position))
                nearestDrop = dropoff;
        return nearestDrop;
    }

    public Position bestDropoffLocation() {
        //TODO
        //int value;
        //Position ??
        //tempVal =
        return null;
    }

    public void markEnemyShips(Player me, ArrayList<Player> enemies) {
        for (Player enemy : enemies) {
            if (me.id == enemy.id)
                continue;
            else
                for (Ship ship : enemy.ships.values())
                    for (Position position : ship.position.getSurroundingCardinals())
                        at(position).markUnsafe(ship);
        }
    }

    public Position highestValueLocation(Ship ship, Player me) {
        int bestVal = ((at(ship).halite/Constants.EXTRACT_RATIO)+(at(ship).halite/Constants.MOVE_COST_RATIO))*6;
        Position bestPosition = ship.position;
        int bestDistance = (this.height+this.width)/2;
        //for every position in the map
        for (int x = 0; x < this.width; ++x)
            for (int y = 0; y < this.height; ++y) {
                Position tempPosition = new Position(x,y);
                if( tempPosition.equals(ship.position) )
                    continue;

                int tempVal;
                int tempDistance = bestDistance = this.calculateDistance(ship.position, tempPosition);
                int totalDistance = this.calculateDistance(ship.position, tempPosition) /*+
                        this.calculateDistance(tempPosition, getNearestDropoff(ship, me).position)*/;

                //if position is inspired use inspired extraction ratio
                if (this.at(tempPosition).inspired)
                    tempVal = this.at(tempPosition).halite/totalDistance*2;
                else
                    tempVal = this.at(tempPosition).halite/totalDistance;

                //assign temp vars to best vars if better than best
                if (tempVal >= bestVal) {
                    bestVal = tempVal;
                    bestPosition = tempPosition;
                    bestDistance = this.calculateDistance(ship.position, bestPosition);
                }
            }
        return bestPosition;
    }
}
