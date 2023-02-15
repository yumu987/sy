package uk.ac.bris.cs.scotlandyard.model;


import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;


/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState{
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private static ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives)
		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
            //check
			if(mrX.isDetective()) throw new IllegalArgumentException("No MRX!");
			if(detectives.isEmpty()) throw new IllegalArgumentException("Detectives is null!");
			if(mrX.equals(null)) throw new IllegalArgumentException("MRX is null!");
			for(int tmp1 = 0; tmp1 < detectives.size(); tmp1++){
				for(int tmp2 = tmp1 + 1; tmp2 <detectives.size();tmp2++ ){
					if(detectives.get(tmp1).location() == detectives.get(tmp2).location())
						throw new IllegalArgumentException("The location of detectives are same!");
					if(detectives.get(tmp1).piece().equals(detectives.get(tmp2).piece()))
						throw new IllegalArgumentException("There are two same Detectives!");
				}
			}
			for(Player player : detectives){
				if(!player.tickets().get(ScotlandYard.Ticket.SECRET).equals(0))
					throw new IllegalArgumentException("Detectives has secret ticket!");
				if(!player.tickets().get(ScotlandYard.Ticket.DOUBLE).equals(0))
					throw new IllegalArgumentException("Detectives has double tickets!");
			}
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(setup.graph.edges().size() == 0 || setup.graph.nodes().size()== 0 )
				throw new IllegalArgumentException("Graph is empty!");
		}



		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			HashSet<Piece> all = new HashSet<>();
			all.add(mrX.piece());
			for (Player player :detectives){
				all.add(player.piece());
			}
			return ImmutableSet.copyOf(all);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
				for (Player player : detectives) {
					if (player.piece() == detective) {
						return Optional.of(player.location());
					}
				}
				return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			TicketBoard result = new TicketBoard(){
				@Override
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					if(piece.isMrX())
					return mrX.tickets().getOrDefault(ticket,0);
					if(piece.isDetective()){
						for(Player player : detectives){
							if(player.piece().equals(piece)){
								return player.tickets().getOrDefault(ticket,0);
							}
						}
					}
					return 0;
				}
			};

			if(piece.isMrX()){
				return Optional.of(result);
			}

		   for(Player player : detectives){
			   if(player.piece().equals(piece)){
				   return Optional.of(result);
			   }
		   }


             return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {

			//detectives piece
			Set<Piece> detectivesPiece = new HashSet<>();
			for (Player player : detectives){
				detectivesPiece.add(player.piece());
			}

			//mrx moves
			Set<Move> mrXMove = new HashSet<>();
			mrXMove.addAll(makeSingleMoves(setup,detectives,mrX, mrX.location()));
			mrXMove.addAll(makeDoubleMoves(setup,detectives,mrX, mrX.location()));

			//detectives moves
			Set<Move.SingleMove> detectivesMove = new HashSet<>();
			for (Player player : detectives){
				detectivesMove.addAll(makeSingleMoves(setup,detectives,player,player.location()));
			}

			//mrx destinations and detective location
			Set<Integer> mrxDestination = new HashSet<>();
			Set<Integer> detectiveLocation = new HashSet<>();
			for (int destination : setup.graph.adjacentNodes(mrX.location())){
				mrxDestination.add(destination);
			}
			for (Player player :detectives){
				detectiveLocation.add(player.location());
			}
			List<Piece> tmpReaming = new ArrayList<>(remaining);

			//mrX win
			//the travel log is full
			if (log.size() == setup.moves.size() && tmpReaming.contains(mrX.piece())) {
				return ImmutableSet.of(mrX.piece());
			}

            //detective win
			//mrx cornered
			if (mrxDestination.size() == detectiveLocation.size() && detectiveLocation.containsAll(mrxDestination)){//check the all destinations of Mrx has detectives
				return ImmutableSet.copyOf(detectivesPiece);
			}

			//mrX win
			//the detective can't move
			if (detectivesMove.size() == 0){
				for (Player player : detectives){
					if (player.location() == mrX.location()){
						return ImmutableSet.copyOf(detectivesPiece);
					}
					else return ImmutableSet.of(mrX.piece());
				}
			}

			//detective win
			//mrx was captured
			for (Player player : detectives){
				if (player.location() == mrX.location()){
					return ImmutableSet.copyOf(detectivesPiece);
				}
			}

			//mrx was stuck
            if (mrXMove.size() == 0 &&tmpReaming.get(0) == mrX.piece()){
				for (Player player : detectives){
					if (player.location() == mrX.location()){//check the detective can capture  in next rounds after mrx was stuck
						return ImmutableSet.copyOf(detectivesPiece);
					}
					else return ImmutableSet.of(mrX.piece());
				}
			}

			//detectives win if mrx cornered
			if (detectiveLocation.containsAll(mrxDestination) && detectivesMove.size()>0){
				if (mrXMove.size() == 0){
					return ImmutableSet.copyOf(detectivesPiece);
				}
			}

			return ImmutableSet.of();
		}


		//Single move
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.SingleMove> singleMoves = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				boolean locationOccupiedByDetective = false;//check  if the destination is occupied by a detectives

				for(Player detective : detectives){
					if(detective.location() == destination)
					    locationOccupiedByDetective = true;
				}

				if (locationOccupiedByDetective)
					continue;

				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					if(player.has(t.requiredTicket()))//check the ticket of player
					{
						Move.SingleMove tmp1 = new Move.SingleMove(player.piece(), source,t.requiredTicket(),destination);
								singleMoves.add(tmp1);
					}
					if(player.has(ScotlandYard.Ticket.SECRET)){
						Move.SingleMove tmp2 = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET,destination);
						singleMoves.add(tmp2);
					}
				}
			}

			return singleMoves;
		}

		//Double move
		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.DoubleMove> doubleMoves = new HashSet<>();
				for (int firstDestination : setup.graph.adjacentNodes(source)) {
						for (Player player1 : detectives) {
							boolean firstLocationOccupiedByDetective = false;
							if (player1.location() == firstDestination) {
								firstLocationOccupiedByDetective = true;
							}
							if (firstLocationOccupiedByDetective)
								continue;
							for (ScotlandYard.Transport firstTicket : setup.graph.edgeValueOrDefault(source, firstDestination, ImmutableSet.of())) {
								if (player.has(firstTicket.requiredTicket())) {
									for (int secondDestination1 : setup.graph.adjacentNodes(firstDestination)) {
										for (Player player2 : detectives) {
											boolean secondLocationOccupiedByDetective = false;
											if (player2.location() == secondDestination1) {
												secondLocationOccupiedByDetective = true;
											}
											if (secondLocationOccupiedByDetective)
												continue;
											for (ScotlandYard.Transport secondTicket : setup.graph.edgeValueOrDefault(firstDestination, secondDestination1, ImmutableSet.of())) {
												if (player.has(secondTicket.requiredTicket())) {
													if (firstTicket.requiredTicket()== secondTicket.requiredTicket()) {
														if (player.hasAtLeast(secondTicket.requiredTicket(), 2)) {
															Move.DoubleMove tmp1 = new Move.DoubleMove(player.piece(), source, firstTicket.requiredTicket(), firstDestination, secondTicket.requiredTicket(), secondDestination1);
															doubleMoves.add(tmp1);
														}
													}

													if (firstTicket.requiredTicket() != secondTicket.requiredTicket()) {
															Move.DoubleMove tmp2 = new Move.DoubleMove(player.piece(), source, firstTicket.requiredTicket(), firstDestination, secondTicket.requiredTicket(), secondDestination1);
															doubleMoves.add(tmp2);
													}
												}
												if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)) {//mrx use secret ticket in second move
													Move.DoubleMove tmp5 = new Move.DoubleMove(player.piece(), source, firstTicket.requiredTicket(), firstDestination, ScotlandYard.Ticket.SECRET, secondDestination1);
													doubleMoves.add(tmp5);
												}
											}
										}
									}
								}
								if (player.has(ScotlandYard.Ticket.SECRET)) {//mrx use secret ticket in first move
									for (int secondDestination2 : setup.graph.adjacentNodes(firstDestination)) {
										for (Player player2 : detectives) {
											boolean secondLocationOccupiedByDetective = false;
											if (player2.location() == secondDestination2) {
												secondLocationOccupiedByDetective = true;
											}
											if (secondLocationOccupiedByDetective)
												continue;
											for (ScotlandYard.Transport secondTicket2 : setup.graph.edgeValueOrDefault(firstDestination, secondDestination2, ImmutableSet.of())) {
												if (player.has(secondTicket2.requiredTicket())) {
													if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)) {
														Move.DoubleMove tmp3 = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, firstDestination, ScotlandYard.Ticket.SECRET, secondDestination2);
														doubleMoves.add(tmp3);
													}
													if (ScotlandYard.Ticket.SECRET != secondTicket2.requiredTicket()) {
														Move.DoubleMove tmp4 = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, firstDestination, secondTicket2.requiredTicket(), secondDestination2);
														doubleMoves.add(tmp4);
													}
												}
											}
										}
									}
								}


							}
						}
					}
			return doubleMoves;
		}



		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> availableMove = new HashSet<>();
			List<Piece> tmpReaming = new ArrayList<>(remaining);
			Piece currentPieceMove = tmpReaming.get(0);//to check the mrx or detectives to get available moves


			winner = getWinner();
			if (!winner.isEmpty()) return ImmutableSet.of();

			if (currentPieceMove.isMrX()) {
					if (mrX.has(ScotlandYard.Ticket.DOUBLE) && (setup.moves.size() - log.size() >= 2)) {
						availableMove.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
						availableMove.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
					} else {
						availableMove.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					}
				} else {
					for (Piece piece : tmpReaming) {
						for (Player detective : detectives) {
							if (detective.piece().equals(piece)) {
								availableMove.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
							}

						}
					}
				}

         return ImmutableSet.copyOf(availableMove);

		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			moves = getAvailableMoves();
			if(!moves.contains(move)) {
				throw new IllegalArgumentException("Illegal move: "+move);
			}

			List<Piece> newReaming = new ArrayList<>();
			List<Player> newDetectives = new ArrayList<>(detectives);
			List<LogEntry> updateLog = new ArrayList<>(log);

			// player location visitor pattern
			int destination = move.accept(new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});

			//the visitor of update log
			List<LogEntry> newLog = move.accept(new Move.Visitor<List<LogEntry>>() {
				List<LogEntry> tmp = new ArrayList<>();
				@Override
				public List<LogEntry> visit(Move.SingleMove move) {
					if (move.commencedBy().isMrX()){
						if (setup.moves.get(log.size())){//the mrX need to show the location in this round
							tmp.add(LogEntry.reveal(move.ticket,move.destination));
						}
						else tmp.add(LogEntry.hidden(move.ticket));
					}
					return tmp;
				}

				@Override
				public List<LogEntry> visit(Move.DoubleMove move) {
					if (move.commencedBy().isMrX()){
						if (setup.moves.get(log.size())){//the mrX need to show the first destination in this round(when use double ticket)
							tmp.add(LogEntry.reveal(move.ticket1,move.destination1));
						}
						else {
							tmp.add(LogEntry.hidden(move.ticket1));
						}
						if (setup.moves.get(log.size()+1)){//the mrX need to show the second destination in next round(when use double ticket)
							tmp.add(LogEntry.reveal(move.ticket2,move.destination2));
						}
						else tmp.add(LogEntry.hidden(move.ticket2));
					}
					return tmp;
				}
			});

			//update the location and tickets with mrX and detectives
			if (move.commencedBy().isMrX()){
				//update mrX tickets and location
				mrX = mrX.use(move.tickets());
				mrX = mrX.at(destination);

				//update detectives and reaming
				for (Player player : detectives) {
					//the no moves detective will remove
					if (!makeSingleMoves(setup,detectives,player,player.location()).isEmpty()){
					newReaming.add(player.piece());
					}
				}
				//update log
				updateLog.addAll(newLog);
			}
			if (move.commencedBy().isDetective()){
					for(Player player : detectives) {
						//update detective location and tickets
						if (player.piece().equals(move.commencedBy())) {
							newDetectives.remove(player);//remove the previous player
							player = player.at(destination);
							player = player.use(move.tickets());
							newDetectives.add(player);//add back the update player
							mrX = mrX.give(move.tickets());//give mrx ticket which detective used
					    }
					//update remaining
					newReaming.addAll(remaining);
					newReaming.remove(move.commencedBy());
					if (newReaming.size() == 0) {
						newReaming.add(mrX.piece());
					}
					}
			 }
			
			return new MyGameState(setup,ImmutableSet.copyOf(newReaming), ImmutableList.copyOf(updateLog),mrX,newDetectives);
     }
	}

	@Nonnull
	@Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}
