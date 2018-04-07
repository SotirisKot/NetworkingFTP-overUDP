package Net;

import java.io.*;

public class UDPserver{

  public static void main(String args[]) throws IOException{
      System.out.println("Server started!!!");
      if(args.length == 2){
        String ServerIP = args[0];
        int portNumber = Integer.parseInt(args[1]);
        ClientHandler clientThread = new ClientHandler(ServerIP,portNumber);
        clientThread.start();
      }else{
        System.out.println("Wrong arguments!!!");
      }
  }

}