package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	@Nonnull @Override
	public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		return new Model() {
			Board.GameState gameState = new MyGameStateFactory().build(setup, mrX, detectives);
			Set<Observer> observers = new HashSet<>();
			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return gameState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observer.equals(null))
					throw new NullPointerException("This observer is null");
				if (observers.contains(observer))
					throw new IllegalArgumentException("The observers has this observer");
				observers.add(observer);
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer.equals(null))
					throw new NullPointerException("no observer");
			    if (!observers.contains(observer))
				throw new IllegalArgumentException("don't have this observer");
				observers.remove(observer);
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observers);
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				gameState = gameState.advance(move);//update the gameSate
				for (Observer observer : observers) {
				if (gameState.getWinner().isEmpty()) {//use winner to chooseMove
						observer.onModelChanged(gameState, Observer.Event.MOVE_MADE);

				} else {
						observer.onModelChanged(gameState, Observer.Event.GAME_OVER);
				}
			}
			}
		};
	}
}
