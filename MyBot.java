// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;

import java.util.ArrayList;
import java.util.Random;

public class MyBot {
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        for (;;) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();
            ArrayList<Command> tempQueue = new ArrayList<>();

            //Find inspired locations

            //Set ships' enroute on dropoffs and the shipyard to be false
            me.clearEnroute(gameMap);

            for (final Ship ship : me.ships.values()) {
                //if the game is close to ending have the ships use an end-game move
                    //its ok to crash on a dropoff/shipyard but nowhere else
                if (gameMap.calculateDistance(ship.position, me.shipyard.position) >= Constants.MAX_TURNS-5) {
                    //move to nearest dropoff w/ intent to crash
                        // TODO: Make a move method that causes ships to crash on dropoff/shipyard
                    Log.log("Stuck in endgame.");
                    commandQueue.add(
                            ship.move(
                                    gameMap.naiveNavigate(ship, gameMap.getNearestDropoff(ship, me).position)
                            )
                    );
                }
                //if ship is full/almost full, or on dropoffRoute move to a dropoff
                    //DO NOT CALCULATE MAPCELL.HALITE / EXTRACTRATIO to determine if ship should go to dropoff. it will
                    // change after moving to a new location
                else if (ship.isFull() || ship.enroute) {
                    Log.log("Stuck in dropoff.");
                    //move to nearest dropoff safely
                    ship.enroute=true;
                    commandQueue.add(
                            ship.move(
                                    gameMap.naiveNavigate(ship, gameMap.getNearestDropoff(ship, me).position)
                            )
                    );
                }
                else {
                    //find highest value spot
                        //set mapcell at highest value position to have a desired ship w/ value
                        //also set a ship's desired location (for the direction(s) it wants to move)
                        //determine which direction the whip will move
                        //add an action to a stack of ship actions
                    commandQueue.add(
                            ship.move(
                                    gameMap.naiveNavigate(ship, gameMap.highestValueLocation(ship, me))
                            )
                    );
                }

//                final Direction randomDirection = Direction.ALL_CARDINALS.get(rng.nextInt(4));
//                commandQueue.add(ship.move(randomDirection));
            }

            // TODO
            //for each thing in the stack, see if there are conflicts. if there are, solve them otherwise add to queue
//            for(int a=0; a<tempQueue.size(); a++) {
//
//            }

            commandQueue.addAll(tempQueue);

            if (
                game.turnNumber <= 300 &&
                me.halite >= Constants.SHIP_COST &&
                !gameMap.at(me.shipyard).isOccupied())
            {
                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
        }
    }
}
