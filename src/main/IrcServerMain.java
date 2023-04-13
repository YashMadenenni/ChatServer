package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;

/**
 * Server class implementing thread.
 * To connect establish connectioons
 */
public class IrcServerMain implements Runnable {
    private ExecutorService threadPool;
    private ArrayList<Connections> connectionsArray;
    private ArrayList<Channel> channels;
    private ServerSocket server;
    private Boolean finished;
    private int port;
    private String serverName;

    /**
     * Constructor for server class IrcServerMain.
     * 
     */
     IrcServerMain() {
        connectionsArray = new ArrayList<>();
        channels = new ArrayList<>();
        finished = false;
    }

    /**
     * Run method for server thread.
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            server = new ServerSocket(port); // Telnet localhost 8888
            threadPool = Executors.newCachedThreadPool();
            while (!finished) {
                Socket socketAccept = server.accept();
                Connections connect = new Connections(socketAccept);
                connectionsArray.add(connect);
                // connect.run();
                threadPool.execute(connect);
            }
        } catch (Exception e) {
            closeServer();
        }
    }

    /**
     * Method to send message to client.
     * 
     * @param message TO store content of the message body
     */
    public void sendMessageToClient(String message) {
        for (Connections connected : connectionsArray) {
            if (connected != null) {
                connected.messageClient(message);
            }
        }
    }

    /**
     * Method to close server and connections.
     * 
     */
    public void closeServer() {

        try {
            finished = true;
            if (!server.isClosed()) {
                server.close();
                threadPool.shutdown();
            }
            for (Connections connections : connectionsArray) {
                connections.closeConnection();
            }
        } catch (IOException e) {
        }
    }

    class Connections implements Runnable {
        private Socket socketAccept;
        private String nickname;
        private InputStreamReader input;
        private BufferedReader inputBuffer;
        private PrintWriter outputClient;

         Connections(Socket socketAccept) {
            this.socketAccept = socketAccept;
        }

        public void run() {
            try {
                HashMap<String, String> registeredUsers = new HashMap<String, String>();
                input = new InputStreamReader(socketAccept.getInputStream());
                inputBuffer = new BufferedReader(input);
                outputClient = new PrintWriter(socketAccept.getOutputStream(), true);
                // outputClient.println(socketAccept);
                //
                String inputMessage;
                while ((inputMessage = inputBuffer.readLine()) != null) {
                    // outputClient.println(inputMessage);
                    // System.out.println("in first run connections");
                    String[] userCommand = inputMessage.split(" ");

                    // outputClient.println(userCommand[1]);
                    switch (userCommand[0]) {
                        case "NICK":
                            if (userCommand.length == 2) {

                                String[] splitUserCommand = userCommand[1].split("");
                                int lengthSplitUserCommand = splitUserCommand.length;

                                Boolean regex = false;
                                final int one = 1;
                                final int nine = 9;
                                if (lengthSplitUserCommand >= one && lengthSplitUserCommand <= nine
                                        && splitUserCommand[0].matches("[A-Za-z_]")) {
                                    // outputClient.println(lengthsplituserCommand);
                                    for (int i = 0; i < lengthSplitUserCommand; i++) {
                                        if (splitUserCommand[i].matches("[A-Za-z0-9_]")) {

                                            regex = true;
                                        } else {
                                            regex = false;

                                        }
                                    }
                                }

                                if (!regex) {
                                    outputClient.println(":" + serverName + " 400 * :Invalid nickname");
                                } else {
                                    // outputClient.println(regex);
                                    nickname = userCommand[1];

                                }
                            } else {
                                outputClient.println(":" + serverName + " 400 * :Invalid nickname");

                            }
                            break;
                        case "USER":
                            // outputClient.println(" IN USER");
                            if (nickname != null) {
                                final int five = 5;
                                if (userCommand.length < five) {
                                    outputClient.println(":" + serverName + " 400 * :Not enough arguments");
                                } else if (registeredUsers.containsKey(userCommand[1])) {
                                    outputClient.println(":" + serverName + " 400 * :You are already registered");
                                } else {
                                    final int four = 4;
                                    String realname = userCommand[four];
                                    registeredUsers.put(nickname, realname);
                                    outputClient.println(":" + serverName + " 001 " + this.nickname
                                            + " :Welcome to the IRC network, " + this.nickname);
                                }
                            } else {
                                outputClient.println("Give Nick name first");
                            }
                            break;
                        case "QUIT":
                            if (registeredUsers.containsKey(nickname)) {
                                sendMessageToClient(":" + nickname + " QUIT");

                                for (Channel ch : channels) {
                                    if (ch.connection.socketAccept.equals(this.socketAccept)) {
                                        ch.removeUser();
                                    }
                                }
                                this.closeConnection();

                            }
                            break;
                        case "TIME":
                            outputClient.println("in TIME");
                            LocalDateTime now = LocalDateTime.now();
                            outputClient.println(":" + serverName + " 391 * :" + now);
                            break;
                        case "INFO":
                            outputClient.println(
                                    ":" + serverName + " 371 * : Chat server created by the founders of Flash Talk");
                            break;
                        case "PING":
                            outputClient.println("PONG " + userCommand[1]);
                            break;
                        case "JOIN":
                            String str = userCommand[1];
                            // outputClient.println("JOIN " + this.nickname);
                            if (userCommand[1].matches("^#[A-Za-z0-9_]*")
                                    && registeredUsers.containsKey(this.nickname)) {
                                Channel channel = new Channel(str, this);
                                channel.addUser();
                                messageChannel(":" + this.nickname + " JOIN " + channel.channelName,
                                        channel.channelName);

                            } else if (!registeredUsers.containsKey(this.nickname)
                                    && userCommand[1].matches("^#[A-Za-z0-9_]*")) {
                                outputClient.println(":" + serverName + " 400 * :You need to register first");

                            } else {
                                outputClient.println(":" + serverName + " 400 * :Invalid channel name");
                            }
                            break;

                        case "LIST":

                            for (Channel channel : channels) {
                                outputClient.println(
                                        ":" + serverName + " 322 " + this.nickname + " " + channel.channelName);
                            }
                            outputClient.println(":" + serverName + " 323 " + this.nickname + " :End of LIST");
                            break;
                        case "PART":
                            // outputClient.print("in part");
                            Boolean channelExsists = false;
                            if (!registeredUsers.containsKey(this.nickname)) {
                                outputClient.println(":" + serverName + " 400 * :You need to register first");
                            } else {
                                for (Channel channel : channels) {

                                    if (channel.channelName.equals(userCommand[1])
                                            && registeredUsers.containsKey(this.nickname)) {
                                        channelExsists = true;
                                        // outputClient.print("in part if");

                                        messageChannel(":" + this.nickname + " PART " + channel.channelName,
                                                channel.channelName);
                                        channels.remove(channel);
                                        break;
                                    }
                                }
                                if (!channelExsists) {

                                    outputClient.println(":" + serverName + " 400 " + this.nickname
                                            + " :No channel exists with that name");
                                }
                            }
                            break;

                        case "NAMES":
                            Boolean channelExsist = false;
                            ArrayList<String> names = new ArrayList<>();
                            for (Channel channel : channels) {
                                if (channel.channelName.equals(userCommand[1])) {
                                    channelExsist = true;
                                    names.add(channel.connection.nickname);
                                }
                            }
                            if (!channelExsist) {
                                outputClient.println(":" + serverName + " 400 * :No channel exists with that name");
                            } else {
                                outputClient.print(
                                        ":" + serverName + " 353 " + this.nickname + " = " + userCommand[1] + " :");
                                for (String name : names) {
                                    outputClient.println(name);
                                }

                            }
                            break;
                        case "PRIVMSG":
                            final int three = 3;
                            String[] splitUserCommand = inputMessage.split(" ", three);
                            String messageTo = splitUserCommand[1];
                            String messageContent = splitUserCommand[2];
                            boolean isChannelExsist = false;

                            if (splitUserCommand.length == three && messageTo.matches("^#[A-Za-z0-9]*")) {
                                for (Channel channel : channels) {
                                    if (channel.channelName.equals(messageTo)) {
                                        isChannelExsist = true;
                                    }
                                }
                                if (isChannelExsist) {
                                    messageChannel(
                                            ":" + this.nickname + " PRIVMSG " + messageTo + " " + messageContent,
                                            messageTo);
                                } else {
                                    outputClient.println(":" + serverName + " 400 * :No channel exists with that name");
                                }
                            } else if (registeredUsers.containsKey(this.nickname)) {
                                boolean chechName = false;
                                for (Connections con : connectionsArray) {
                                    if (con.nickname.equals(messageTo)) {
                                        chechName = true;
                                        con.messageClient(
                                                ":" + this.nickname + " PRIVMSG " + messageTo + " " + messageContent);
                                    }
                                }
                                if (!chechName) {
                                    this.messageClient(":" + serverName + " 400 * :No user exists with that name");
                                }

                            } else if (!registeredUsers.containsKey(this.nickname)) {
                                 this.messageClient(":" + serverName + " 400 * :You need to register first");
                            } else if (splitUserCommand.length < three) {
                                this.messageClient(":" + serverName + " 400 * :Invalid arguments to PRIVMSG command");
                            }
                            break;

                        default:
                            break;
                    }

                }

            } catch (Exception e) {
                closeConnection();
            }
        }

        public void messageClient(String message) {
            outputClient.println(message);
        }

        public void closeConnection() {
            try {
                inputBuffer.close();
                outputClient.close();
                input.close();
                if (!socketAccept.isClosed()) {
                    socketAccept.close();
                }
            } catch (Exception e) {

            }
        }
    }

    class Channel {
        private String channelName;
        private Connections connection;

         Channel(String channelName, Connections connection) {
            this.channelName = channelName;
            this.connection = connection;
        }

        public void addUser() {
            channels.add(this);
        }

        public void removeUser() {
            channels.remove(this);
        }
    }

    /** Method to send message to channels.
     * @param message to store message body to be sent to channel
     * @param chanelName the channel the message is to be sent
     */
    public void messageChannel(String message, String chanelName) {
        for (Channel channel : channels) {

            if (channel.connection != null && channel.channelName.equals(chanelName)) {
                channel.connection.messageClient(message);
            }
        }
    }

    /** Main class.
     * @param args to take the user inputs for servername and port address
     */
    public static void main(String[] args) {
        IrcServerMain ircServerMain = new IrcServerMain();
        if (args.length >= 1) {
            try {
                ircServerMain.serverName = args[0];
                ircServerMain.port = Integer.parseInt(args[1]);
                // System.out.println("Started");
                ircServerMain.run();

            } catch (Exception e) {
                System.out.println("Usage: java IrcServerMain <server_name> <port>");
            }

        } else {
            System.out.println("Usage: java IrcServerMain <server_name> <port>");
        }
    }

}
