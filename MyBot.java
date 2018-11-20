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
            gameMap.markEnemyShips(me, game.players);

            final ArrayList<Command> commandQueue = new ArrayList<>();
            ArrayList<Ship> myShips = new ArrayList<Ship>();

            //TODO: Find inspired locations

            //TODO: Make a hash/set of shipContainers for their initialization variables each turn

            //TODO: get ships to make a dropoff when resource near the shipyard is low

            //Set ships' enroute to be false if on a dropoff/shipyard
            me.clearEnroute(gameMap);

            for (final Ship ship : me.ships.values()) {
                Log.log("Ship id: "+ship.id+" Enroute: "+ship.enroute);
                //if the game is close to ending have the ships use an end-game move
                if (gameMap.calculateDistance(ship.position, me.shipyard.position) >= Constants.MAX_TURNS-game.turnNumber-15) {
                    //move to nearest dropoff w/ intent to crash
                    // TODO: Make a move method that causes ships to crash on dropoff/shipyard to be more efficient
                    // (Try having the value system that makes ships want to branch out more first)
                    Log.log("Ship '" + ship.id + "' is in endgame.");
                    commandQueue.add(
                            ship.move(
                                    gameMap.crashNavigate(ship, gameMap.getNearestDropoff(ship, me).position)
                            )
                    );
                }
                //if ship is full/almost full, or enroute, move to nearest dropoff location
                else if (ship.enroute || ship.isFull()) {
                    Log.log("Ship '" + ship.id + "' is enroute to " + gameMap.getNearestDropoff(ship, me).id + ".");

                    ship.enroute = true;
                    ship.moves = gameMap.getUnsafeMoves(ship.position, gameMap.getNearestDropoff(ship, me).position);
                    myShips.add(ship);
                }
                // if the ship has enough resource to move from the current spot, find most valuable spot
                else if(gameMap.canMove(ship)) {
                    Log.log("Ship '" + ship.id + "' is making a movement.");
                    ship.moves = gameMap.getUnsafeMoves(ship.position, gameMap.highestValueLocation(ship, me));
                    myShips.add(ship);
                }
                // otherwise stay still
                else {
                    commandQueue.add(ship.stayStill());
                }
                Log.log("Ship id: "+ship.id+" Enroute: "+ship.enroute);
            }

            // for each ship with an action to move, settle movement disputes
            while(!(myShips.isEmpty())) {
                gameMap.newNavigate(myShips.get(0), myShips, commandQueue);
            }

            if (
                game.turnNumber <= (Constants.MAX_TURNS * .375) &&
                me.halite >= Constants.SHIP_COST &&
                !gameMap.at(me.shipyard).isOccupied())
            {
                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
        }
    }
}
