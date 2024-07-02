import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Croupier {

    private final int PORT = 9876;
    private Map<String, PlayerInfo> players = new HashMap<>();
    private Map<String, CounterInfo> counters = new HashMap<>();
    private List<String> cardHistory = new ArrayList<>();
    private int numberOfDecks = 6;

    private void start(){
        try(DatagramSocket socket = new DatagramSocket(PORT)){
            byte[] recieveBuffer = new byte[1024];

            while(true){
                DatagramPacket recievePacket = new DatagramPacket(recieveBuffer, recieveBuffer.length);
                socket.receive(recievePacket);
                String message = new String(recievePacket.getData(), 0, recievePacket.getLength(), StandardCharsets.UTF_8);
                System.out.println("Recieved: " + message);

                String response = processMessage(message, recievePacket.getAddress(), recievePacket.getPort());
                byte[] responseBuffer = response.getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length,
                        recievePacket.getAddress(), recievePacket.getPort());
                socket.send(responsePacket);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private String processMessage(String message, InetAddress address, int port) {
        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case "registerPlayer":
                return registerPlayer(parts, address, port);
            case "bet":
                return placeBet(parts);
            case "hit":
            case "stand":
            case "doubleDown":
            case "surrender":
                return processAction(parts);
            case "registerCounter":
                return registerCounter(parts, address, port);
            case "card":
                return processCard(parts);
            default:
                return "unknown command";
        }
    }

    private String processCard(String[] parts) {
        String owner = parts[1];
        String cardDeck  = parts[2];
        String cardToString = parts[3];
        cardHistory.add(cardToString);

        if(players.containsKey(owner)){
            PlayerInfo player = players.get(owner);
            player.addCard(cardToString);
            return "player " + player.getName() + "recieved " + cardDeck + " " + cardToString;
        } else if (counters.containsKey(owner)) {
            CounterInfo counter = counters.get(owner);
            return "counter " + counter.getPlayerName() + " recieved " + cardDeck + cardToString;
        } else {
            return "card declined, unknown player";
        }

    }

    private String registerCounter(String[] parts, InetAddress address, int port) {
        String ipAdress = parts[1];
        int counterPort = Integer.parseInt(parts[2]);
        String playerName = parts[3];
        if(!players.containsKey(playerName)){
            return "registration denied unknown player";
        } else {
            counters.put(playerName, new CounterInfo(ipAdress, counterPort, playerName));
            return "registration successful";
        }
    }

    private String processAction(String[] parts) {
        String action = parts[0];
        String name = parts[1];
        String cardDeck = parts[2];
        String cardToString = parts[3];

        PlayerInfo player = players.get(name);
        if(player == null)return "action denied unknown player";

        switch (action){
            case "hit":
                return handleHit(player, cardToString);
            case "stand":
                return handleStand(player);
            case "split":
                return handleSplit(player, cardToString);
            case "doubleDown":
                return handleDoubleDown(player, cardToString);
            case "surrender":
                return handleSurrender(player);
            default:
                return "action declined unknown action";
        }
    }

    private String handleHit(PlayerInfo player, String cardToString) {
        if (player.isStanding()) {
            return "action declined Spieler steht bereits";
        }
        player.addCard(cardToString);
        return "action accepted";
    }

    private String handleStand(PlayerInfo player) {
        player.stand();
        return "action accepted";
    }

    private String handleSplit(PlayerInfo player, String cardToString) {
        if (player.hasSplit()) {
            return "action declined Split bereits durchgeführt";
        }

        player.split();
        player.addCard(cardToString);
        return "action accepted";
    }

    private String handleDoubleDown(PlayerInfo player, String cardToString) {
        if (player.hasDoubledDown()) {
            return "action declined Double Down bereits durchgeführt";
        }

        player.doubleDown();
        player.addCard(cardToString);
        return "action accepted";
    }

    private String handleSurrender(PlayerInfo player) {
        player.surrender();
        return "action accepted";
    }

    private String placeBet(String[] parts) {
        String name = parts[1];
        int betAmount = Integer.parseInt(parts[2]);
        PlayerInfo player = players.get(name);

        if(player == null)return "bet declined";

        if(betAmount > player.getBalance()){
            return "bet declined, stake larger than Balance";
        } else if (betAmount > 1000) {
            return "bet declined, stake too high";
        } else {
            player.setBetAmount(betAmount);
            player.adjustBalance(-betAmount);
            return "bet accepted";
        }
    }

    private String registerPlayer(String[] parts, InetAddress address, int port) {
        if(players.size() >= 5)return "registration denied too many players";

        String ipAdress = parts[1];
        int playerPort = Integer.parseInt(parts[2]);
        String name = parts[3];
        int intitialBalance = 5000;
        players.put(name, new PlayerInfo(ipAdress, playerPort, name, intitialBalance));
        return "registration successful";
    }

    private String numberOfDecks(String[] parts) {
        String playerName = parts[1];
        if (!players.containsKey(playerName)) {
            return "numberOfDecks declined unbekannter Spielername";
        } else {
            return "numberOfDecks " + numberOfDecks;
        }
    }


    private class PlayerInfo{
        String ipAddress;
        int port;
        String name;
        int betAmount;
        int balance;
        List<String> cards = new ArrayList<>();
        boolean isStanding = false;
        boolean hasSplit = false;
        List<String> splitHand = new ArrayList<>();
        boolean hasDoubledDown = false;

        PlayerInfo(String ipAddress, int port, String name, int initialBalance) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.name = name;
            this.balance = initialBalance;
        }

        void setBetAmount(int betAmount) {
            this.betAmount = betAmount;
        }

        int getBetAmount() {
            return betAmount;
        }

        String getName() {
            return name;
        }

        List<String> getCards() {
            return cards;
        }

        void addCard(String card) {
            cards.add(card);
        }

        void addCardToSplitHand(String card) {
            splitHand.add(card);
        }

        void adjustBalance(int amount) {
            this.balance += amount;
        }

        int getBalance() {
            return balance;
        }

        void stand() {
            isStanding = true;
        }

        void split() {
            if (cards.size() == 2 && cards.get(0).equals(cards.get(1))) {
                hasSplit = true;
                splitHand.add(cards.remove(1));
            }
        }

        void doubleDown() {
            if (cards.size() == 2) {
                hasDoubledDown = true;
                adjustBalance(-betAmount);
                betAmount *= 2;
            }
        }

        void surrender() {
            adjustBalance(betAmount / 2);
            betAmount /= 2;
            isStanding = true;
        }

        boolean isStanding() {
            return isStanding;
        }

        boolean hasSplit() {
            return hasSplit;
        }

        boolean hasDoubledDown() {
            return hasDoubledDown;
        }

        List<String> getSplitHand() {
            return splitHand;
        }
    }

    private class CounterInfo{
        String ipAddress;
        int port;
        String playerName;
        List<String> cards = new ArrayList<>();

        CounterInfo(String ipAddress, int port, String playerName) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.playerName = playerName;
        }

        void receiveCard(String card) {
            cards.add(card);
        }

        String getPlayerName() {
            return playerName;
        }
    }
}


