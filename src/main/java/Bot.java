import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.PingEvent;
import org.pircbotx.hooks.events.UnknownEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import javax.swing.*;
import java.util.*;
import java.util.Timer;

public class Bot extends ListenerAdapter {

	//USE WHISPERS
	public boolean USE_WHISPERS = true;

	//Used to send one time CAP message that allows us to later send whispers
	public boolean SENTRAW = false;

	//Mods usernames must be lowercase
	private List<String> mods = new ArrayList<>();

	private boolean TTTattempt = false; //Tracks if there is an attempt to start a match, waiting for 2nd player
	private boolean TTTonGoing = false; //Tracks is a match is currently in progress
	private boolean noMoreMoves = false; //Disallows further moves after game is completed
	private int numPlays = 0; //Tracks the number of plays in current game, used for draws
	private boolean Xturn = true; //Tracks player turns
	private String TTTplayerOne, TTTplayerTwo; //Player names
	private HashMap<String, String> TTTboard; //Actual game board

	private Timer playerJoinTimer, playerTurnTimer, gameEndTimer; //Used for tracking game start, player turns and victory screen timeouts

	//Add yourself as a mod
	public Bot() { mods.add("YOURNAMEHERE"); }


	//Process the TQT command
	private String processTQT(String nick)
	{
		//Player one attempts to start a match
		if (!TTTattempt && !TTTonGoing)
		{
			TTTattempt = true;
			TTTplayerOne = nick;
			CyborgImp.idleText02.setText("<html>Waiting on player 2.<br><br>Type !TQT to join!</html>");
			playerJoinTimer = new Timer();
			//Begin timeout counter (1 min)
			playerJoinTimer.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					CyborgImp.bot.sendIRC().message("#" + CyborgImp.CHANNEL, "No one else wants to play, match cancelled. BibleThump");
					CyborgImp.idleText02.setText("Type !TQT to start a match");
					TTTattempt = false;
				}
			}, 60000);
		}
		//Prevent the same person from becoming first and second player
		else if (TTTattempt && nick.equals(TTTplayerOne))
			return "We don't really want to see you play with yourself @" + nick;
		//Second player joins in
		else if (TTTattempt && !nick.equals(TTTplayerOne))
		{
			//Cancel game start timeout timer
			playerJoinTimer.cancel();
			playerJoinTimer.purge();

			//Initialize variables and board
			TTTattempt = false;
			TTTonGoing = true;
			resetTTTboard();

			TTTplayerTwo = nick;

			choosePlayerOne(); //Randomly select who plays first
			startPlayerTurnTimer(); //Start player turn timeout timer (1 min)
			//Inform both players through whispers of their turns and commands
			if (USE_WHISPERS)
			{
				CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " + TTTplayerOne + " You go first. Use !a1, !c3, etc to make your choices. 60 seconds to make each move.");
				CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " + TTTplayerTwo + " You go second. Use !a1, !c3, etc to make your choices. 60 seconds to make each move.");
			}
			return "Starting new match. @" + TTTplayerOne + " goes first.";
		}
		//Prevents people from starting another match while there is still one in progress
		else
			return "A match is in progress @" + nick + ", wait for it to end.";
		return null;
	}

	//This will return the response from the command
	private String runCommands(GenericMessageEvent event, String command) {
		//This must be sent to server once, before being allowed to send whispers
		if(!SENTRAW)
		{
			CyborgImp.bot.sendRaw().rawLineNow("CAP REQ :twitch.tv/commands");
			SENTRAW = true;
		}

		command = command.toLowerCase();

		//Process !tqt, game start command
		if(command.equals("!tqt"))
			return processTQT(event.getUser().getNick());
		//Process player movement commands
		else if(command.equals("!a1") || command.equals("!a2") || command.equals("!a3") || command.equals("!b1") || command.equals("!b2") || command.equals("!b3") || command.equals("!c1") || command.equals("!c2") || command.equals("!c3"))
		{
			//Remove !
			command = command.substring(1);
			if (!TTTonGoing)
				return "There is no ongoing match.";
			else if ((Xturn && event.getUser().getNick().equals(TTTplayerOne)) || (!Xturn && event.getUser().getNick().equals(TTTplayerTwo)))
				return placeMove(command);
			else
				return (Xturn) ? "It's " + TTTplayerOne + "'s turn, and you're not him/her!" : "It's " + TTTplayerTwo + "'s turn, and you're not him/her!";
		}
		//Test whisper command
		else if(command.equals("!whisper"))
		{
			if(USE_WHISPERS)
				CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " +event.getUser().getNick() +" I dont know what I'm doing Kappa");
			else
				return "Whispers are not enabled.";
		}
		//Disable sending of whispers (only mods can use this command)
		else if(mods.contains(event.getUser().getNick()) && command.equals("!nowhispers"))
			USE_WHISPERS = false;
		//Enable sending of whispers (only mods can use this command)
		else if(mods.contains(event.getUser().getNick()) && command.equals("!usewhispers"))
			USE_WHISPERS = true;

		return null;
	}

	//Randomly selects who goes first
	private void choosePlayerOne(){
		String tempOne = TTTplayerOne;
		String tempTwo = TTTplayerTwo;

		Random rnd = new Random();
		int choice = 1 + rnd.nextInt(101);
		if(choice <= 50)
		{
			TTTplayerOne = tempTwo;
			TTTplayerTwo = tempOne;
		}
	}

	//Tracks time that each player has for his turn (1min)
	private void startPlayerTurnTimer(){
		playerTurnTimer = new Timer();
		playerTurnTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				//Time runs out, player forfeits
				if(Xturn)
				{
					CyborgImp.bot.sendIRC().message("#" + CyborgImp.CHANNEL, TTTplayerOne +" seems to be asleep, " +TTTplayerTwo +" wins by default");
					gameWon("f1");
				}
				else
				{
					CyborgImp.bot.sendIRC().message("#" + CyborgImp.CHANNEL, TTTplayerTwo +" seems to be asleep, " +TTTplayerOne +" wins by default");
					gameWon("f2");
				}

			}
		}, 60000);
	}

	//Reset the game board
	private void resetTTTboard()
	{
		TTTboard = new HashMap<>();
		TTTboard.put("a1", " ");
		TTTboard.put("a2", " ");
		TTTboard.put("a3", " ");
		TTTboard.put("b1", " ");
		TTTboard.put("b2", " ");
		TTTboard.put("b3", " ");
		TTTboard.put("c1", " ");
		TTTboard.put("c2", " ");
		TTTboard.put("c3", " ");

		//Change GUI back to initial screen
		CyborgImp.idleArea.setVisible(false);
		CyborgImp.gameArea.setVisible(true);
		noMoreMoves = false; //Disable move restriction
	}

	//Places a move on the board
	private String placeMove(String move)
	{
		//After highlighting the winning move, players are not allowed to make moves anymore
		if(noMoreMoves)
			return null;

		//Place is already taken by a previous move, inform player
		if(!TTTboard.get(move).equals(" "))
			return "Place is already taken! Please choose a different one.";
		//Place is empty, allow move
		else
		{
			//Stop the timer for this turn
			playerTurnTimer.cancel();
			playerTurnTimer.purge();

			//First player turn, place an X on the board and display a Quad
			if(Xturn)
			{
				TTTboard.put(move, "X");
				CyborgImp.labels.get(move).setIcon(new ImageIcon("images/Quad.png"));
			}
			//Second player turn, place an O on the board and display an Invulnerability
			else
			{
				TTTboard.put(move, "O");
				CyborgImp.labels.get(move).setIcon(new ImageIcon("images/Invuln.png"));
			}
			Xturn = !Xturn; //Next player turn
			numPlays = numPlays + 1; //Increase the number of plays in this game

			//Start timer for next player turn
			startPlayerTurnTimer();
			//Check for win condition
			checkWinConditions();
		}
		return null;
	}

	//Check if there is a win condition for the current match
	public void checkWinConditions()
	{
		//Check each column
		if(!TTTboard.get("a1").equals(" ") && TTTboard.get("a1").equals(TTTboard.get("b1")) && TTTboard.get("a1").equals(TTTboard.get("c1")))
		{
			highlightWinningCombo("a1", "b1", "c1", TTTboard.get("a1"));
			gameWon(TTTboard.get("a1"));
		}
		else if(!TTTboard.get("a2").equals(" ") && TTTboard.get("a2").equals(TTTboard.get("b2")) && TTTboard.get("a2").equals(TTTboard.get("c2")))
		{
			highlightWinningCombo("a2", "b2", "c2", TTTboard.get("a2"));
			gameWon(TTTboard.get("a2"));
		}
		else if(!TTTboard.get("a3").equals(" ") && TTTboard.get("a3").equals(TTTboard.get("b3")) && TTTboard.get("a3").equals(TTTboard.get("c3")))
		{
			highlightWinningCombo("a3", "b3", "c3", TTTboard.get("a3"));
			gameWon(TTTboard.get("a3"));
		}
		//Check each row
		else if(!TTTboard.get("a1").equals(" ") && TTTboard.get("a1").equals(TTTboard.get("a2")) && TTTboard.get("a1").equals(TTTboard.get("a3")))
		{
			highlightWinningCombo("a1", "a2", "a3", TTTboard.get("a1"));
			gameWon(TTTboard.get("a1"));
		}
		else if(!TTTboard.get("b1").equals(" ") && TTTboard.get("b1").equals(TTTboard.get("b2")) && TTTboard.get("b1").equals(TTTboard.get("b3")))
		{
			highlightWinningCombo("b1", "b2", "b3", TTTboard.get("b1"));
			gameWon(TTTboard.get("b1"));
		}
		else if(!TTTboard.get("c1").equals(" ") && TTTboard.get("c1").equals(TTTboard.get("c2")) && TTTboard.get("c1").equals(TTTboard.get("c3")))
		{
			highlightWinningCombo("c1", "c2", "c3", TTTboard.get("c1"));
			gameWon(TTTboard.get("c1"));
		}
		//Check the diagonals
		else if(!TTTboard.get("a1").equals(" ") && TTTboard.get("a1").equals(TTTboard.get("b2")) && TTTboard.get("a1").equals(TTTboard.get("c3")))
		{
			highlightWinningCombo("a1", "b2", "c3", TTTboard.get("a1"));
			gameWon(TTTboard.get("a1"));
		}
		else if(!TTTboard.get("c1").equals(" ") && TTTboard.get("c1").equals(TTTboard.get("b2")) && TTTboard.get("c1").equals(TTTboard.get("a3")))
		{
			highlightWinningCombo("c1", "b2", "a3", TTTboard.get("c1"));
			gameWon(TTTboard.get("c1"));
		}
		//No winning condition and out of moves, declare a draw
		else if(numPlays == 9)
			gameWon("Draw");
	}

	//Highlight the winning move with a different image
	public void highlightWinningCombo(String posOne, String posTwo, String posThree, String winner){
		//First player won
		if(winner.equals("X"))
		{
			CyborgImp.labels.get(posOne).setIcon(new ImageIcon("images/QuadWon.png"));
			CyborgImp.labels.get(posTwo).setIcon(new ImageIcon("images/QuadWon.png"));
			CyborgImp.labels.get(posThree).setIcon(new ImageIcon("images/QuadWon.png"));
		}
		//Second player won
		else
		{
			CyborgImp.labels.get(posOne).setIcon(new ImageIcon("images/InvulnWon.png"));
			CyborgImp.labels.get(posTwo).setIcon(new ImageIcon("images/InvulnWon.png"));
			CyborgImp.labels.get(posThree).setIcon(new ImageIcon("images/InvulnWon.png"));
		}
	}

	//Game ended
	public void gameWon(String winner)
	{
		noMoreMoves = true; //Disallow further moves
		numPlays = 0;
		playerTurnTimer.cancel(); //Stop player turn timer
		playerTurnTimer.purge();

		//Update the initial screen label with the appropriate text
		if(winner.equals("Draw"))
			CyborgImp.idleText03.setText("Last match was a draw.");
		else
		{
			if (winner.equals("X") || winner.equals("f2"))
				CyborgImp.idleText03.setText("Last winner: " +TTTplayerOne);
			else
				CyborgImp.idleText03.setText("Last winner: " +TTTplayerTwo);
		}

		//Show the winning move for a few seconds (10) before moving back to the initial screen
		gameEndTimer = new Timer();
		gameEndTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				//Reset variables and the game board GUI
				TTTonGoing = false;
				TTTattempt = false;
				Xturn = true;
				resetTTTboard();
				CyborgImp.boardReset();
				CyborgImp.gameArea.setVisible(false);
				CyborgImp.idleArea.setVisible(true);
			}
		}, 10000);
	}

	/**
	 * PircBotx will return the exact message sent and not the raw line
	 */
	@Override
	public void onGenericMessage(GenericMessageEvent event) {
		String message = event.getMessage();
		String command = getCommandFromMessage(message);

		String response = runCommands(event, command);
		if(response != null) sendMessage(response);
	}

	/**
	 * The command will always be the first part of the message
	 * We can split the string into parts by spaces to get each word
	 * The first word if it starts with our command notifier "!" will get returned
	 * Otherwise it will return null
	 */
	private String getCommandFromMessage(String message) {
		String[] msgParts = message.split(" ");
		if (msgParts[0].startsWith("!")) {
			return msgParts[0];
		} else {
			return null;
		}
	}

	/**
	 * We MUST respond to this or else we will get kicked
	 */
	@Override
	public void onPing(PingEvent event) throws Exception {
		CyborgImp.bot.sendRaw().rawLineNow(String.format("PONG %s\r\n", event.getPingValue()));
	}

	private void sendMessage(String message) {
		CyborgImp.bot.sendIRC().message("#" + CyborgImp.CHANNEL, message);
	}

	//This method will be used whenever the bot receives a whisper
	@Override
	public void onUnknown(UnknownEvent event){
		//Whispers are disabled, ignore them
		if(!USE_WHISPERS)
			return;

		//Since it is a raw message from the server, we must parse it ourselves
		//System.out.println(event.getLine());
		String nick, command;
		String [] lineParts;
		String receivedLine = event.getLine();

		//Message is a whisper
		if(receivedLine.contains("WHISPER"))
		{
			//Parse command from whisper
			lineParts  = receivedLine.split("!");
			nick = lineParts[0].substring(1);
			command = lineParts[lineParts.length-1];
			//System.out.println("Command was " +command +", from user " +nick);

			//In case the first message the bot receives is a whisper, we send server command that allows us to send whispers here
			if(!SENTRAW)
			{
				CyborgImp.bot.sendRaw().rawLineNow("CAP REQ :twitch.tv/commands");
				SENTRAW = true;
			}

			command = command.toLowerCase();
			String answer;

			//Process TQT command
			if(command.equals("tqt"))
			{
				answer = processTQT(nick);
				if(answer != null)
					CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " +nick +" " +answer);
			}
			//Process game commands
			else if(command.equals("a1") || command.equals("a2") || command.equals("a3") || command.equals("b1") || command.equals("b2") || command.equals("b3") || command.equals("c1") || command.equals("c2") || command.equals("c3"))
			{
				//Someone tried to use a game command with no game in progress
				if(!TTTonGoing)
					CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " +nick +" There is no ongoing match.");
				//Correct player issued a game command
				else if((Xturn && nick.equals(TTTplayerOne)) || (!Xturn && nick.equals(TTTplayerTwo)))
				{
					answer = placeMove(command);
					if(answer != null)
						CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " + nick + " " +answer);
				}
				//Player tried to make a move when it's not his turn
				else
					CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " +nick +" " +((Xturn) ? "It's " +TTTplayerOne +"'s turn, and you're not him/her!" : "It's " +TTTplayerTwo +"'s turn, and you're not him/her!"));
			}
			//Whisper test message
			else if(command.equals("whisper"))
					CyborgImp.bot.sendRaw().rawLineNow("PRIVMSG #jtv :/w " +nick +" Echo test message!");
		}
	}
}