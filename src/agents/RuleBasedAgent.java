package agents;
import hanabAI.*;


public class RuleBasedAgent implements Agent{

    private Cards[] hand;
    private boolean firstAction = true;
    private int numPlayers;
    private int index;


  /**
   * An inner class to hold information about the agents cards
   * Used to determine what the cards are, or what the probability of a card is
  **/
//TODO make private and create access methods
  protected class Cards {
      private Colour colour;
      private int number;
      private int cardAge;

      public Cards() {
          this.colour = null;
          this.number = 0;
          this.cardAge = 0;
      }


      public void setColour (Colour colour) {
          this.colour = colour;
      }

      public Colour getColour () {
          return this.colour;
      }

      public void setNumber (int number) {
          this.number = number;
      }

      public int getNumber () {
          return this.number;
      }


      public void incrementCardAge () {
          this.cardAge++;
      }

      public int getCardAge () {
          return cardAge;

      }

      public void resetCard() {
          this.colour = null;
          this.number = 0;
          this.cardAge = 0;
      }
  }


  /**
   * Default constructor, does stuff TODO: fill in description.
   * **/
  public RuleBasedAgent(){
  }

  /**
   * Initialises variables on the first call to do action.
   * @param s the State of the game at the first action
   **/
  public void init(State s){

    numPlayers = s.getPlayers().length;
    if(numPlayers>3){
        hand = new Cards[4];
        for (int i = 0; i < 4; i++) {
            hand[i] = new Cards();
        }
    }
    else{
        hand = new Cards[5];
        for (int i = 0; i < 5; i++) {
            hand[i] = new Cards();
        }
    }
    index = s.getNextPlayer();
    firstAction = false;
  }

  /**
   * Returns the name BaseLine.
   * @return the String "BaseLine"
   * */
  public String toString(){return "RuleAgent";}

  /**
   * Performs an action given a state.
   * Assumes that they are the player to move.
   * The strategy will
   * a) play a card if a card is known to be playable,
   * b) discard a card if a card is known to be useless
   * c) give a number hint to the next player with a playable card (0.1 per hint token)
   * d) give a colour hint to the next player with a playable card (0.1 per hint token)
   * e) play a potential card (0.1 per fuse token)
   * f) discard an unknown card
   * g) discard a known card
   * @param s the current state of the game.
   * @return the action the player takes.
   **/
  public Action doAction(State s){
    if(firstAction){
      init(s);
    }
    //Assume players index is s.getNextPlayer()
    index = s.getNextPlayer();
    //get any hints
    try{
      getHintedCards(s);
      Action a = playKnown(s);
      if (a==null) a= playOnes(s);
      if(a==null) a = hint(s);
      if(a==null) a = discardKnown(s);

      if(a==null) a = discardOldestCard(s);
      if(a==null) a = hintRandom(s);
      for (int i = 0; i < hand.length; i++) {
          hand[i].incrementCardAge();
      }
      return a;
    }
    catch(IllegalActionException e){
      e.printStackTrace();
      throw new RuntimeException("Something has gone very wrong");
    }
  }

  //updates colours and values from hints received
  public void getHintedCards(State s){
    try{
      State t = (State) s.clone();
      for(int i = 0; i<Math.min(numPlayers-1,s.getOrder());i++){
        Action a = t.getPreviousAction();
        if((a.getType()==ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE) && a.getHintReceiver()==index){
          boolean[] hints = t.getPreviousAction().getHintedCards();
          for(int j = 0; j<hints.length; j++){
            if(hints[j]){
              if(a.getType() == ActionType.HINT_COLOUR) {
                  hand[j].setColour(a.getColour());
              }
              else {
                  hand[j].setNumber(a.getValue());
              }
            }
          }
        }
        t = t.getPreviousState();
      }
    }
    catch(IllegalActionException e){e.printStackTrace();}
  }

  //returns the value of the next playable card of the given colour
  public int playable(State s, Colour c){
    java.util.Stack<Card> fw = s.getFirework(c);
    if (fw.size()==5) return -1;
    else return fw.size()+1;
  }

  //plays the first card known to be playable.
  public Action playKnown(State s) throws IllegalActionException{
      for(int i = 0; i<hand.length; i++){
      if(hand[i].colour != null && (hand[i].number != 0 && hand[i].number == playable(s, hand[i].colour))) {
          hand[i].resetCard();
        return new Action(index, toString(), ActionType.PLAY, i);
      }
    }
    return null;
  }

  public Action playOnes(State s) throws  IllegalActionException {
      int fwsize = 0;
      for (int i = 0; i < hand.length; i++) {
          for (Colour fwColour: Colour.values()) {
              if (s.getFirework(fwColour).size() > 0)
              fwsize = s.getFirework(fwColour).size();

          }
      }
      if (fwsize == 0) {
          for (int i = 0; i < hand.length; i++) {
              if (hand[i].getNumber() == 1) {
                  return new Action(index, toString(), ActionType.PLAY, i);
              }
          }
      }
      State t = (State) s.clone();
      for(int i = 0; i<Math.min(numPlayers-1,s.getOrder());i++) {
          int numHints = 0;
          int cardNo = 0;
          Action a = t.getPreviousAction();
          if ((a.getType() == ActionType.HINT_COLOUR || a.getType() == ActionType.HINT_VALUE) && a.getHintReceiver() == index) {
              boolean[] hints = t.getPreviousAction().getHintedCards();
              for (int j = 0; j < hints.length; j++) {
                  if (hints[j]) {
                      numHints++;
                      cardNo = j;
                  }
              }
          }
          if (numHints == 1) {
              return new Action(index, toString(), ActionType.PLAY, cardNo);
          }
          /**
           * TODO implement if one player has a large number of hints of a particular number, maybe play that number
          else if (fwsize < 2 && numHints > 3) {
              java.util.Random rand = new Random();
              rand.nextInt(hand.length);
          }
           **/
      }


      return null;
  }

  //discards the first card known to be unplayable.
  public Action discardKnown(State s) throws IllegalActionException{
    if (s.getHintTokens() != 8) {
      for(int i = 0; i<hand.length; i++){
        if(hand[i].getColour() != null && hand[i].getNumber() > 0 && hand[i].getNumber() < playable(s, hand[i].colour)) {
          hand[i].resetCard();
          return new Action(index, toString(), ActionType.DISCARD, i);
        }
      }
    }
    return null;
  }

  //gives hint of first playable card in next players hand
  //flips a coin to determine whether it is a colour hint or value hint
  //return null if no hint token left, or no playable cards
  public Action hint(State s) throws IllegalActionException{
    if(s.getHintTokens()>0){
      for(int i = 1; i<numPlayers; i++){
        int hintee = (index+i)%numPlayers;
        Card[] otherPlayerHand = s.getHand(hintee);
        for(int j = 0; j<otherPlayerHand.length; j++){
          Card c = otherPlayerHand[j];
          if(c!=null && c.getValue()==playable(s,c.getColour())){
            //flip coin
            if(Math.random()>0.5){//give colour hint
              boolean[] col = new boolean[otherPlayerHand.length];
              for(int k = 0; k< col.length; k++){
                col[k]=c.getColour().equals((otherPlayerHand[k]==null?null:otherPlayerHand[k].getColour()));
              }
              return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
            }
            else{//give value hint
              boolean[] val = new boolean[otherPlayerHand.length];
              for(int k = 0; k< val.length; k++){
                val[k]=c.getValue() == (otherPlayerHand[k]==null?-1:otherPlayerHand[k].getValue());
              }
              return new Action(index,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
            }
          }
        }
      }
    }
    return null;
  }


  //discard the oldest card that we have no information on card
  public Action discardOldestCard(State s) throws IllegalActionException{
    if (s.getHintTokens() != 8) {
        int oldestCardValue = 0, oldestCardPos = 0;
        for (int i = 0; i < hand.length; i++) {
            if (oldestCardValue < hand[i].getCardAge() && (hand[i].getNumber() == 0 && hand[i].getColour() == null)) {
                oldestCardValue = hand[i].getCardAge();
                oldestCardPos = i;
                continue;
            } else {
                continue;
            }

        }
        if (oldestCardPos == 0 && oldestCardValue == 0 && s.getHintTokens() != 0) {
            return null;
        }
        else {
            hand[oldestCardPos].resetCard();
            return new Action(index, toString(), ActionType.DISCARD, oldestCardPos);
        }

    }
    return null;
  }

  //gives random hint of a card in next players hand
  //flips a coin to determine whether it is a colour hint or value hint
  //return null if no hint token left
  public Action hintRandom(State s) throws IllegalActionException{
    if(s.getHintTokens()>0){
        int hintee = (index+1)%numPlayers;
        Card[] hand = s.getHand(hintee);

        java.util.Random rand = new java.util.Random();
        int cardIndex = rand.nextInt(hand.length);
        while(hand[cardIndex]==null) cardIndex = rand.nextInt(hand.length);
        Card c = hand[cardIndex];

        if(Math.random()>0.5){//give colour hint
          boolean[] col = new boolean[hand.length];
          for(int k = 0; k< col.length; k++){
            col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
          }
          return new Action(index,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
        }
        else{//give value hint
          boolean[] val = new boolean[hand.length];
          for(int k = 0; k< val.length; k++){
            if (hand[k] == null) continue;
            val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
          }
          return new Action(index,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
        }

      }

    return null;
  }

}
