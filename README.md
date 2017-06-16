# Client-Server-Chatroom

To compile and run the client-server application:

    javac Server.java
    javac Client.java

    java Server
    java Client

Valid Commands:

  - help - View all valid commands.
  - login UserId Password - Attempt to login as UserId with Password.
  - register UserId Password - Attempt to register as user UserId with password Password.
  - send all Message - Send Message to entire chat room.
  - send UserId Message - Send Message to only UserID.
  - who - View all current users connected to the server.
  - logout - Leave the chat room.
