# ChatServer
 A java based chatserver code
### Commands to run server 
Navigate to main dir and compile the java file 
```
javac IrcServerMain.java
```
Then run the java file
```
java IrcServerMain.java <server_name> <port>
```

### The Protocol
This server will accept only certain formats of strings from connected clients, and in some cases it will send a reply to the client. Each supported client message has the form
```
<command> <arguments>
```
where <command> is equal to NICK, USER, QUIT, JOIN, PART, NAMES, LIST, PRIVMSG, TIME, INFO, or PING. The arguments will then appear after a space, and the arguments that are expected will depend on what the command was.

If the server needs to send a reply, it will have the form
```
:<server_name> <reply_code> <nick> <text>
```
where

<server_name> is whatever name was specified for the server on the command line when it was started (see Compiling and Running);
<reply_code> is a 3-digit number that identifies what sort of reply it is (errors use 400);
<nick> is the nickname of the client the reply is being sent to, or * if no nickname has been set; and
<text> is some more text, which will usually start with a colon, and will have a format depending on what sort of reply it is. The colon is there to indicate that the text might contain spaces.
The different types of command and reply are explained below.

#### Commands
This server will support the following commands from clients. The most important ones are listed first, but some of the later ones like TIME, INFO and PING might be the easiest ones to implement, so you could start with these if you want.

#### NICK
This message is sent by the client in order to declare what nickname the user wants to be known by. The whole message will have the following format:
```
NICK <nickname>
```
where <nickname> is the chosen nickname. A valid nickname has 1–9 characters, and contains only letters, numbers and underscores. It cannot start with a number.

If the nickname is valid, the server will remember that this is the nickname for this client, and send no reply. If the nickname is invalid, the server will reject it by sending the reply

:<server_name> 400 * :Invalid nickname
#### USER
This message will be sent by the client after they have send a NICK message (see NICK). The USER message allows the client to specify their username and their real name. It has the following form:

```
USER <username> 0 * :<real_name>
```
where

<username> is the username, which will not include spaces (the username is stored internally but not used for anything else);
0 and * are meaningless (they are used for permissions in full IRC, but you can ignore them for this practical); and
<real_name> is the user’s real full name, which may include spaces.
If the message is valid, and a nickname has already been set, then the server will store all these details and regard the client as registered. A registered client can send and receive private messages, and join and leave channels. A reply will be sent of the form

:<server_name> 001 <nick> :Welcome to the IRC network, <nick>
If the message is invalid, one of the following replies will be sent:

:<server_name> 400 * :Not enough arguments
:<server_name> 400 * :Invalid arguments to USER command
:<server_name> 400 * :You are already registered
as appropriate.

#### QUIT
This message indicates that the user wants to end their connection to the server. It has no arguments, so it will just be the message:
```
QUIT
```
If the client is registered (see USER) then the server will send the message

:<nick> QUIT
to all connected clients (where <nick> is the quitting client’s nickname). The quitting user will also be removed from any channels they may be in (see JOIN).

Finally, the connection to the quitting client will be closed.

#### JOIN
This message is sent by a client in order to join a channel. A channel is like a chat room that users can join, and any messages sent to a channel will be seen by all users that are in the channel (see PRIVMSG).

This message will have the form:
```
JOIN <channel_name>
```
where <channel_name> is the name of the channel to join. A channel name must be a single # symbol followed by any number of letters, numbers and underscores.

If the channel name is valid, this user will be added to that channel. If no channel with this name exists on the server, it will be created. All users in the channel (including the joining user) will be sent the message

:<nick> JOIN <channel_name>
to indicate that a new user has joined. That user will then receive all chat messages sent to that channel until they leave the channel or quit the server.

If the channel name is invalid or the user is not registered, one of the following error messages will be sent:

:<server_name> 400 * :Invalid channel name
:<server_name> 400 * :You need to register first
as appropriate.

#### PART
This message is sent by a client when that user wishes to leave a channel they are in. It has the form:

```
PART <channel_name>
``` 
where <channel_name> is the name of the channel they want to leave. If successful, the message

:<nick> PART <channel_name>
will be sent to all users in the channel, and the user will be removed from the channel. If the channel is now empty, it will be deleted from the server. If the channel exists, but the user is not in it, the server will do nothing.

If unsuccessful, one of the following error replies might be necessary:

:<server_name> 400 * :You need to register first
:<server_name> 400 * :No channel exists with that name
#### PRIVMSG
This is perhaps the most important command, because it allows registered users to send chat messages to each other! It has the form:
```
PRIVMSG <target> :<message>
```
where <target> is either a channel name or a user’s nickname, and <message> is the full chat message they want to send, which may include spaces.

If the message is valid, then a message of the form

:<sender_nick> PRIVMSG <target> :<message>
will be sent to all appropriate users, where <sender_nick> is the nickname of the user that sent the message. If <target> is a channel, this will be sent to all users in that channel; if it is a user’s nickname, it will be sent to that user only.

If the message is invalid, one of the following error messages may be sent:

:<server_name> 400 * :No channel exists with that name
:<server_name> 400 * :No user exists with that name
:<server_name> 400 * :Invalid arguments to PRIVMSG command
:<server_name> 400 * :You need to register first
#### NAMES
This message is sent by a registered client to request the nicknames of all users in a given channel. It has the form:
```
NAMES <channel_name>
```
If a channel with the given name exists, the server will send a reply of the following form:

:<server_name> 353 <nick> = <channel_name> :<nicks>
where

<server_name> is the server name;
<nick> is the nickname of the user that sent the request;
<channel_name> is the channel being queried;
<nicks> is a space-separated list of the nicknames of all users in the channel, for example moeen zak jos ben jofra jonny.
The server might need to reply with one of the following error replies:

:<server_name> 400 * :You need to register first
:<server_name> 400 * :No channel exists with that name
#### LIST
This message allows a registered client to request the names of all channels on the server. It has no arguments, so the whole message is just:
```
LIST
```
and the server will reply with one line for each channel, of the form:

:<server_name> 322 <nick> <channel_name>
followed by one final line of the form:

:<server_name> 323 <nick> :End of LIST
where <nick> is the nickname of the user who sent the LIST command.

If the user is not registered, they will receive the same error reply as they would for NAMES.

#### TIME
Clients can send the simple message:
```
TIME
```
 
to ask the server to respond with the current date and time. The server will send a reply of the form:

:<server_name> 391 * :<time>

where <time> is the server’s local time, in the standard ISO 8601 format, something like

2022-10-13T14:23:34.443757
This is fairly easy to achieve if you use Java’s java.time.LocalDateTime package. Look it up in the Java API!

#### INFO
The user can request some basic information about the server by sending the message:
```
 INFO
 ```
The server will send a reply of the form:
:<server_name> 371 * :<message>

where <message> is a short string saying what the server is and who wrote it. The exact content of this is up to you, but it will fit on one line, and will not include your real name.

#### PING
Finally, any client can send a message of the form
```
 PING <text>
 ```
where <text> is any string of characters. The server will respond with
```
 PONG <text>
 ```
where <text> is the exact same string sent back. This can be useful for clients to make sure their connection is still active.

java IrcServerMain hello 12345
If This server is started without supplying the command-line arguments, or with an invalid port number, it will simply print the usage message indicated below and exit:

Usage: java IrcServerMain <server_name> <port>