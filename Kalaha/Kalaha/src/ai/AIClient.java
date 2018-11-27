package ai;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;
import ai.Node;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
    	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /*--------------------------------------------- --------------------*/
    
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {
        GameState startSate = currentBoard.clone();
        int max = player;
        int min = 1;
        if (max == 1) 
        {
            min = 2;
        }
        long startTime = System.currentTimeMillis();
        Node bestmove = new Node();
        int maxtime = 5;  //maximum time for iterative deepening search
        while (maxtime >= ((double) (System.currentTimeMillis() - startTime) / (double) 1000))
        {
            Node newmove = MinMax(startSate, 5, max, min, startTime, -999, 999);//5 is the depth in this case
            //checking the search
            if(newmove.utility > bestmove.utility)
            {
                bestmove.utility = newmove.utility;
                bestmove.ambos = newmove.ambos;
            }
        }
        if (bestmove.ambos <= 0) 
        {
            return getRandom();
        }
        return bestmove.ambos;
    }
    
    //Grade B: iterative deepening search with alpha beta pruning
    private Node MinMax(GameState gameState, int depth, int max, int min, long startTime, int alpha, int beta) {
        int maxtime = 5;
        //checking the game state whether it is in terminal state if so we assign utility
        if (gameState.gameEnded()) {
            Node move = new Node();
            if (gameState.getWinner() == max) {
                move.utility = 999;
            } else {
                move.utility = -999;
            }
            return move;
        } else if (depth <= 0 || maxtime <= ((double) (System.currentTimeMillis() - startTime) / (double) 1000)) {
            //checking if it reached the terminal state and if has reached the maximum time
            Node move = new Node();
            move.utility = gameState.getScore(max) - gameState.getScore(min);
            return move;
        }
        
        int player = gameState.getNextPlayer();
        int utility = 0;
        int bestMove = 1;
        

 /*             Designed This Algorithm According To
  * 		Alpha-Beta Pruning Principle in MiniMax Algorithm.  
  * 
  *      
  *      if maximizingPlayer
             
             maxEval=-infinity
		     
		     for each child of position
			
				eval=minimax(state, depth-1, max, min, startTime, alpha, beta);
				
				maxEval=max(maxEval,eval);
				
				if eval > maxEval
				   maxEval=eval
				   bestMove= i   //i--position of child
				
				
				alpha=max(alpha,eval)
				
				if(beta<=alpha)
	   			  break

		   return maxEval
		   
		 else 
		     minEval=-infinity
		     
		     for each child of position
			
				eval=minimax(state, depth-1, max, min, startTime, alpha, beta);
				
				minEval=max(minEval,eval);
				
				if eval < maxEval
				   maxEval=eval
				   bestMove= i   //i--position of child
				
				
				beta=min(beta,eval)
				
				if(beta<=alpha)
	   			  break

		   return minEval
   
  * */     
        
if (player == max)//max for maximizingPlayer 
{
utility = -999;
for (int i = 1; i <=6; i++)
{
	if (gameState.moveIsPossible(i)) //check whether the move is possible or not
	{
        GameState state = gameState.clone();//cloning
        
        state.makeMove(i);//making the move
        
        Node move = MinMax(state, --depth, max, min, startTime, alpha, beta);

	if (move.utility > utility) 
		{
                       utility = move.utility;
                       bestMove = i;
        }
		//alpha beta pruning
		alpha=Math.max(alpha, move.utility);
		
		if(alpha>=beta)
			break;	
}

  }

}
else
{
  utility = 999;
               	
  for (int i = 1; i <= 6; i++) // selecting the best move among all cases
  {
        if (gameState.moveIsPossible(i)) //check whether the move is possible or not
        {
        GameState state = gameState.clone();//cloning
               
        state.makeMove(i);//making the move
               
        Node move = MinMax(state, depth-1, max, min, startTime, alpha, beta);
               
        if (move.utility < utility) 
               	   {
                       utility = move.utility;
                       bestMove = i;
               }	
        		     //alpha beta pruning
                     beta=Math.min(beta, move.utility);
                     if(beta<=alpha)
                    	 break;
	}
  
  } 

}
        
        Node move = new Node();
        move.utility = utility;
        move.ambos = bestMove;
        return move;
    }

    
    
    /*------------------------------------------------------------------*/
    
    
    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     * 
     * @return Random ambo number
     */
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }
}
