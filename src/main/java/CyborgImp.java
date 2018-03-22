import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;


public class CyborgImp extends JFrame {

	public static final String BOTNAME = "BOTNAMEHERE"; //Name of your Twitch bot account, in lower case
	public static final String OAUTH = "oauth:KEYHERE"; //The OAUTH key for your bot account
	public static final String CHANNEL = "YOURNAMEHERE"; //Name of your streaming Twitch account, in lower case

	public static PircBotX bot;

	//GUI components
	public static JPanel idleArea = new JPanel();
	public static JLabel idleText01 ,idleText02, idleText03;
	public static JPanel gameArea = new JPanel();
	public static JPanel lettersRow = new JPanel();
	public static JPanel gameRow01 = new JPanel();
	public static JPanel gameRow02 = new JPanel();
	public static JPanel gameRow03 = new JPanel();
	public static HashMap<String, JLabel> labels = new HashMap<>();
	public static JLabel grid01, grid02, grid03, gridA, gridB, gridC;
	public static JLabel tempLabel;

	public CyborgImp(){
		//Window configuration
		super("Tic Quad Toe");
		setSize(400,350);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);

		initialGUIsetup();
	}

	public static void boardReset()
	{
		labels.get("a1").setIcon(new ImageIcon("images/empty.png"));
		labels.get("a2").setIcon(new ImageIcon("images/empty.png"));
		labels.get("a3").setIcon(new ImageIcon("images/empty.png"));
		labels.get("b1").setIcon(new ImageIcon("images/empty.png"));
		labels.get("b2").setIcon(new ImageIcon("images/empty.png"));
		labels.get("b3").setIcon(new ImageIcon("images/empty.png"));
		labels.get("c1").setIcon(new ImageIcon("images/empty.png"));
		labels.get("c2").setIcon(new ImageIcon("images/empty.png"));
		labels.get("c3").setIcon(new ImageIcon("images/empty.png"));

		idleText02.setText("Type !TQT to start a match");
	}

	//Reset the GUI
	public void initialGUIsetup()
	{
		//Initial screen text
		idleText01 = new JLabel("TIC QUAD TOE");
		idleText01.setBorder(new EmptyBorder(75, 0, 0, 0));
		idleText01.setFont(new Font("DpQuake", Font.PLAIN, 32));
		idleArea.add(idleText01);
		idleText02 = new JLabel("Type !TQT to start a match");
		idleText02.setBorder(new EmptyBorder(10, 10, 75, 0));
		idleText02.setFont(new Font("Franklin Gothic Medium", Font.PLAIN, 24));
		idleArea.add(idleText02);
		idleText03 = new JLabel("");
		idleText03.setBorder(new EmptyBorder(0, 15, 0, 0));
		idleText03.setFont(new Font("Franklin Gothic Medium", Font.PLAIN, 24));
		idleArea.add(idleText03);
		add(idleArea);

		//Grid numbers
		grid01 = new JLabel(new ImageIcon("images/1.png"));
		grid01.setBorder(new EmptyBorder(0, 50, 0, 25));
		lettersRow.add(grid01);
		grid02 = new JLabel(new ImageIcon("images/2.png"));
		grid02.setBorder(new EmptyBorder(0, 25, 0, 25));
		lettersRow.add(grid02);
		grid03 = new JLabel(new ImageIcon("images/3.png"));
		grid03.setBorder(new EmptyBorder(0, 25, 0, 25));
		lettersRow.add(grid03);
		gameArea.add(lettersRow);

		//Grid letters
		gridA = new JLabel(new ImageIcon("images/a.png"));
		gameRow01.add(gridA);
		gridB = new JLabel(new ImageIcon("images/b.png"));
		gameRow02.add(gridB);
		gridC = new JLabel(new ImageIcon("images/c.png"));
		gameRow03.add(gridC);

		//Game board
		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("a1", tempLabel);
		gameRow01.add(labels.get("a1"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("a2", tempLabel);
		gameRow01.add(labels.get("a2"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("a3", tempLabel);
		gameRow01.add(labels.get("a3"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("b1", tempLabel);
		gameRow02.add(labels.get("b1"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("b2", tempLabel);
		gameRow02.add(labels.get("b2"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("b3", tempLabel);
		gameRow02.add(labels.get("b3"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("c1", tempLabel);
		gameRow03.add(labels.get("c1"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("c2", tempLabel);
		gameRow03.add(labels.get("c2"));

		tempLabel = new JLabel(new ImageIcon("images/empty.png"));
		labels.put("c3", tempLabel);
		gameRow03.add(labels.get("c3"));

		gameArea.add(gameRow01);
		gameArea.add(gameRow02);
		gameArea.add(gameRow03);

		gameArea.setPreferredSize(new Dimension(300, 350));
		gameArea.setVisible(false);

		add(gameArea);
	}

	public static void main(String[] args) throws Exception {

		//Launch window
		new CyborgImp();

		Configuration config = new Configuration.Builder()
				.setName(BOTNAME)
				.setServer("irc.chat.twitch.tv", 6667)
				.setServerPassword(OAUTH)
				.addListener(new Bot())
				.addAutoJoinChannel("#" + CHANNEL)
				.buildConfiguration();

		bot = new PircBotX(config);
		bot.startBot();
	}
}