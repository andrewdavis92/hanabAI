package agents;

import hanabAI.*;


public class Agent20743192 implements Agent {
    //2D array to hold information about own cards and what each player knows about their own cards
    private Cards[][] hand;
    //Check to see if first action to initialise agent
    private boolean firstAction = true;
    //The number of players in the game
    private int numPlayers;
    //What number player the agent is
    private int index;


    /**
     * An inner class to hold information about the agents cards
     * Used to determine what the cards are
     **/
    protected class Cards {
        //What colour the card is or null if no hints given so far
        private Colour colour;
        //What number the card is or 0 if no hints given so far
        private int number;
        //How many rounds the card has been in the hand for
        private int cardAge;

        /**
         * Constructor for Card object, set everything to default values
         */
        Cards() {
            this.colour = null;
            this.number = 0;
            this.cardAge = 0;
        }

        /**
         * A function to set the colour of a particular card
         * @param colour the colour to set the card to
         */
        void setColour(Colour colour) {
            this.colour = colour;
        }

        /**
         * Get the colour in this card
         * @return Colour the colour of the card
         */
        Colour getColour() {
            return this.colour;
        }

        /**
         * A function to set the number of a particular card
         * @param number the number to set the card to
         */
        void setNumber(int number) {
            this.number = number;
        }

        /**
         * Get the number of this card
         * @return int the number of the card
         */
        int getNumber() {
            return this.number;
        }

        /**
         * Increase the age of the card by one
         */
        void incrementCardAge() {
            this.cardAge++;
        }

        /**
         * Get the age of the card (How many rounds it has been in the hand for)
         * @return int the age of the card
         */
        int getCardAge() {
            return cardAge;
        }

        /**
         * Reset the card, used when a card is discarded or played
         */
        void resetCard() {
            this.colour = null;
            this.number = 0;
            this.cardAge = 0;
        }
    }


    /**
     * Default constructor, creates a new instance of Agent20743192
     **/
    public Agent20743192() {
    }

    /**
     * Initialises variables on the first call to do action.
     * This code has been modified from the original BasicAgent code
     * @param s the State of the game at the first action
     **/
    private void initialiseHands(State s) {
        //Get the number of players
        numPlayers = s.getPlayers().length;
        //Sets the number of cards based on the number of players
        //Sets up the 2D array to hold information about the hands depending on the number of players
        if (numPlayers > 3) {
            hand = new Cards[numPlayers][4];
            for (int i = 0; i < numPlayers; i++) {
                for (int j = 0; j < 4; j++) {
                    hand[i][j] = new Cards();
                }
            }
        } else {
            hand = new Cards[numPlayers][5];
            for (int i = 0; i < numPlayers; i++) {
                for (int j = 0; j < 5; j++) {
                    hand[i][j] = new Cards();
                }
            }
        }
        //Find out what number player the agent is
        index = s.getNextPlayer();
        //So the agent doesn't do this method again
        firstAction = false;
    }

    /**
     * Returns the name of the agent.
     * This code is kept the same as BasicAgent
     * @return the String "Agent20743192"
     */
    public String toString() {
        return "Agent20743192";
    }

    /**
     * Performs an action given a state.
     * Assumes that they are the player to move.
     *
     * This code is slightly modified from BasicAgent
     *
     * @param s the current state of the game.
     * @return the action the player takes.
     **/
    public Action doAction(State s) {
        //If the first time, initialise agent
        if (firstAction) {
            initialiseHands(s);
        }
        try {
            //Get previous hints
            getPreviousPlays( s );
            //Get previous discards and plays
            getDiscardsPlays( s );
            //Play any known cards
            Action a = playKnown( s );
            //Play any cards where only 1 card hinted at
            //Assumes that the player hinted at the card for a reason
            if ( a == null ) {
                a = playOnes( s );
            }
            //Hint to another player about a card that is playable
            if ( a == null ) {
                a = hintPlayableCard( s );
            }
            //Discard a card known to be unplayable
            if ( a == null ) {
                a = discardKnown( s );
            }
            //Discard a card that is the oldest with no hints, assumed to be useless
            if ( a == null ) {
                a = discardOldestCard( s );
            }
            //If no other moves make a random hint (catch all in case, so a move has to be performed)
            if ( a == null) {
                a = hintRandom(s);
            }
            //Increment the card age for every player
            for (int i = 0; i < numPlayers; i++) {
                for (int j = 0; j < hand[index].length; j++) {
                    hand[i][j].incrementCardAge();
                }
            }
            //Return the move
            return a;
            //Catch illegal actions
        } catch (IllegalActionException e) {
            e.printStackTrace();
            throw new RuntimeException("Something has gone very wrong");
        }
    }

    /**
     * Gets the previous hints for every player and inserts them into hands
     * to keep track of what information each player has, this method is derived from the same
     * method in basicAgent, but it tracks for all players not just the agent
     * @param s the state of previous rounds
     */
    private void getPreviousPlays(State s) {
        try {
            //Clone the previous states
            State t = (State) s.clone();
            // For each of the previous moves
            for (int i = 0; i < Math.min(numPlayers - 1, s.getOrder() ); i++) {
                //Determine if a hint was given
                Action a = t.getPreviousAction();
                if ((a.getType() == ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE)) {
                    //Determine who received the hint
                    int receiver = a.getHintReceiver();
                    boolean[] hints = a.getHintedCards();
                    //Set the hints for the array that holds what players know about their own hands
                    for (int j = 0; j < hints.length; j++) {
                        if (hints[j]) {
                            if (a.getType() == ActionType.HINT_COLOUR) {
                                hand[receiver][j].setColour(a.getColour());
                            } else {
                                hand[receiver][j].setNumber(a.getValue());
                            }
                        }
                    }
                }
                //Continue to loop over each move
                t = t.getPreviousState();
            }
        } catch (IllegalActionException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method determines which cards were discarded or played to remove them from the hand array
     * @param s the state of previous moves
     */
    private void getDiscardsPlays(State s) {
        try {
            //Clone the state
            State t = (State) s.clone();
            //For each of the previous moves
            for ( int i = 0; i < Math.min( numPlayers - 1, s.getOrder() ); i++ ) {
                Action a = t.getPreviousAction();
                //If the action was a play or discard
                if ( ( a.getType() == ActionType.PLAY || a.getType() == ActionType.DISCARD ) ) {
                    //Reset the card information in hand
                    int player = a.getPlayer();
                    int card = a.getCard();
                    hand[player][card].resetCard();
                }
                //Continue to loop over each move
                t = t.getPreviousState();
            }
        } catch ( IllegalActionException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Determines if a card is playable using the stacks of fireworks already played.
     * This method is from basicAgent
     * @param s the current game state, used to get the fireworks stacks
     * @param c the colour to check
     * @return the value of the next playable car
     */
    private int playable(State s, Colour c) {
        //Create a stack for a particular fireworks colour
        java.util.Stack<Card> fw = s.getFirework(c);
        //If the stack is complete return -1
        if (fw.size() == 5) {
            return -1;
        } else {
            //Return the next card for that colour
            return (fw.size() + 1);
        }
    }

    /**
     * Plays the first card in the agent's hand that is playable using.
     * This method is adapted from basicAgent
     * @param s the game state
     * @return the action of playing a card, or null if no card is played
     * @throws IllegalActionException if the action is not allowed
     */
    private Action playKnown( State s ) throws IllegalActionException {
        for ( int i = 0; i < hand[index].length; i++ ) {
            if ( hand[index][i].colour != null && ( hand[index][i].number != 0 && hand[index][i].number == playable( s, hand[index][i].colour ) ) ) {
                hand[index][i].resetCard();
                return new Action( index, toString(), ActionType.PLAY, i );
            }
        }
        return null;
    }

    /**
     * Plays any ones if there are no fireworks started yet, otherwise plays a card if that card was hinted and no other cards were hinted at the same time
     * @param s the game state
     * @return the action of playing a card or null if no card is played
     * @throws IllegalActionException if the action is not allowed
     */
    private Action playOnes(State s) throws IllegalActionException {
        //Get the size of the fireworks play to determine if any have been started
        int fireWorkSize = 0;
        //Loop over each colour checking the size of the stack
        for ( Colour fwColour : Colour.values() ) {
            if ( s.getFirework(fwColour ).size() > 0) {
                fireWorkSize = s.getFirework(fwColour).size();
            }
        }
        //If no fireworks have started
        if ( fireWorkSize == 0 ) {
            //Play any ones in the hand
            for (int i = 0; i < hand[index].length; i++) {
                if (hand[index][i].getNumber() == 1) {
                    hand[index][i].resetCard();
                    return new Action( index, toString(), ActionType.PLAY, i );
                }
            }
        }
        //Clone the game state
        State t = ( State ) s.clone();
        //Loop over the previous moves to find any hints given to the agent
        for ( int i = 0; i < Math.min(numPlayers - 1, s.getOrder() ); i++ ) {
            //The number of hints fro a previous move
            int numHints = 0;
            //The index of the card hinted at, only applicable if one card was hinted
            int cardNo = 0;
            Action a = t.getPreviousAction();
            //Get hints towards the agent
            if ( ( a.getType() == ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE ) && a.getHintReceiver() == index ) {
                boolean[] hints = t.getPreviousAction().getHintedCards();
                //Count the number of cards hinted
                for ( int j = 0; j < hints.length; j++ ) {
                    if ( hints[j] ) {
                        numHints++;
                        cardNo = j;
                    }
                }
            }
            //If only one card was hinted, play that card
            if ( numHints == 1 ) {
                hand[index][i].resetCard();
                return new Action(index, toString(), ActionType.PLAY, cardNo);
            }
        }
        return null;
    }

    /**
     * Discards any cards that are known to be unplayable
     * @param s the game state
     * @return the action of discarding if a card is unplayable otherwise null if nothing is discarded
     * @throws IllegalActionException if the action is not allowed
     */
    private Action discardKnown( State s ) throws IllegalActionException {
        //Check that a discard can happen
        if ( s.getHintTokens() != 8 ) {
            //Loop over the know information of the cards to find one unplayable
            for (int i = 0; i < hand[index].length; i++) {
                if ( hand[index][i].getColour() != null && hand[index][i].getNumber() > 0 && hand[index][i].getNumber() < playable( s, hand[index][i].colour ) ) {
                    //Reset the card in hand
                    hand[index][i].resetCard();
                    //Discard the card
                    return new Action( index, toString(), ActionType.DISCARD, i );
                }
            }
        }
        return null;
    }

    /**
     * Hint at any playable cards in other players hands
     * This method is adapted from basicAgent
     * @param s the game state
     * @return the action of hinting a card or null if nothing is hinted
     * @throws IllegalActionException if the action is not allowed
     */
    private Action hintPlayableCard(State s) throws IllegalActionException {
        //Check to see if there are hint tokens
        if (s.getHintTokens() > 0) {
            //Loop through each player
            for ( int i = 1; i < numPlayers; i++ ) {
                int hintee = ( (index + i) % numPlayers );
                //An array for the other players hand
                Card[] otherPlayerHand = s.getHand(hintee);
                //Go over each card
                for ( int j = 0; j < otherPlayerHand.length; j++ ) {
                    Card c = otherPlayerHand[j];
                    //If the card is playable
                    if ( c != null && c.getValue() == playable( s, c.getColour() ) ) {
                        //CHeck if they have a had a value hint on that card before
                        if ( hand[hintee][j].getNumber() == 0 ) {
                            //Check which cards need to be hinted at
                            boolean[] val = new boolean[otherPlayerHand.length];
                            for ( int k = 0; k < val.length; k++ ) {
                                val[k] = c.getValue() == ( otherPlayerHand[k] == null ? - 1 : otherPlayerHand[k].getValue() );
                                if ( val[k] ) {
                                    //Put the hint in the hand array
                                    hand[hintee][k].setNumber( c.getValue() );
                                }
                            }
                            //Play the hint
                            return new Action( index, toString(), ActionType.HINT_VALUE, hintee, val, c.getValue() );//
                        }
                        //Check if a colour hint has been given before
                        else if ( hand[hintee][j].getColour() == null ) {
                            //Check which cards to hint at
                            boolean[] col = new boolean[otherPlayerHand.length];
                            for ( int k = 0; k < col.length; k++ ) {
                                col[k] = c.getColour().equals( ( otherPlayerHand[k] == null ? null : otherPlayerHand[k].getColour() ) );
                                if ( col[k] ) {
                                    //Put the hint in an array
                                    hand[hintee][k].setColour(c.getColour());
                                }
                            }
                            //Play the hint
                            return new Action( index, toString(), ActionType.HINT_COLOUR, hintee, col, c.getColour() );
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Discard the oldest card with no information
     * @param s the game state
     * @return the action to be played or null if nothing discarded
     * @throws IllegalActionException if the action is not allowed
     */
    private Action discardOldestCard( State s ) throws IllegalActionException {
        //Check to see if card can be discarded
        if (s.getHintTokens() != 8) {
            //Look for the oldest card with no information
            int oldestCardValue = 0, oldestCardPos = 0;
            for (int i = 0; i < hand[index].length; i++) {
                if (oldestCardValue < hand[index][i].getCardAge() && (hand[index][i].getNumber() == 0 && hand[index][i].getColour() == null)) {
                    oldestCardValue = hand[index][i].getCardAge();
                    oldestCardPos = i;
                }

            }
            //If no card with no information is found, return null
            if (oldestCardPos == 0 && oldestCardValue == 0 && s.getHintTokens() != 0) {
                return null;
            } else {
                //Otherwise discard card
                hand[index][oldestCardPos].resetCard();
                return new Action(index, toString(), ActionType.DISCARD, oldestCardPos);
            }
        }
        return null;
    }

    /**
     * Gives a random hint to the next player
     * This method is from basicAgent. It is used as a catch-all if no other rules match
     * @param s the game state
     * @return the action to be performed
     * @throws IllegalActionException if the move is not allowed
     */
    private Action hintRandom(State s) throws IllegalActionException {
        //Check to see if a hint can be given
        if (s.getHintTokens() > 0) {
            //Go to the next player
            int hintee = (index + 1) % numPlayers;
            Card[] hand = s.getHand(hintee);

            //Choose a random card in their hand
            java.util.Random rand = new java.util.Random();
            int cardIndex = rand.nextInt(hand.length);
            while (hand[cardIndex] == null) cardIndex = rand.nextInt(hand.length);
            Card c = hand[cardIndex];
            if (Math.random() > 0.5) {//give colour hint
                boolean[] col = new boolean[hand.length];
                for (int k = 0; k < col.length; k++) {
                    col[k] = c.getColour().equals((hand[k] == null ? null : hand[k].getColour()));
                }
                return new Action(index, toString(), ActionType.HINT_COLOUR, hintee, col, c.getColour());
            } else {//give value hint
                boolean[] val = new boolean[hand.length];
                for (int k = 0; k < val.length; k++) {
                    if (hand[k] == null) continue;
                    val[k] = c.getValue() == (hand[k] == null ? -1 : hand[k].getValue());
                }
                return new Action(index, toString(), ActionType.HINT_VALUE, hintee, val, c.getValue());
            }
        }
        return null;
    }
}
